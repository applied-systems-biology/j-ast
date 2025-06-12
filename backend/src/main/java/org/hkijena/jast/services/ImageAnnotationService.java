package org.hkijena.jast.services;

import jakarta.transaction.Transactional;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.WebSecurityConfig;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.MaskImageAnnotationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ImageAnnotationService {

    private final ImageRepository imageRepository;
    private final MaskImageAnnotationRepository maskImageAnnotationRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public ImageAnnotationService(ImageRepository imageRepository, MaskImageAnnotationRepository maskImageAnnotationRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.maskImageAnnotationRepository = maskImageAnnotationRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public MaskImageAnnotation getOrCreateMaskImageAnnotation(Authentication authentication, AccountConfig accountConfig, long imageId, String annotationType, boolean edit) {
        Optional<Image> image_ = imageRepository.findById(imageId);
        if (image_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!MaskImageAnnotation.isValidType(annotationType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Image image = image_.get();
        if (!image.getProject().canAccess(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (edit && !image.getProject().canEdit(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Optional<MaskImageAnnotation> imageAnnotation_ = maskImageAnnotationRepository.findFirstByImageAndType(image, annotationType);
        if (imageAnnotation_.isEmpty()) {
            // Create a new annotation
            MaskImageAnnotation maskImageAnnotation = new MaskImageAnnotation();
            maskImageAnnotation.setImage(image);
            maskImageAnnotation.setType(annotationType);
            maskImageAnnotation.resetToEmptyMask(fileStorageService, image);

            image.addMaskImageAnnotation(maskImageAnnotation);
            return maskImageAnnotationRepository.save(maskImageAnnotation);
        } else {
            return imageAnnotation_.get();
        }
    }
}
