package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.payloads.ProjectImagesPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.MimeTypeUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ImageController {
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ImageController(ProjectRepository projectRepository, ImageRepository imageRepository, ProjectService projectService, UserService userService, FileStorageService fileStorageService) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/api/project/{id}/images")
    public ResponseEntity<ProjectImagesPayload> getImagesPayload(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(new ProjectImagesPayload(project));
    }

    @GetMapping("/api/image/{id}")
    public ResponseEntity<ImagePayload> getImagePayload(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return ResponseEntity.ok(new ImagePayload(image));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/api/project/{id}/list-images")
    public ResponseEntity<List<ImagePayload>> listImages(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        List<ImagePayload> result = new ArrayList<>();
        for (Image image : project.getImages()) {
            result.add(new ImagePayload(image));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/image/{id}/thumbnail")
    public void getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, image.getThumbnailData(fileStorageService), MimeTypeUtils.MIME_TYPE_PNG);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/api/image/{id}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canAccess(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            RequestUtils.sendContent(response, image.getRawData(fileStorageService), MimeTypeUtils.MIME_TYPE_PNG);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/api/image/{id}/delete")
    public void deleteImage(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Optional<Image> inputData_ = imageRepository.findById(id);
        if (inputData_.isPresent()) {
            Image image = inputData_.get();

            if (!image.getProject().canEdit(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            image.deleteFilesLater(fileStorageService);
            imageRepository.delete(image);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/api/project/{id}/update-images")
    public void updateImages(Authentication authentication, @PathVariable long id, @RequestBody ProjectImagesPayload imagesPayload) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Check if the images are actually owned by the project
        Set<Long> imageIdsInProject = project.getImages().stream().map(Image::getId).collect(Collectors.toSet());
        for (Map.Entry<Long, ImagePayload> entry : imagesPayload.getImagesById().entrySet()) {
            if (entry.getKey() != entry.getValue().getId()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inconsistent data");
            }
            if (entry.getValue().getProjectId() != project.getId()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inconsistent data");
            }
            if (!imageIdsInProject.contains(entry.getKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inconsistent data");
            }
        }

        // Update the images in the database
        List<Image> images = new ArrayList<>();
        for (Map.Entry<Long, ImagePayload> entry : imagesPayload.getImagesById().entrySet()) {
            Image image = imageRepository.findById(entry.getKey()).orElseThrow();
            ImagePayload payload = entry.getValue();
            image.updateFromPayload(payload);
            images.add(image);
        }
        imageRepository.saveAll(images);

        images = project.fixImageTableConsistency();
        imageRepository.saveAll(images);
    }

    @PostMapping("/api/image/{id}/update")
    public void updateImage(Authentication authentication, @PathVariable long id, @RequestBody ImagePayload imagePayload) {
        userService.validateAuthentication(authentication);
        Optional<Image> image_ = imageRepository.findById(id);
        if (image_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Image image = image_.get();
        if (!image.getProject().canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        image.updateFromPayload(imagePayload);
        imageRepository.save(image);

        imageRepository.saveAll(image.getProject().fixImageTableConsistency());
    }
}
