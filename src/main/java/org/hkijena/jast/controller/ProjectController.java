package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.*;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.TimeSeries;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.messages.AnalysisStatusMessage;
import org.hkijena.jast.model.messages.DatasetUpdateMessage;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.AnalysisService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.hkijena.jast.utils.StringUtils;
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
public class ProjectController {

    private final RuntimeConfig runtimeConfig;

    private final AccountConfig accountConfig;
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final JobScheduler jobScheduler;
    private final AnalysisService analysisService;
    private final ProjectService projectService;

    @Autowired
    public ProjectController(RuntimeConfig runtimeConfig, AccountConfig accountConfig, ProjectRepository projectRepository, ImageRepository imageRepository, JobScheduler jobScheduler, AnalysisService analysisService, ProjectService projectService) {
        this.runtimeConfig = runtimeConfig;
        this.accountConfig = accountConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.jobScheduler = jobScheduler;
        this.analysisService = analysisService;
        this.projectService = projectService;
    }

    @GetMapping("/project/new")
    public ModelAndView newProject(Model model, Authentication authentication) throws IOException {
        if(authentication == null || !authentication.isAuthenticated() || !authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(!projectService.canCreateProject(authentication)) {
            return new ModelAndView("redirect:/");
        }
        Project project = new Project();
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            project.setOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        }
        project = projectRepository.save(project);
        return new ModelAndView("redirect:/project/view/" + project.getId());
    }

