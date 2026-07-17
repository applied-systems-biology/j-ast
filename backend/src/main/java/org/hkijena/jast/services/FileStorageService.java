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

import jakarta.transaction.Transactional;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);
    private final Path storageLocation;

    @Autowired
    public FileStorageService(RuntimeConfig runtimeConfig) {
        // Use the directory from RuntimeConfig
        this.storageLocation = Paths.get(runtimeConfig.getDataDirectory()).toAbsolutePath();
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public Set<String> findAllStoredFileIds() {
        Set<String> result = new HashSet<>();
        try (var stream = Files.list(storageLocation)) {
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.startsWith(".") && fileName.contains("-")) {
                    result.add(fileName);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Could not list stored files", e);
        }
        return result;
    }

    /**
     * Stores a byte array as a file with a UUID as the filename.
     *
     * @param data The byte array to store.
     * @return The UUID of the stored file.
     */
    public String store(byte[] data) {
        String fileId = UUID.randomUUID().toString();

        try {
            Path targetLocation = storageLocation.resolve(fileId);
            Files.write(targetLocation, data);
            return fileId;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    /**
     * Loads a file's content as a byte array.
     *
     * @param fileId The UUID of the file to load.
     * @return The byte array content of the file.
     */
    public byte[] load(String fileId) {
        try {
            Path filePath = storageLocation.resolve(fileId).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not load file", e);
        }
    }

    /**
     * Deletes a file from storage.
     *
     * @param fileId The UUID of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     */
    public boolean delete(String fileId) {
        if (StringUtils.isNullOrEmpty(fileId)) {
            return false;
        }
        try {
            Path filePath = storageLocation.resolve(fileId).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            LOGGER.error("Could not delete file", e);
            return false;
        }
    }

    /**
     * Gets the storage directory path as a string.
     *
     * @return The storage directory path.
     */
    public String getStorageLocation() {
        return storageLocation.toString();
    }

    /**
     * Schedules a file to be deleted only after a successful transaction.
     *
     * @param fileId The ID of the file to delete.
     */
    @Transactional
    public void deleteLater(String fileId) {
        if (StringUtils.isNullOrEmpty(fileId)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete(fileId); // Perform deletion only after the transaction is committed
                }
            });
        } else {
            throw new IllegalStateException("No active transaction. deleteLater requires an active transaction.");
        }
    }

    public BufferedImage loadPngOrNull(String fileId) {
        if (StringUtils.isNullOrEmpty(fileId)) {
            return null;
        }
        Path filePath = storageLocation.resolve(fileId).normalize();
        if (!Files.exists(filePath)) {
            return null;
        }
        try {
            return ImageIO.read(filePath.toFile());
        } catch (Exception e) {
            LOGGER.error("Could not load file", e);
        }
        return null;
    }

    public String store(BufferedImage image) {
        String fileId = UUID.randomUUID().toString();

        try {
            ImageIO.write(image, "PNG", storageLocation.resolve(fileId).toFile());
            return fileId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String loadStringOrNull(String fileId) {
        if (StringUtils.isNullOrEmpty(fileId)) {
            return null;
        }
        Path filePath = storageLocation.resolve(fileId).normalize();
        if (!Files.exists(filePath)) {
            return null;
        }
        try {
            return Files.readString(filePath);
        } catch (Exception e) {
            LOGGER.error("Could not load file", e);
        }
        return null;
    }

    public byte[] loadOrNull(String fileId) {
        try {
            Path filePath = storageLocation.resolve(fileId).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            return null;
        }
    }

    public String store(String text) {
        return store(text.getBytes(StandardCharsets.UTF_8));
    }
}

