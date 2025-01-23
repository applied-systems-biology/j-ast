package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.payloads.MaskImageAnnotationPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.MaskImageAnnotationRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ImageAnnotationService;
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
    private final FileStorageService fileStorageService;
    private final ImageAnnotationService imageAnnotationService;

    @Autowired
    public AnnotationController(ProjectRepository projectRepository, ImageRepository imageRepository, MaskImageAnnotationRepository maskImageAnnotationRepository, ProjectService projectService, UserService userService, FileStorageService fileStorageService, ImageAnnotationService imageAnnotationService) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.maskImageAnnotationRepository = maskImageAnnotationRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.imageAnnotationService = imageAnnotationService;
    }

    @PostMapping("/api/mask-image-annotation/{imageId}/{annotationType}/raw")
    public void updateRaw(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType, @RequestPart("file") MultipartFile imageFile) {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = imageAnnotationService.getOrCreateMaskImageAnnotation(authentication, imageId, annotationType, true);

        try (InputStream stream = imageFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
            }

            // Check if size is correct
            if (bufferedImage.getWidth() != maskImageAnnotation.getImage().getImageWidth() || bufferedImage.getHeight() != maskImageAnnotation.getImage().getImageHeight()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data (wrong size)");
            }

            maskImageAnnotation.setRawData(fileStorageService, ImageUtils.toPNGByteArray(bufferedImage));
            maskImageAnnotation.setThumbnailData(fileStorageService, ImageUtils.toPNGByteArrayThumbnail(bufferedImage));
            maskImageAnnotation.incrementVersion();
            maskImageAnnotationRepository.save(maskImageAnnotation);

            // Update the image thumbnail
            maskImageAnnotation.getImage().rebuildThumbnail(fileStorageService);
            maskImageAnnotation.getImage().incrementVersion();
            imageRepository.save(maskImageAnnotation.getImage());

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
        }
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}")
    public ResponseEntity<MaskImageAnnotationPayload> getPayload(Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = imageAnnotationService.getOrCreateMaskImageAnnotation(authentication, imageId, annotationType, false);
        return ResponseEntity.ok(MaskImageAnnotationPayload.create(maskImageAnnotation));
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) throws IOException {
        userService.validateAuthentication(authentication);
        MaskImageAnnotation maskImageAnnotation = imageAnnotationService.getOrCreateMaskImageAnnotation(authentication, imageId, annotationType, false);

        RequestUtils.sendContent(response, maskImageAnnotation.getRawData(fileStorageService), MimeTypeUtils.MIME_TYPE_PNG);
    }

    @GetMapping("/api/mask-image-annotation/{imageId}/{annotationType}/thumbnail")
    public void getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long imageId, @PathVariable String annotationType) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<Image> image_ = imageRepository.findById(imageId);
        if (image_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!MaskImageAnnotation.isValidType(annotationType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Image image = image_.get();
        if (!image.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Optional<MaskImageAnnotation> imageAnnotation_ = maskImageAnnotationRepository.findFirstByImageAndType(image, annotationType);
        if (imageAnnotation_.isPresent()) {
            RequestUtils.sendContent(response, imageAnnotation_.get().getThumbnailData(fileStorageService), MimeTypeUtils.MIME_TYPE_PNG);
        } else {
            // Send dummy thumbnail
            RequestUtils.sendContent(response, ImageUtils.getDummyThumbnailBytes(), MimeTypeUtils.MIME_TYPE_PNG);
        }
    }


}
