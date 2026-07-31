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
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.DownloadConfig;
import org.hkijena.jast.model.DownloadBundleMode;
import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;
import org.hkijena.jast.model.entities.Result;
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadBundleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadBundleService.class);
    private static final int BUFFER_SIZE = 8192;

    private final DownloadBundleRepository downloadBundleRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final AccountConfig accountConfig;
    private final DownloadConfig downloadConfig;
    private final UserService userService;
    private final JobScheduler jobScheduler;

    @Autowired
    public DownloadBundleService(DownloadBundleRepository downloadBundleRepository,
                                  ResultRepository resultRepository,
                                  FileStorageService fileStorageService,
                                  AccountConfig accountConfig,
                                  UserService userService,
                                  JobScheduler jobScheduler,
                                  DownloadConfig downloadConfig) {
        this.downloadBundleRepository = downloadBundleRepository;
        this.resultRepository = resultRepository;
        this.fileStorageService = fileStorageService;
        this.accountConfig = accountConfig;
        this.userService = userService;
        this.jobScheduler = jobScheduler;
        this.downloadConfig = downloadConfig;
    }

    @PostConstruct
    public void resetStuckBundles() {
        List<DownloadBundle> stuck = downloadBundleRepository.findByStatus(DownloadBundleStatus.Preparing);
        for (DownloadBundle bundle : stuck) {
            LOGGER.info("Resetting stuck download bundle {} to Failed", bundle.getId());
            bundle.setStatus(DownloadBundleStatus.Failed);
            bundle.setErrorMessage("Server restarted during generation");
            downloadBundleRepository.save(bundle);
        }
    }

    public DownloadBundle prepareDownload(long resultId, String path, DownloadBundleMode mode, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(resultId);
        if (result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if (!result.getProject().canAccess(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        DownloadBundle bundle = new DownloadBundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setResult(result);
        bundle.setStatus(DownloadBundleStatus.Preparing);
        bundle.setPath(path != null ? path : "/");
        bundle.setMode(mode);
        if (mode == DownloadBundleMode.SPLIT_ZIP) {
            bundle.setOutputFileName(sanitizeFileName(result.getName()) + ".zip");
        }
        bundle.setCreatedAt(LocalDateTime.now());
        bundle.setExpiresAt(LocalDateTime.now().plusHours(downloadConfig.getExpiryHours()));
        bundle.setProgressPercent(0);
        bundle.setProgressMessage("Preparing...");

        long totalSize = 0;
        for (ResultItem item : result.getResultItems()) {
            totalSize += item.getRawDataFileSize();
        }
        bundle.setTotalSize(totalSize);

        downloadBundleRepository.save(bundle);

        final String bundleId = bundle.getId();
        jobScheduler.enqueue(() -> generateBundle(bundleId));

        return bundle;
    }

    public DownloadBundle getBundle(String bundleId, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<DownloadBundle> bundle = downloadBundleRepository.findById(bundleId);
        if (bundle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!bundle.get().getResult().getProject().canAccess(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return bundle.get();
    }

    public Path getPartFilePath(String bundleId, int partIndex, Authentication authentication) {
        DownloadBundle bundle = getBundle(bundleId, authentication);
        if (bundle.getStatus() != DownloadBundleStatus.Ready) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bundle is not ready");
        }
        if (partIndex < 0 || partIndex >= bundle.getParts().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid part index");
        }
        DownloadBundlePart part = bundle.getParts().get(partIndex);
        Path filePath = fileStorageService.getFilePath(part.getFileId());
        if (filePath == null || !Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Part file not found");
        }
        return filePath;
    }

    public String getPartFileName(String bundleId, int partIndex, Authentication authentication) {
        DownloadBundle bundle = getBundle(bundleId, authentication);
        if (partIndex < 0 || partIndex >= bundle.getParts().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid part index");
        }
        return bundle.getParts().get(partIndex).getFileName();
    }

    public void generateBundle(String bundleId) {
        Optional<DownloadBundle> bundle_ = downloadBundleRepository.findByIdWithData(bundleId);
        if (bundle_.isEmpty()) {
            LOGGER.error("Download bundle {} not found", bundleId);
            return;
        }
        DownloadBundle bundle = bundle_.get();
        Result result = bundle.getResult();
        List<DownloadBundlePart> completedParts = new ArrayList<>();
        Path currentZipPath = null;
        String currentFileId = null;

        try {
            String normalizedPath = bundle.getPath();
            while (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }

            List<ResultItem> matchingItems = new ArrayList<>();
            for (ResultItem item : result.getResultItems()) {
                String itemPath = item.getPath() == null ? "" : item.getPath();
                String displayPath = "/" + itemPath;
                String filterPath = bundle.getPath();
                if (!filterPath.startsWith("/")) {
                    filterPath = "/";
                }
                if (!filterPath.endsWith("/")) {
                    filterPath = filterPath + "/";
                }
                if (displayPath.startsWith(filterPath) || displayPath.equals(filterPath.substring(0, filterPath.length() - 1))) {
                    matchingItems.add(item);
                }
            }

            matchingItems.sort(Comparator.comparing(item -> (item.getPath() == null ? "" : item.getPath()) + "/" + (item.getName() == null ? "" : item.getName())));

            List<List<ResultItem>> parts = splitIntoParts(matchingItems);
            bundle.setPartCount(parts.size());
            updateProgress(bundleId, 0, "Creating " + parts.size() + " part(s)");

            String baseName = sanitizeFileName(result.getName());
            int totalItems = matchingItems.size();
            int processedItems = 0;
            Path storageDir = Paths.get(fileStorageService.getStorageLocation());

            for (int partIdx = 0; partIdx < parts.size(); partIdx++) {
                String fileId = UUID.randomUUID().toString();
                Path zipPath = storageDir.resolve(fileId);
                currentFileId = fileId;
                currentZipPath = zipPath;
                String partName;
                if (parts.size() == 1) {
                    partName = baseName + ".zip";
                } else {
                    partName = baseName + "_part" + (partIdx + 1) + ".zip";
                }

                try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    for (ResultItem item : parts.get(partIdx)) {
                        String rawDataFileId = item.getRawDataFileId();
                        if (rawDataFileId == null || rawDataFileId.isEmpty()) {
                            continue;
                        }
                        Path filePath = fileStorageService.getFilePath(rawDataFileId);
                        if (filePath == null || !Files.exists(filePath)) {
                            continue;
                        }

                        String entryName = buildEntryName(item, normalizedPath);
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zipOut.putNextEntry(zipEntry);
                        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }
                        }
                        zipOut.closeEntry();

                        processedItems++;
                        int percent = (int) ((processedItems * 100L) / totalItems);
                        updateProgress(bundleId, percent,
                                "Creating part " + (partIdx + 1) + " of " + parts.size() +
                                        " (" + processedItems + "/" + totalItems + " files)");
                    }
                }

                long zipSize = Files.size(zipPath);
                DownloadBundlePart part = new DownloadBundlePart();
                part.setFileId(fileId);
                part.setFileName(partName);
                part.setSize(zipSize);
                completedParts.add(part);

                // Persist completed parts incrementally for crash recovery
                DownloadBundle progressBundle = downloadBundleRepository.findById(bundleId).orElse(null);
                if (progressBundle != null) {
                    progressBundle.setParts(new ArrayList<>(completedParts));
                    downloadBundleRepository.save(progressBundle);
                }

                currentZipPath = null;
                currentFileId = null;

                updateProgress(bundleId, (int) ((partIdx + 1) * 100L / parts.size()),
                        "Completed part " + (partIdx + 1) + " of " + parts.size());
            }

            bundle = downloadBundleRepository.findById(bundleId).orElseThrow();
            bundle.setParts(completedParts);
            bundle.setPartCount(parts.size());
            bundle.setStatus(DownloadBundleStatus.Ready);
            bundle.setProgressPercent(100);
            bundle.setProgressMessage("Ready");
            downloadBundleRepository.save(bundle);

        } catch (Exception e) {
            LOGGER.error("Failed to generate download bundle {}", bundleId, e);
            if (currentZipPath != null && Files.exists(currentZipPath)) {
                try {
                    Files.deleteIfExists(currentZipPath);
                } catch (IOException ioEx) {
                    LOGGER.warn("Could not delete partial ZIP file: {}", currentZipPath, ioEx);
                }
            }
            bundle = downloadBundleRepository.findById(bundleId).orElseThrow();
            bundle.setStatus(DownloadBundleStatus.Failed);
            bundle.setParts(completedParts);
            bundle.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
            downloadBundleRepository.save(bundle);
        }
    }

    private void updateProgress(String bundleId, int percent, String message) {
        Optional<DownloadBundle> bundle_ = downloadBundleRepository.findById(bundleId);
        if (bundle_.isPresent()) {
            DownloadBundle bundle = bundle_.get();
            bundle.setProgressPercent(percent);
            bundle.setProgressMessage(message);
            downloadBundleRepository.save(bundle);
        }
    }

    List<List<ResultItem>> splitIntoParts(List<ResultItem> items) {
        List<List<ResultItem>> parts = new ArrayList<>();
        List<ResultItem> currentPart = new ArrayList<>();
        long currentSize = 0;

        for (ResultItem item : items) {
            long itemSize = item.getRawDataFileSize();
            if (currentSize + itemSize > downloadConfig.getMaxPartSizeMb() * 1_000_000L && !currentPart.isEmpty()) {
                parts.add(currentPart);
                currentPart = new ArrayList<>();
                currentSize = 0;
            }
            currentPart.add(item);
            currentSize += itemSize;
        }

        if (!currentPart.isEmpty()) {
            parts.add(currentPart);
        }

        if (parts.isEmpty()) {
            parts.add(new ArrayList<>());
        }

        return parts;
    }

    private String buildEntryName(ResultItem item, String normalizedPath) {
        String itemPath = item.getPath() == null ? "" : item.getPath();
        String itemName = item.getName() == null ? "unnamed" : item.getName();
        String entryName = itemPath + "/" + itemName;
        if (!normalizedPath.isEmpty() && entryName.startsWith(normalizedPath)) {
            entryName = entryName.substring(normalizedPath.length());
        }
        while (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
        if (entryName.isEmpty()) {
            entryName = itemName;
        }
        return entryName;
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isEmpty()) return "result";
        String clean = input.replaceAll("[<>:\"/\\\\|?*\\x00]", "_").trim();
        if (clean.isEmpty()) return "result";
        if (clean.length() > 255) {
            clean = clean.substring(0, 255);
        }
        return clean;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cleanupExpired() {
        List<DownloadBundle> expired = downloadBundleRepository.findByExpiresAtBefore(LocalDateTime.now());
        for (DownloadBundle bundle : expired) {
            if (bundle.getStatus() == DownloadBundleStatus.Expired) {
                continue;
            }
            LOGGER.info("Cleaning up expired download bundle {}", bundle.getId());
            for (DownloadBundlePart part : bundle.getParts()) {
                fileStorageService.delete(part.getFileId());
            }
            bundle.setStatus(DownloadBundleStatus.Expired);
            downloadBundleRepository.save(bundle);
        }
    }
}
