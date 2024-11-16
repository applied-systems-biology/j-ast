package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.ImageAnnotation;
import org.hkijena.jast.payloads.ProjectImageAnnotationPayload;
import org.hkijena.jast.repositories.ImageAnnotationRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.MimeTypeUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@RestController
public class AnnotationController {

    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final ImageAnnotationRepository imageAnnotationRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public AnnotationController(ProjectRepository projectRepository, ImageRepository imageRepository, ImageAnnotationRepository imageAnnotationRepository, ProjectService projectService, UserService userService) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.imageAnnotationRepository = imageAnnotationRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping("/api/image-annotation/{imageId}/{annotationType}")
    public ResponseEntity<ProjectImageAnnotationPayload> getPayload(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) {
        userService.validateAuthentication(authentication);
        ImageAnnotation imageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType);
        return ResponseEntity.ok(ProjectImageAnnotationPayload.create(imageAnnotation));
    }

    @GetMapping("/api/image-annotation/{imageId}/{annotationType}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) throws IOException {
        userService.validateAuthentication(authentication);
        ImageAnnotation imageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType);

        RequestUtils.sendContent(response, imageAnnotation.getRawData(), MimeTypeUtils.MIME_TYPE_PNG);
    }

    private ImageAnnotation getOrCreateImageAnnotation(Authentication authentication, long imageId, String annotationType) {
        Optional<Image> image_ = imageRepository.findById(imageId);
        if (image_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if(!ImageAnnotation.isValidType(annotationType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Image image = image_.get();
        if (!image.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<ImageAnnotation> imageAnnotation_ = imageAnnotationRepository.findFirstByImageAndType(image, annotationType);
        ImageAnnotation imageAnnotation;
        if(imageAnnotation_.isEmpty()) {
            // Create a new annotation
            imageAnnotation = new ImageAnnotation();
            imageAnnotation.setImage(image);
            imageAnnotation.setType(annotationType);
            imageAnnotation.resetToMask(image);

            image.addImageAnnotation(imageAnnotation);
            imageAnnotationRepository.save(imageAnnotation);
        }
        else {
            imageAnnotation = imageAnnotation_.get();
        }
        return imageAnnotation;
    }
}
