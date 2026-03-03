package org.hkijena.jast.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.WebSecurityConfig;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.CreateEditProjectRequest;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.payloads.ProjectMetadataPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.BackendTaskService;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ProjectController {

    private final AccountConfig accountConfig;
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final BackendTaskService backendTaskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProjectController(AccountConfig accountConfig, ProjectRepository projectRepository, ImageRepository imageRepository, BackendTaskService backendTaskService, ProjectService projectService, UserService userService, FileStorageService fileStorageService) {
        this.accountConfig = accountConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.backendTaskService = backendTaskService;
        this.projectService = projectService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/api/list-projects")
    public ResponseEntity<List<ProjectMetadataPayload>> listProjects(Authentication authentication) {
        userService.validateAuthentication(authentication);
        ArrayList<ProjectMetadataPayload> result = new ArrayList<>();
        for (Project project : projectRepository.getByAuthentication(authentication, accountConfig)) {
            result.add(new ProjectMetadataPayload(project));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/project/{id}")
    public ResponseEntity<ProjectMetadataPayload> getProject(Authentication authentication, @PathVariable("id") long id) {
        userService.validateAuthentication(authentication);
        return ResponseEntity.ok(new ProjectMetadataPayload(projectService.getProjectByIdOrError(id)));
    }

    @PostMapping("/api/project/{id}/edit")
    public ResponseEntity<ProjectMetadataPayload> editProject(Authentication authentication, @PathVariable("id") long id, @RequestBody CreateEditProjectRequest request) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (project.canEdit(authentication, accountConfig)) {
            project.setName(StringUtils.orElse(request.getName(), "Unnamed project"));
            project.setViewMode(request.getViewMode() != null ? request.getViewMode() : ViewMode.Timeline);
            project.markAsModified();
            projectRepository.save(project);
            return ResponseEntity.ok(new ProjectMetadataPayload(project));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/api/new-project")
    public ResponseEntity<ProjectMetadataPayload> createProject(Authentication authentication, @RequestBody CreateEditProjectRequest request) {
        userService.validateAuthentication(authentication);
        if (!projectService.canCreateProject(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Project project = new Project();
        project.setName(StringUtils.orElse(request.getName(), "Unnamed project"));
        project.setViewMode(request.getViewMode());
        project.setOwner(userService.authenticationToUser(authentication));
        project = projectRepository.save(project);
        return ResponseEntity.ok(new ProjectMetadataPayload(project));
    }

    @PostMapping("/api/project/{id}/upload-raw-image")
    public ImagePayload uploadRawImage(Authentication authentication, @PathVariable("id") long id, @RequestPart("file") MultipartFile imageFile) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!projectService.canUploadImage(project, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Image image;

        try (InputStream stream = imageFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
            }

            // Create object
            image = new Image();
            image.setOriginalFileName(StringUtils.nullToEmpty(imageFile.getOriginalFilename()));
            image.setImageWidth(bufferedImage.getWidth());
            image.setImageHeight(bufferedImage.getHeight());
            image.setRawData(fileStorageService, ImageUtils.toPNGByteArray(bufferedImage));
            image.rebuildThumbnail(fileStorageService, bufferedImage);

            project.addImage(image);
            image = imageRepository.save(image);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
        }

        project.markAsModified();
        projectRepository.save(project);

        return new ImagePayload(image);
    }

    @PostMapping("/api/project/{id}/delete")
    public void deleteProject(Authentication authentication, @PathVariable("id") long id) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canEdit(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        project.deleteFilesLater(fileStorageService);
        projectRepository.delete(project);
    }

}
