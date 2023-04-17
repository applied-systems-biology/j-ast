package org.hkijena.jipipe.webapp.growthassay.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.InputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.services.AnalysisService;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class DatasetController {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;
    private final InputDataRepository inputDataRepository;

    private final JobScheduler jobScheduler;
    private final AnalysisService analysisService;

    @Autowired
    public DatasetController(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository, InputDataRepository inputDataRepository, JobScheduler jobScheduler, AnalysisService analysisService) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
        this.inputDataRepository = inputDataRepository;
        this.jobScheduler = jobScheduler;
        this.analysisService = analysisService;
    }

    @GetMapping("/dataset/new")
    public ModelAndView newDataset(Model model) throws IOException {
        Dataset dataset = new Dataset();
        Path storageDir;
        if(StringUtils.isNullOrEmpty(runtimeConfig.getCustomTempDirectory())) {
            storageDir = Files.createTempDirectory("jip-webapp");
        }
        else {
            storageDir = Files.createTempDirectory(Paths.get(runtimeConfig.getCustomTempDirectory()), "jip-webapp");
        }
        dataset.setStoragePath(storageDir.toString());
        dataset = datasetRepository.save(dataset);
        return new ModelAndView("redirect:/dataset/view/" + dataset.getId());
    }

    @GetMapping("/dataset/view/{id}")
    public ModelAndView viewDataset(Model model, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if (dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            datasetRepository.putSortedToModel(model);
            model.addAttribute("currentDataset", dataset);
            model.addAttribute("currentDatasetId", dataset.getId());

            switch (dataset.getStatus()) {
                case Preparing -> {
                    return new ModelAndView("dataset-editor");
                }
                case Running -> {
                    return new ModelAndView("dataset-run");
                }
                case RunInterrupted -> {
                    return new ModelAndView("dataset-run-interrupted");
                }
                case RunFinished -> {
                    return new ModelAndView("dataset-run-finished");
                }
                default -> throw new UnsupportedOperationException();
            }

        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/delete/{id}")
    public ModelAndView deleteDataset(Model model, RedirectAttributes redirectAttributes, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {

            Dataset dataset = dataset_.get();

            if(dataset.getStatus() == Dataset.Status.Running) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            datasetRepository.delete(dataset);

            Notification.pushToRedirect("Dataset deleted", "The dataset '" + dataset.getName() + "' was deleted.", Notification.Style.success, redirectAttributes);
            return new ModelAndView("redirect:/");
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/rename/{id}")
    public ModelAndView renameDataset(Model model, @PathVariable long id, @RequestParam("datasetName") String datasetName) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            datasetName = StringUtils.nullToEmpty(datasetName).trim();
            if(StringUtils.isNullOrEmpty(datasetName)) {
                datasetName = "Unnamed";
            }
            dataset.setName(datasetName);
            datasetRepository.save(dataset);
            return new ModelAndView("redirect:/dataset/view/" + id);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/validate/{id}")
    public ResponseEntity<ValidationResult> validateDataset(@PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            return ResponseEntity.ok(dataset_.get().validate());
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dataset/upload-input/{id}")
    public ModelAndView uploadFileToDataset(Model model, @PathVariable long id, RedirectAttributes redirectAttributes, @RequestParam("imageFiles") MultipartFile[] imageFiles) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(dataset.getStatus() != Dataset.Status.Preparing) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // Create target directory
            Path targetDir = Paths.get(dataset.getStoragePath()).resolve("inputs_raw");
            Path targetDirThumbnails = Paths.get(dataset.getStoragePath()).resolve("inputs_raw_thumbnails");
            try {
                Files.createDirectories(targetDir);
                Files.createDirectories(targetDirThumbnails);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Iterate through images and organize them
            int numSuccess = 0;
            int numFailures = 0;
            List<String> failureNames = new ArrayList<>();
            for (MultipartFile imageFile : imageFiles) {
                if(imageFile != null && !imageFile.isEmpty()) {
                    try {
                        try (InputStream stream = imageFile.getInputStream()) {
                            BufferedImage image = ImageIO.read(stream);
                            if (image == null) {
                                throw new NullPointerException("Unable to load image!");
                            }

                            // Re-save as PNG
                            Path imageStoragePath = Files.createTempFile(targetDir, "img", ".png");
                            ImageIO.write(image, "PNG", imageStoragePath.toFile());

                            // Create thumbnail
                            double thumbnailScale = Math.max(64.0 / image.getWidth(), 64.0 / image.getHeight());
                            Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
                            BufferedImage thumbnail = new BufferedImage(64,64, BufferedImage.TYPE_3BYTE_BGR);
                            Graphics2D graphics2D = thumbnail.createGraphics();
                            graphics2D.drawImage(scaledImage, 32 - scaledImage.getWidth(null) / 2, 32 - scaledImage.getHeight(null) / 2, null);
                            graphics2D.dispose();
                            Path thumbnailStoragePath = targetDirThumbnails.resolve(imageStoragePath.getFileName());
                            ImageIO.write(thumbnail, "PNG", thumbnailStoragePath.toFile());

                            // Create object
                            InputData inputData = new InputData();
                            inputData.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            inputData.setStoragePath(imageStoragePath.toString());
                            inputData.setThumbnailStoragePath(thumbnailStoragePath.toString());
                            inputData.tryAutoFill(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));

                            dataset.addInputData(inputData);
                            ++numSuccess;
                        }
                    }
                    catch (Throwable e) {
                        ++numFailures;

                        if(!StringUtils.isNullOrEmpty(imageFile.getOriginalFilename())) {
                            failureNames.add(imageFile.getOriginalFilename());
                        }
                    }
                }
            }
            datasetRepository.save(dataset);

            if(numSuccess > 0) {
                Notification.pushToRedirect("Successfully imported images",
                        numSuccess + " images were successfully imported.",
                        Notification.Style.success,
                        redirectAttributes);
            }
            if(numFailures > 0) {
                Notification.pushToRedirect("Unable to import images",
                        numFailures + " images could not be imported! Please ensure to only provide PNG files. Affected files: " + String.join(", ", failureNames),
                        Notification.Style.danger,
                        redirectAttributes);
            }


            return new ModelAndView("redirect:/dataset/view/" + id);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/update/{id}")
    public void update(HttpServletResponse response, @PathVariable long id, @RequestBody DatasetUpdateMessage message) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            if(dataset.getStatus() == Dataset.Status.Preparing) {
                message.getParametersUpdateMessage().update(dataset);
                datasetRepository.save(dataset);
                for (DatasetUpdateMessage.InputDataUpdateMessage inputDataUpdateMessage : message.getInputDataUpdateMessageMap().values()) {
                    Optional<InputData> inputData_ = inputDataRepository.findById(inputDataUpdateMessage.getId());
                    if (inputData_.isPresent()) {
                        InputData inputData = inputData_.get();
                        inputDataUpdateMessage.update(inputData);
                        inputDataRepository.save(inputData);
                    }
                }
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/run/{id}")
    public ModelAndView run(RedirectAttributes redirectAttributes, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            if(dataset.getStatus() == Dataset.Status.Preparing) {
                if (dataset.validate().isValid()) {
                    dataset.setStatus(Dataset.Status.Running);
                    datasetRepository.save(dataset);
                    jobScheduler.enqueue(() -> analysisService.runAnalysis(id, JobContext.Null));
                    return new ModelAndView("redirect:/dataset/view/" + id);
                } else {
                    Notification.pushToRedirect("Dataset is invalid!", "Validation checks failed.", Notification.Style.danger, redirectAttributes);
                    return new ModelAndView("redirect:/dataset/view/" + id);
                }
            }
            else {
                Notification.pushToRedirect("Dataset is not ready!", "An analysis is currently in progress.", Notification.Style.danger, redirectAttributes);
                return new ModelAndView("redirect:/dataset/view/" + id);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/reset/{id}")
    public ModelAndView reset(RedirectAttributes redirectAttributes, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            if (dataset.getStatus() != Dataset.Status.Running) {
                dataset.setStatus(Dataset.Status.Preparing);
                datasetRepository.save(dataset);
                Notification.pushToRedirect("Dataset reset", "You can now edit all parameters and modify the inputs.", Notification.Style.info, redirectAttributes);
                return new ModelAndView("redirect:/dataset/view/" + id);
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/query-status/{id}")
    public ResponseEntity<AnalysisStatusMessage> queryDatasetStatus(@PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            AnalysisStatusMessage message = new AnalysisStatusMessage();
            message.setStatus(dataset.getStatus());
            return ResponseEntity.ok(message);
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarting(ApplicationReadyEvent event) {
        // Schedule full cleanup/invalidation of all running tasks
        jobScheduler.enqueue(() -> analysisService.cleanupAllOrphanedRunningTasks(JobContext.Null));
    }
}
