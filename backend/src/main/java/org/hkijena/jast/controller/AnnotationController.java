package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.payloads.MaskImageAnnotationPayload;
import org.hkijena.jast.repositories.MaskImageAnnotationRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.MimeTypeUtils;
import org.hkijena.jast.utils.RequestUtils;
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
import java.util.Optional;

@RestController
public class AnnotationController {

    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final MaskImageAnnotationRepository maskImageAnnotationRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public AnnotationController(ProjectRepository projectRepository, ImageRepository imageRepository, MaskImageAnnotationRepository maskImageAnnotationRepository, ProjectService projectService, UserService userService) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.maskImageAnnotationRepository = maskImageAnnotationRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @PostMapping("/api/mask-image-annotation/{imageId}/{annotationType}/raw")
    public void updateRaw(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType, @RequestPart("file") MultipartFile imageFile) {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType, true);

        try (InputStream stream = imageFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
            }

            // Check if size is correct
            if(bufferedImage.getWidth() != maskImageAnnotation.getImage().getImageWidth() || bufferedImage.getHeight() != maskImageAnnotation.getImage().getImageHeight()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data (wrong size)");
            }

            maskImageAnnotation.setRawData(ImageUtils.toPNGByteArray(bufferedImage));
            maskImageAnnotation.setThumbnailData(ImageUtils.toPNGByteArrayThumbnail(bufferedImage));
            maskImageAnnotationRepository.save(maskImageAnnotation);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
        }
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}")
    public ResponseEntity<MaskImageAnnotationPayload> getPayload(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType, false);
        return ResponseEntity.ok(MaskImageAnnotationPayload.create(maskImageAnnotation));
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) throws IOException {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = getOrCreateImageAnnotation(authentication, imageId, annotationType, false);

        RequestUtils.sendContent(response, maskImageAnnotation.getRawData(), MimeTypeUtils.MIME_TYPE_PNG);
    }

    private MaskImageAnnotation getOrCreateImageAnnotation(Authentication authentication, long imageId, String annotationType, boolean edit) {
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
        if (edit && !image.getProject().canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<MaskImageAnnotation> imageAnnotation_ = maskImageAnnotationRepository.findFirstByImageAndType(image, annotationType);
        MaskImageAnnotation maskImageAnnotation;
        if(imageAnnotation_.isEmpty()) {
            // Create a new annotation
            maskImageAnnotation = new MaskImageAnnotation();
            maskImageAnnotation.setImage(image);
            maskImageAnnotation.setType(annotationType);
            maskImageAnnotation.resetToMask(image);

            image.addImageAnnotation(maskImageAnnotation);
            maskImageAnnotationRepository.save(maskImageAnnotation);
        }
        else {
            maskImageAnnotation = imageAnnotation_.get();
        }
        return maskImageAnnotation;
    }
}
