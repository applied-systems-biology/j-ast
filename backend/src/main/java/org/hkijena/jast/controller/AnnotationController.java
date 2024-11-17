package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.payloads.MaskImageAnnotationPayload;
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

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}")
    public ResponseEntity<MaskImageAnnotationPayload> getPayload(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType);
        return ResponseEntity.ok(MaskImageAnnotationPayload.create(maskImageAnnotation));
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) throws IOException {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType);

        RequestUtils.sendContent(response, maskImageAnnotation.getRawData(), MimeTypeUtils.MIME_TYPE_PNG);
    }

    private MaskImageAnnotation getOrCreateImageAnnotation(Authentication authentication, long imageId, String annotationType) {
        Optional<Image> image_ = imageRepository.findById(imageId);
        if (image_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if(!MaskImageAnnotation.isValidType(annotationType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Image image = image_.get();
        if (!image.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<MaskImageAnnotation> imageAnnotation_ = imageAnnotationRepository.findFirstByImageAndType(image, annotationType);
        MaskImageAnnotation maskImageAnnotation;
        if(imageAnnotation_.isEmpty()) {
            // Create a new annotation
            maskImageAnnotation = new MaskImageAnnotation();
            maskImageAnnotation.setImage(image);
            maskImageAnnotation.setType(annotationType);
            maskImageAnnotation.resetToMask(image);

            image.addImageAnnotation(maskImageAnnotation);
            imageAnnotationRepository.save(maskImageAnnotation);
        }
        else {
            maskImageAnnotation = imageAnnotation_.get();
        }
        return maskImageAnnotation;
    }
}
