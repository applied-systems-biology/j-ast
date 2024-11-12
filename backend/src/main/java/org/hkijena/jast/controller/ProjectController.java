package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.CreateEditProjectRequest;
import org.hkijena.jast.payloads.ProjectInfoMessage;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.AnalysisService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.StringUtils;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ProjectController {

    private final AccountConfig accountConfig;
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final JobScheduler jobScheduler;
    private final AnalysisService analysisService;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public ProjectController(AccountConfig accountConfig, ProjectRepository projectRepository, ImageRepository imageRepository, JobScheduler jobScheduler, AnalysisService analysisService, ProjectService projectService, UserService userService) {
        this.accountConfig = accountConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.jobScheduler = jobScheduler;
        this.analysisService = analysisService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping("/api/list-projects")
    public ResponseEntity<List<ProjectInfoMessage>> listProjects(Authentication authentication) {
        userService.validateAuthentication(authentication);
        ArrayList<ProjectInfoMessage> result = new ArrayList<>();
        for (Project project : projectRepository.getByAuthentication(authentication)) {
            result.add(ProjectInfoMessage.create(project));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/project/{id}")
    public ResponseEntity<ProjectInfoMessage> getProject(Authentication authentication, @PathVariable("id") long id) {
        userService.validateAuthentication(authentication);
        return ResponseEntity.ok(ProjectInfoMessage.create(projectService.getProjectByIdOrError(id)));
    }

    @PostMapping("/api/project/{id}/edit")
    public ResponseEntity<ProjectInfoMessage> editProject(Authentication authentication, @PathVariable("id") long id, @RequestBody CreateEditProjectRequest request) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if(project.canEdit(authentication)) {
            project.setName(StringUtils.orElse(request.getName(), "Unnamed project"));
            projectRepository.save(project);
            return ResponseEntity.ok(ProjectInfoMessage.create(project));
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/api/new-project")
    public ResponseEntity<ProjectInfoMessage> createProject(Authentication authentication, @RequestBody CreateEditProjectRequest request) {
        userService.validateAuthentication(authentication);
        if (!projectService.canCreateProject(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Project project = new Project();
        project.setName(StringUtils.orElse(request.getName(), "Unnamed project"));
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            project.setOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        }
        project = projectRepository.save(project);
        return ResponseEntity.ok(ProjectInfoMessage.create(project));
    }

    @PostMapping("/api/project/{id}/upload-raw-image")
    public void uploadRawImage(Authentication authentication, @PathVariable("id") long id, @RequestPart("file") MultipartFile imageFile) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if(!projectService.canUploadImage(project, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try (InputStream stream = imageFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
            }

            // Create object
            Image image = new Image();
            image.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
            image.setImageWidth(bufferedImage.getWidth());
            image.setImageHeight(bufferedImage.getHeight());
            image.setRawData(ImageUtils.toPNGByteArray(bufferedImage));
            image.setThumbnailData(ImageUtils.toPNGByteArray(ImageUtils.createThumbnail(bufferedImage, 128, 128)));

            project.addImage(image);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
        }

        projectRepository.save(project);
    }

    @PostMapping("/api/project/{id}/delete")
    public void deleteProject(Authentication authentication, @PathVariable("id") long id) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if(!project.canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        projectRepository.delete(project);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarting(ApplicationReadyEvent event) {
        // Schedule full cleanup/invalidation of all running tasks
        jobScheduler.enqueue(() -> analysisService.cleanupAllOrphanedRunningTasks(JobContext.Null));
    }
}
