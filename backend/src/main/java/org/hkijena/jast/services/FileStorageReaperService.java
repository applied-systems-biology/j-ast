/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.services;

import jakarta.annotation.PostConstruct;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.MaskImageAnnotationRepository;
import org.hkijena.jast.repositories.ResultItemRepository;
import org.hkijena.jast.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class FileStorageReaperService {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileStorageReaperService.class);

    private final FileStorageService fileStorageService;
    private final ImageRepository imageRepository;
    private final ResultItemRepository resultItemRepository;
    private final MaskImageAnnotationRepository maskImageAnnotationRepository;

    @Autowired
    public FileStorageReaperService(FileStorageService fileStorageService, ImageRepository imageRepository, ResultItemRepository resultItemRepository, MaskImageAnnotationRepository maskImageAnnotationRepository) {
        this.fileStorageService = fileStorageService;
        this.imageRepository = imageRepository;
        this.resultItemRepository = resultItemRepository;
        this.maskImageAnnotationRepository = maskImageAnnotationRepository;
    }

    @PostConstruct
    public void deleteUnusedFiles() {
        LOGGER.info("Looking for unused files...");
        Set<String> usedIds = new HashSet<>();
        LOGGER.info("Looking for unused files... images");
        for (Image image : imageRepository.findAll()) {
            if (!StringUtils.isNullOrEmpty(image.getRawDataFileId())) {
                usedIds.add(image.getRawDataFileId());
            }
            if (!StringUtils.isNullOrEmpty(image.getThumbnailDataFileId())) {
                usedIds.add(image.getThumbnailDataFileId());
            }
        }
        LOGGER.info("Looking for unused files... annotations");
        for (MaskImageAnnotation maskImageAnnotation : maskImageAnnotationRepository.findAll()) {
            if (!StringUtils.isNullOrEmpty(maskImageAnnotation.getRawDataFileId())) {
                usedIds.add(maskImageAnnotation.getRawDataFileId());
            }
            if (!StringUtils.isNullOrEmpty(maskImageAnnotation.getThumbnailDataFileId())) {
                usedIds.add(maskImageAnnotation.getThumbnailDataFileId());
            }
        }
        LOGGER.info("Looking for unused files... results");
        for (ResultItem resultItem : resultItemRepository.findAll()) {
            if (!StringUtils.isNullOrEmpty(resultItem.getRawDataFileId())) {
                usedIds.add(resultItem.getRawDataFileId());
            }
            if (!StringUtils.isNullOrEmpty(resultItem.getThumbnailDataFileId())) {
                usedIds.add(resultItem.getThumbnailDataFileId());
            }
            if (!StringUtils.isNullOrEmpty(resultItem.getVisualizationDataFileId())) {
                usedIds.add(resultItem.getVisualizationDataFileId());
            }
        }
        LOGGER.info("Looking for unused files... collecting present files");
        Set<String> allStoredFileIds = fileStorageService.findAllStoredFileIds();
        LOGGER.info("Looking for unused files... found " + allStoredFileIds.size() + " stored files");
        allStoredFileIds.removeAll(usedIds);
        LOGGER.info("Looking for unused files... found " + allStoredFileIds.size() + " stored files that are not used. Deleting now.");
        for (String fileId : allStoredFileIds) {
            fileStorageService.delete(fileId);
        }

    }
}
