package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.*;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.messages.AutoProcessStatusMessage;
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

    private final AccountConfig accountConfig;
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final JobScheduler jobScheduler;
    private final AnalysisService analysisService;
    private final ProjectService projectService;

    @Autowired
    public ProjectController(AccountConfig accountConfig, ProjectRepository projectRepository, ImageRepository imageRepository, JobScheduler jobScheduler, AnalysisService analysisService, ProjectService projectService) {
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
        Optional<Project> project_ = projectRepository.findById(id);
        if (project_.isPresent()) {
            Project project = project_.get();

            if(!project.canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // Iterate through images and organize them
            int numSuccess = 0;
            int numFailures = 0;
            int numLimitReached = 0;
            List<String> failureNames = new ArrayList<>();
            if(imageFile != null && !imageFile.isEmpty()) {
                if(projectService.canUploadImage(project, authentication)) {
                    try {
                        try (InputStream stream = imageFile.getInputStream()) {
                            BufferedImage bufferedImage = ImageIO.read(stream);
                            if (bufferedImage == null) {
                                throw new NullPointerException("Unable to load image!");
                            }

                            // Create object
                            Image image = new Image();
                            image.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
                            image.setImageWidth(bufferedImage.getWidth());
                            image.setImageHeight(bufferedImage.getHeight());
                            image.setRawData(ImageUtils.toPNGByteArray(bufferedImage));
                            image.setThumbnailData(ImageUtils.toPNGByteArray(ImageUtils.createThumbnail(bufferedImage, 128, 128)));

                            project.addImage(image);
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
            projectRepository.save(project);

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
                        "Guests can only upload up to " + accountConfig.getGuestImageLimit() + " images per data set",
                        Notification.Style.danger,
                        redirectAttributes);
            }

            return ResponseEntity.ok("Upload successful");
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