    @GetMapping("/project/view/{id}")
    public ModelAndView viewProject(Model model, Authentication authentication, @PathVariable long id) {
        Optional<Project> project_ = projectRepository.findById(id);
        if (project_.isPresent()) {
            Project project = project_.get();

            if(!project.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            projectRepository.putSortedToModel(model, authentication);
            model.addAttribute("currentProject", project);
            model.addAttribute("currentProjectId", project.getId());

            // Add owner information
            if(project.isOwnedBy(authentication)) {
                model.addAttribute("currentProjectOwnedByOtherUser", false);
            }
            else {
                model.addAttribute("currentProjectOwnedByOtherUser", true);
                model.addAttribute("currentProjectOwner", project.getOwner() != null ? project.getOwner().getEmail() : accountConfig.getAdminUsername());
            }

            // Add limits info
            model.addAttribute("guestImageLimit", accountConfig.getGuestImageLimit());

            return new ModelAndView("dataset-editor");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/project/delete/{id}")
    public ModelAndView deleteProject(Authentication authentication, RedirectAttributes redirectAttributes, @PathVariable long id) {
        Optional<Project> project_ = projectRepository.findById(id);
        if (project_.isPresent()) {

            Project project = project_.get();

            if(!project.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            projectService.delete(project);

            Notification.pushToRedirect("Project deleted", "The project '" + project.getName() + "' was deleted.", Notification.Style.success, redirectAttributes);
            return new ModelAndView("redirect:/");
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/project/rename/{id}")
    public ModelAndView renameDataset(Authentication authentication, @PathVariable long id, @RequestParam("datasetName") String datasetName) {
        Optional<Project> project_ = projectRepository.findById(id);
        if (project_.isPresent()) {
            Project project = project_.get();

            if(!project.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            datasetName = StringUtils.nullToEmpty(datasetName).trim();
            if(StringUtils.isNullOrEmpty(datasetName)) {
                datasetName = "Unnamed";
            }
            project.setName(datasetName);
            projectRepository.save(project);
            return new ModelAndView("redirect:/project/view/" + id);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/project/upload/{id}")
    public ResponseEntity<?> uploadFileToDataset(Authentication authentication, @PathVariable long id, RedirectAttributes redirectAttributes, @RequestParam("imageFile") MultipartFile imageFile) {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent()) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(timeSeries.getStatus() != TimeSeries.Status.Preparing) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // Create target directory
            Path targetDir = Paths.get(timeSeries.getStoragePath()).resolve("inputs_raw");
            Path targetDirThumbnails = Paths.get(timeSeries.getStoragePath()).resolve("inputs_raw_thumbnails");
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
                if(projectService.canUploadImage(timeSeries, authentication)) {
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
                            Image inputData = new Image();
                            inputData.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            inputData.setStoragePath(imageStoragePath.toString());
                            inputData.setThumbnailStoragePath(thumbnailStoragePath.toString());
                            inputData.tryAutoFill(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            inputData.setImageWidth(image.getWidth());
                            inputData.setImageHeight(image.getHeight());

                            timeSeries.addImage(inputData);
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
            timeSeriesRepository.save(timeSeries);

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
                        "Guests can only upload up to " + accountConfig.getguestImageLimit() + " images per data set",
                        Notification.Style.danger,
                        redirectAttributes);
            }

            return ResponseEntity.ok("Upload successful");
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/project/update/{id}")
    public void update(HttpServletResponse response, Authentication authentication, @PathVariable long id, @RequestBody DatasetUpdateMessage message) {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent()) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(timeSeries.getStatus() == TimeSeries.Status.Preparing) {
                message.getParametersUpdateMessage().update(timeSeries);
                timeSeriesRepository.save(timeSeries);
                for (DatasetUpdateMessage.InputDataUpdateMessage inputDataUpdateMessage : message.getInputDataUpdateMessageMap().values()) {
                    Optional<Image> inputData_ = imageRepository.findById(inputDataUpdateMessage.getId());
                    if (inputData_.isPresent()) {
                        Image image = inputData_.get();
                        inputDataUpdateMessage.update(image);
                        imageRepository.save(image);
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

    @PostMapping("/project/run/{id}")
    public ModelAndView run(RedirectAttributes redirectAttributes, Authentication authentication, @PathVariable long id) {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent()) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(timeSeries.getStatus() == TimeSeries.Status.Preparing) {
                if (timeSeries.validate().isValid()) {

                    Path logFilePath = Paths.get(timeSeries.getStoragePath()).resolve("log.txt");
                    try {
                        Files.deleteIfExists(logFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    timeSeries.setStatus(TimeSeries.Status.Running);
                    timeSeriesRepository.save(timeSeries);
                    jobScheduler.enqueue(() -> analysisService.runAnalysis(id, JobContext.Null));
                    return new ModelAndView("redirect:/project/view/" + id);
                } else {
                    Notification.pushToRedirect("Dataset is invalid!", "Validation checks failed.", Notification.Style.danger, redirectAttributes);
                    return new ModelAndView("redirect:/project/view/" + id);
                }
            }
            else {
                Notification.pushToRedirect("Dataset is not ready!", "An analysis is currently in progress.", Notification.Style.danger, redirectAttributes);
                return new ModelAndView("redirect:/project/view/" + id);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/project/reset/{id}")
    public ModelAndView reset(RedirectAttributes redirectAttributes, Authentication authentication, @PathVariable long id) {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent()) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            timeSeries.tryCancelCurrentJob();

            timeSeries.setStatus(TimeSeries.Status.Preparing);
            timeSeries.clearOutputData();
            timeSeriesRepository.save(timeSeries);
            Notification.pushToRedirect("Dataset reset", "You can now edit all parameters and modify the inputs.", Notification.Style.info, redirectAttributes);
            return new ModelAndView("redirect:/project/view/" + id);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/query-status/{id}")
    public ResponseEntity<AnalysisStatusMessage> queryDatasetStatus(Authentication authentication, @PathVariable long id) {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent()) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            AnalysisStatusMessage message = new AnalysisStatusMessage();
            message.setStatus(timeSeries.getStatus());

            StringBuilder stringBuilder = new StringBuilder();

            try {
                Path logFilePath = Paths.get(timeSeries.getStoragePath()).resolve("log.txt");
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

    @GetMapping("/project/download-log/{id}")
    public void downloadDatasetLog(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path logFilePath = Paths.get(timeSeries.getStoragePath()).resolve("log.txt");
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

    @GetMapping("/project/download-results/zip/{id}")
    public void downloadAllResultsAsZIP(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(timeSeries.getStoragePath()).resolve("results").resolve("results.zip");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(timeSeries.getName()) + "-results.zip");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/download-results/xlsx-per-experiment/{id}")
    public void downloadXLSXPerExperimentResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(timeSeries.getStoragePath()).resolve("project").resolve("results").resolve("results_per_experiment.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(timeSeries.getName()) + "-results_per_experiment.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/download-results/xlsx-classic/{id}")
    public void downloadXLSXClassicResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(timeSeries.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one_classic.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(timeSeries.getName()) + "-results_all_in_one_classic.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/download-results/xlsx-all-in-one/{id}")
    public void downloadXLSXAllInOneResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(timeSeries.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one.xlsx");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(timeSeries.getName()) + "-results_all_in_one.xlsx");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/download-results/csv/{id}")
    public void downloadCSVAllInOneResults(HttpServletResponse httpServletResponse, Authentication authentication, @PathVariable long id) throws IOException {
        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(id);
        if(dataset_.isPresent() && dataset_.get().getStatus() != TimeSeries.Status.Preparing) {
            TimeSeries timeSeries = dataset_.get();

            if(!timeSeries.canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Path resultsFilePath = Paths.get(timeSeries.getStoragePath()).resolve("project").resolve("results").resolve("results_all_in_one.csv");
            if(Files.isRegularFile(resultsFilePath)) {
                RequestUtils.sendAttachment(httpServletResponse, resultsFilePath, StringUtils.makeFilesystemCompatible(timeSeries.getName()) + "-results_all_in_one.csv");
            }
            else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/project/query-all-status")
    public ResponseEntity<Map<Long, TimeSeries.Status>> queryAllDatasetStatus(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Map<Long, TimeSeries.Status> result = new HashMap<>();
        for (TimeSeries timeSeries : timeSeriesRepository.getByAuthentication(authentication)) {
            result.put(timeSeries.getId(), timeSeries.getStatus());
        }
        return ResponseEntity.ok(result);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarting(ApplicationReadyEvent event) {
        // Schedule full cleanup/invalidation of all running tasks
        jobScheduler.enqueue(() -> analysisService.cleanupAllOrphanedRunningTasks(JobContext.Null));
    }
}
