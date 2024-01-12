package org.hkijena.jipipe.webapp.growthassay.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.InputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.services.AnalysisService;
import org.hkijena.jipipe.webapp.growthassay.services.DatasetService;
import org.hkijena.jipipe.webapp.growthassay.services.UserService;
import org.hkijena.jipipe.webapp.growthassay.utils.ImageUtils;
import org.hkijena.jipipe.webapp.growthassay.utils.RequestUtils;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class DatasetController {

    private final RuntimeConfig runtimeConfig;

    private final AccountConfig accountConfig;
    private final DatasetRepository datasetRepository;
    private final InputDataRepository inputDataRepository;
    private final JobScheduler jobScheduler;
    private final AnalysisService analysisService;
    private final DatasetService datasetService;

    @Autowired
    public DatasetController(RuntimeConfig runtimeConfig, AccountConfig accountConfig, DatasetRepository datasetRepository, InputDataRepository inputDataRepository, JobScheduler jobScheduler, AnalysisService analysisService, DatasetService datasetService) {
        this.runtimeConfig = runtimeConfig;
        this.accountConfig = accountConfig;
        this.datasetRepository = datasetRepository;
        this.inputDataRepository = inputDataRepository;
        this.jobScheduler = jobScheduler;
        this.analysisService = analysisService;
        this.datasetService = datasetService;
    }

    @GetMapping("/dataset/new")
    public ModelAndView newDataset(Model model, Authentication authentication) throws IOException {
        if(authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(!datasetService.canCreateProject(authentication)) {
            return new ModelAndView("redirect:/");
        }
        Dataset dataset = new Dataset();
        Path storageDir;
        if(StringUtils.isNullOrEmpty(runtimeConfig.getCustomTempDirectory())) {
            storageDir = Files.createTempDirectory("jip-webapp");
        }
        else {
            storageDir = Files.createTempDirectory(Paths.get(runtimeConfig.getCustomTempDirectory()), "jip-webapp");
        }
        dataset.setStoragePath(storageDir.toString());
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            dataset.setOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        }
        dataset = datasetRepository.save(dataset);
        return new ModelAndView("redirect:/dataset/view/" + dataset.getId());
    }

    @GetMapping("/dataset/view/{id}")
    public ModelAndView viewDataset(Model model, Authentication authentication, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if (dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            datasetRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentDataset", dataset);
            model.addAttribute("currentDatasetId", dataset.getId());

            // Add owner information
            if(dataset.isOwnedBy(authentication)) {
                model.addAttribute("currentDatasetOwnedByOtherUser", false);
            }
            else {
                model.addAttribute("currentDatasetOwnedByOtherUser", true);
                model.addAttribute("currentDatasetOwner", dataset.getOwner() != null ? dataset.getOwner().getEmail() : accountConfig.getAdminUsername());
            }

            // Add limits info
            model.addAttribute("guestInputDataLimit", accountConfig.getGuestInputDataLimit());

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
    public ModelAndView deleteDataset(Authentication authentication, RedirectAttributes redirectAttributes, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {

            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(dataset.getStatus() == Dataset.Status.Running) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            datasetService.delete(dataset);

            Notification.pushToRedirect("Dataset deleted", "The dataset '" + dataset.getName() + "' was deleted.", Notification.Style.success, redirectAttributes);
            return new ModelAndView("redirect:/");
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dataset/rename/{id}")
    public ModelAndView renameDataset(Authentication authentication, @PathVariable long id, @RequestParam("datasetName") String datasetName) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

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
    public ResponseEntity<ValidationResult> validateDataset(Authentication authentication, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {

            Dataset dataset = dataset_.get();
            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return ResponseEntity.ok(dataset.validate());
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dataset/upload-input/{id}")
    public ResponseEntity<?> uploadFileToDataset(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes, @RequestParam("imageFile") MultipartFile imageFile) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

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
            int numLimitReached = 0;
            List<String> failureNames = new ArrayList<>();
            if(imageFile != null && !imageFile.isEmpty()) {
                if(datasetService.canUploadInput(dataset, authentication)) {
                    try {
                        try (InputStream stream = imageFile.getInputStream()) {
                            BufferedImage image = ImageIO.read(stream);
                            if (image == null) {
                                throw new NullPointerException("Unable to load image!");
                            }

                            // Re-save as PNG
                            Path imageStoragePath = Files.createTempFile(targetDir, "img", ".png");
                            Path thumbnailStoragePath = targetDirThumbnails.resolve(imageStoragePath.getFileName());
                            ImageIO.write(image, "PNG", imageStoragePath.toFile());

                            // Create thumbnail
                            ImageUtils.createThumbnail(image, 64, 64, thumbnailStoragePath);

                            // Create object
                            InputData inputData = new InputData();
                            inputData.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            inputData.setStoragePath(imageStoragePath.toString());
                            inputData.setThumbnailStoragePath(thumbnailStoragePath.toString());
                            inputData.tryAutoFill(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            inputData.setImageWidth(image.getWidth());
                            inputData.setImageHeight(image.getHeight());

                            dataset.addInputData(inputData);
                            ++numSuccess;
                        }
                    } catch (Throwable e) {
                        ++numFailures;

                        if (!StringUtils.isNullOrEmpty(imageFile.getOriginalFilename())) {
                            failureNames.add(imageFile.getOriginalFilename());
                        }
                    }
                }
                else {
                    ++numLimitReached;
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
            if(numLimitReached > 0) {
                Notification.pushToRedirect("Input image limit reached",
                        "Guests can only upload up to " + accountConfig.getGuestInputDataLimit() + " images per data set",
                        Notification.Style.danger,
                        redirectAttributes);
            }

            return ResponseEntity.ok("Upload successful");
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/dataset/update/{id}")
    public void update(HttpServletResponse response, Authentication authentication, @PathVariable long id, @RequestBody DatasetUpdateMessage message) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

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
    public ModelAndView run(RedirectAttributes redirectAttributes, Authentication authentication, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(dataset.getStatus() == Dataset.Status.Preparing) {
                if (dataset.validate().isValid()) {

                    Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
                    try {
                        Files.deleteIfExists(logFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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
    public ModelAndView reset(RedirectAttributes redirectAttributes, Authentication authentication, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            dataset.tryCancelCurrentJob();

            dataset.setStatus(Dataset.Status.Preparing);
            dataset.clearOutputData();
            datasetRepository.save(dataset);
            Notification.pushToRedirect("Dataset reset", "You can now edit all parameters and modify the inputs.", Notification.Style.info, redirectAttributes);
            return new ModelAndView("redirect:/dataset/view/" + id);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/query-status/{id}")
    public ResponseEntity<AnalysisStatusMessage> queryDatasetStatus(Authentication authentication, @PathVariable long id) {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            AnalysisStatusMessage message = new AnalysisStatusMessage();
            message.setStatus(dataset.getStatus());

            StringBuilder stringBuilder = new StringBuilder();

            try {
                Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
                if(Files.isRegularFile(logFilePath)) {
                    List<String> lines = Files.readAllLines(logFilePath);
                    for (int i = Math.max(0, lines.size() - 500 - 1); i < lines.size(); i++) {
                        stringBuilder.append(lines.get(i)).append("\n");
                    }
                }
                else {
                    stringBuilder.append("[QUEUE] Job is enqueued. Please wait ...");
                }
            }
            catch (IOException ignored) {
            }

            message.setLog(stringBuilder.toString());

            return ResponseEntity.ok(message);
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dataset/download-log/{id}")
    public void downloadDatasetLog(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
            if(Files.isRegularFile(logFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, logFilePath, "log.txt");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/download-results/zip/{id}")
    public void downloadAllResultsAsZIP(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(dataset.getStoragePath()).resolve("results").resolve("results.zip");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(dataset.getName()) + "-results.zip");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/download-results/xlsx-per-experiment/{id}")
    public void downloadXLSXPerExperimentResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(dataset.getStoragePath()).resolve("project").resolve("results").resolve("results_per_experiment.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(dataset.getName()) + "-results_per_experiment.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/download-results/xlsx-classic/{id}")
    public void downloadXLSXClassicResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(dataset.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one_classic.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(dataset.getName()) + "-results_all_in_one_classic.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/download-results/xlsx-all-in-one/{id}")
    public void downloadXLSXAllInOneResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(dataset.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(dataset.getName()) + "-results_all_in_one.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/download-results/csv/{id}")
    public void downloadCSVAllInOneResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<Dataset> dataset_ = datasetRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != Dataset.Status.Preparing) {
            Dataset dataset = dataset_.get();

            if(!dataset.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(dataset.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one.csv");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(dataset.getName()) + "-results_all_in_one.csv");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/dataset/query-all-status")
    public ResponseEntity<Map<Long, Dataset.Status>> queryAllDatasetStatus(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Map<Long, Dataset.Status> result = new HashMap<>();
        for (Dataset dataset : datasetRepository.getByAuthentication(authentication)) {
            result.put(dataset.getId(), dataset.getStatus());
        }
        return ResponseEntity.ok(result);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarting(ApplicationReadyEvent event) {
        // Schedule full cleanup/invalidation of all running tasks
        jobScheduler.enqueue(() -> analysisService.cleanupAllOrphanedRunningTasks(JobContext.Null));
    }
}
