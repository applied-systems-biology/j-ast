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

package org.hkijena.jast.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.BackendTaskService;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Controller
public class ProjectArchiveController {

    private final AccountConfig accountConfig;
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final BackendTaskService backendTaskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProjectArchiveController(AccountConfig accountConfig, ProjectRepository projectRepository, ImageRepository imageRepository, BackendTaskService backendTaskService, ProjectService projectService, UserService userService, FileStorageService fileStorageService) {
        this.accountConfig = accountConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.backendTaskService = backendTaskService;
        this.projectService = projectService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/api/project/{id}/import-project-archive")
    public void importProjectArchive(Authentication authentication, @PathVariable("id") long id, @RequestPart("file") MultipartFile zipFile) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(id);
        if (!projectService.canUploadImage(project, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // First load all images and metadata after extracting the ZIP
        Path tmpPath = PathUtils.createTempDirectory("j-ast-project-archive");
        try {
            zipFile.transferTo(tmpPath.resolve("archive.zip"));
            ArchiveUtils.decompressZipFile(tmpPath.resolve("archive.zip"), tmpPath, new ProgressInfo());

            // Read the metadata file
            Path metadataFile = tmpPath.resolve("metadata.json");
            Map<String, ImagePayload> metadata = JsonUtils.getObjectMapper().readerFor(new TypeReference<Map<String, ImagePayload>>() {
            }).readValue(metadataFile.toFile());

            for (Map.Entry<String, ImagePayload> entry : metadata.entrySet()) {
                ImagePayload archiveEntryMetadata = entry.getValue();
                Path rawFile = tmpPath.resolve(entry.getKey() + ".png");

                // Import the raw image
                BufferedImage rawImageData = ImageIO.read(rawFile.toFile());
                if (rawImageData == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
                }

                // Create raw image object and add it into the DB
                Image image = new Image();
                image.setOriginalFileName(StringUtils.nullToEmpty(archiveEntryMetadata.getFileName()));
                image.setGroupColumn(archiveEntryMetadata.getGroupColumn());
                image.setGroupRow(archiveEntryMetadata.getGroupRow());
                image.setAssayType(archiveEntryMetadata.getAssayType());
                image.setExperiment(archiveEntryMetadata.getExperiment());
                image.setSample(archiveEntryMetadata.getSample());
                image.setTimePoint(archiveEntryMetadata.getTimePoint());
                image.setMic(archiveEntryMetadata.getMic());
                image.setPixelSizeMillimeter(archiveEntryMetadata.getPixelSizeMillimeter());
                image.setMetadata(archiveEntryMetadata.getMetadata());
                image.setImageWidth(rawImageData.getWidth());
                image.setImageHeight(rawImageData.getHeight());
                image.setRawData(fileStorageService, ImageUtils.toPNGByteArray(rawImageData));
                image.rebuildThumbnail(fileStorageService, rawImageData);

                // Create the annotations
                for (String annotationType : List.of("strip-disk", "plate", "zoi-shape")) {
                    Path annotationFile = tmpPath.resolve(annotationType).resolve(entry.getKey() + ".png");

                    if (Files.isRegularFile(annotationFile)) {
                        // Import the raw image
                        BufferedImage annotationImageData = ImageIO.read(rawFile.toFile());
                        if (annotationImageData == null) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image data");
                        }

                        // Create a new annotation
                        MaskImageAnnotation maskImageAnnotation = new MaskImageAnnotation();
                        maskImageAnnotation.setImage(image);
                        maskImageAnnotation.setType(annotationType);
                        maskImageAnnotation.setRawData(fileStorageService, ImageUtils.toPNGByteArray(annotationImageData));
                        maskImageAnnotation.setThumbnailData(fileStorageService, ImageUtils.toPNGByteArrayThumbnail(annotationImageData));

                        image.addMaskImageAnnotation(maskImageAnnotation);
                    }

                }

                project.addImage(image);
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid project archive");
        } finally {
            PathUtils.deleteDirectoryRecursively(tmpPath, new ProgressInfo());
        }


        projectRepository.save(project);
    }
}
