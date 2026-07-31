# Chromium File Assembly Download Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dual-mode download system where Chromium browsers stream byte-split ZIP parts into one assembled file on disk via the File System Access API, with graceful fallback to the existing separate-ZIPs behavior on Firefox.

**Architecture:** New `DownloadBundleMode` enum (`SPLIT_ZIP` | `SEPARATE_ZIPS`) on the backend entity. `SPLIT_ZIP` mode generates one ZIP then splits raw bytes into part files. Frontend feature-detects `showSaveFilePicker`; Chromium requests `SPLIT_ZIP` and streams parts to one file handle; Firefox requests `SEPARATE_ZIPS` (current behavior unchanged).

**Tech Stack:** Java 21, Spring Boot, JPA/Hibernate, MariaDB (H2 for tests), Vue 3 + Quasar, TypeScript, File System Access API (Chromium-only), JUnit 5 + Mockito

## Global Constraints

- Config classes use `@ConfigurationProperties` + `@Validated` + `@Component` — never `@Value`
- MB unit uses decimal multiplier `1_000_000` (matches existing `2_048_000_000L = 2048 * 1_000_000`)
- Database target is MariaDB; tests run on H2. Entity `@Enumerated(EnumType.STRING)` for enum columns.
- `DownloadBundleMode` default is `SEPARATE_ZIPS` for backward compatibility
- Backend tests run from `backend/` directory: `cd backend && mvn test -Dnet.bytebuddy.experimental=true`
- Frontend has no test framework (`npm test` echoes "No test specified"). Frontend changes verified via `npm run build` (type-check + Vite build).
- Part naming in `SPLIT_ZIP` mode: `baseName.zip.part1`, `baseName.zip.part2`, etc.
- Part naming in `SEPARATE_ZIPS` mode: unchanged (`baseName.zip` or `baseName_part1.zip`, etc.)
- `outputFileName` is `baseName + ".zip"` (the final assembled file name for `SPLIT_ZIP` mode)
- JWT token passed via `?token=` query param for download URLs (existing pattern, unchanged)

---

### Task 1: Backend — `DownloadBundleMode` enum, entity fields, payload, and controller

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/model/DownloadBundleMode.java`
- Modify: `backend/src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java` (add `mode` + `outputFileName` fields)
- Modify: `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePayload.java` (add `mode` + `outputFileName`)
- Modify: `backend/src/main/java/org/hkijena/jast/controller/DownloadBundleController.java:41-49` (add `mode` query param)
- Modify: `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java:91-124` (`prepareDownload` accepts `mode`, stores it)
- Test: `backend/src/test/java/org/hkijena/jast/DownloadBundleModeTest.java`

**Interfaces:**
- Produces: `DownloadBundleMode` enum with values `SPLIT_ZIP` and `SEPARATE_ZIPS`. `DownloadBundle.getMode()` / `setMode()`. `DownloadBundle.getOutputFileName()` / `setOutputFileName()`. `DownloadBundlePayload.getMode()` / `getOutputFileName()`. `DownloadBundleService.prepareDownload(long resultId, String path, DownloadBundleMode mode, Authentication auth)`.
- Consumes: `DownloadConfig` from the configurable-splitting feature (already injected).

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/org/hkijena/jast/DownloadBundleModeTest.java`:

```java
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

package org.hkijena.jast;

import org.hkijena.jast.model.DownloadBundleMode;
import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.payloads.downloadbundle.DownloadBundlePayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "org.jobrunr.dashboard.enabled=false"
})
class DownloadBundleModeTest {

    @Autowired
    private org.hkijena.jast.repositories.DownloadBundleRepository downloadBundleRepository;

    @Test
    void entityStoresSplitZipMode() {
        DownloadBundle bundle = new DownloadBundle();
        bundle.setId(java.util.UUID.randomUUID().toString());
        bundle.setStatus(DownloadBundleStatus.Preparing);
        bundle.setPath("/");
        bundle.setCreatedAt(LocalDateTime.now());
        bundle.setExpiresAt(LocalDateTime.now().plusHours(1));
        bundle.setMode(DownloadBundleMode.SPLIT_ZIP);
        bundle.setOutputFileName("MyResult.zip");
        bundle.setTotalSize(1024);
        bundle.setPartCount(1);
        bundle.setProgressPercent(0);
        bundle.setProgressMessage("Starting");

        downloadBundleRepository.save(bundle);

        Optional<DownloadBundle> retrieved = downloadBundleRepository.findById(bundle.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(DownloadBundleMode.SPLIT_ZIP, retrieved.get().getMode());
        assertEquals("MyResult.zip", retrieved.get().getOutputFileName());

        downloadBundleRepository.delete(bundle);
    }

    @Test
    void entityDefaultsToSeparateZipsMode() {
        DownloadBundle bundle = new DownloadBundle();
        assertEquals(DownloadBundleMode.SEPARATE_ZIPS, bundle.getMode());
        assertNull(bundle.getOutputFileName());
    }

    @Test
    void payloadIncludesModeAndOutputFileName() {
        DownloadBundle bundle = new DownloadBundle();
        bundle.setId("test-id");
        bundle.setStatus(DownloadBundleStatus.Ready);
        bundle.setMode(DownloadBundleMode.SPLIT_ZIP);
        bundle.setOutputFileName("Result.zip");
        bundle.setProgressPercent(100);
        bundle.setProgressMessage("Ready");
        bundle.setTotalSize(5000);
        bundle.setPartCount(2);

        DownloadBundlePayload payload = new DownloadBundlePayload(bundle);
        assertEquals(DownloadBundleMode.SPLIT_ZIP, payload.getMode());
        assertEquals("Result.zip", payload.getOutputFileName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true -Dtest=DownloadBundleModeTest`
Expected: FAIL — compilation error (`DownloadBundleMode` does not exist, `getMode()`/`getOutputFileName()` undefined)

- [ ] **Step 3: Create the `DownloadBundleMode` enum**

Create `backend/src/main/java/org/hkijena/jast/model/DownloadBundleMode.java`:

```java
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

package org.hkijena.jast.model;

public enum DownloadBundleMode {
    SEPARATE_ZIPS,
    SPLIT_ZIP
}
```

- [ ] **Step 4: Add `mode` and `outputFileName` fields to `DownloadBundle` entity**

In `backend/src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java`, add the import after line 19 (`import org.hkijena.jast.model.DownloadBundleStatus;`):

```java
import org.hkijena.jast.model.DownloadBundleMode;
```

Add two new fields after the `errorMessage` field (after line 65):

```java
    @Enumerated(EnumType.STRING)
    private DownloadBundleMode mode = DownloadBundleMode.SEPARATE_ZIPS;

    @Column(name = "output_file_name")
    private String outputFileName;
```

Add getters/setters at the end of the class (before the closing `}`):

```java
    public DownloadBundleMode getMode() { return mode; }
    public void setMode(DownloadBundleMode mode) { this.mode = mode; }
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
```

- [ ] **Step 5: Add `mode` and `outputFileName` to `DownloadBundlePayload`**

In `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePayload.java`, add imports after line 17:

```java
import org.hkijena.jast.model.DownloadBundleMode;
```

Add two fields after the `errorMessage` field (after line 46):

```java
    @JsonProperty
    private DownloadBundleMode mode;

    @JsonProperty
    private String outputFileName;
```

In the constructor `DownloadBundlePayload(DownloadBundle bundle)` (after line 58, `this.errorMessage = ...`), add:

```java
        this.mode = bundle.getMode();
        this.outputFileName = bundle.getOutputFileName();
```

Add getters/setters at the end of the class (before the closing `}`):

```java
    public DownloadBundleMode getMode() { return mode; }
    public void setMode(DownloadBundleMode mode) { this.mode = mode; }
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
```

- [ ] **Step 6: Add `mode` query param to controller and service**

In `backend/src/main/java/org/hkijena/jast/controller/DownloadBundleController.java`, add import after line 17:

```java
import org.hkijena.jast.model.DownloadBundleMode;
```

Change the `prepareDownload` method signature (lines 41-49) to add a `mode` parameter:

```java
    @PostMapping("/api/result/{id}/prepare-download")
    public ResponseEntity<DownloadBundlePayload> prepareDownload(
            Authentication authentication,
            @PathVariable("id") long id,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path,
            @RequestParam(value = "mode", required = false, defaultValue = "SEPARATE_ZIPS") DownloadBundleMode mode
    ) {
        DownloadBundle bundle = downloadBundleService.prepareDownload(id, path, mode, authentication);
        return ResponseEntity.ok(new DownloadBundlePayload(bundle));
    }
```

In `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`, add import after line 19 (`import org.hkijena.jast.model.DownloadBundleStatus;`):

```java
import org.hkijena.jast.model.DownloadBundleMode;
```

Change the `prepareDownload` method signature (line 91) from:

```java
    public DownloadBundle prepareDownload(long resultId, String path, Authentication authentication) {
```

to:

```java
    public DownloadBundle prepareDownload(long resultId, String path, DownloadBundleMode mode, Authentication authentication) {
```

Inside `prepareDownload`, after line 106 (`bundle.setPath(path != null ? path : "/");`), add:

```java
        bundle.setMode(mode);
        if (mode == DownloadBundleMode.SPLIT_ZIP) {
            bundle.setOutputFileName(sanitizeFileName(result.getName()) + ".zip");
        }
```

- [ ] **Step 7: Run test to verify it passes**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true -Dtest=DownloadBundleModeTest`
Expected: PASS — all 3 tests green

- [ ] **Step 8: Run full test suite to verify no regressions**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true`
Expected: All tests pass

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/org/hkijena/jast/model/DownloadBundleMode.java backend/src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePayload.java backend/src/main/java/org/hkijena/jast/controller/DownloadBundleController.java backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java backend/src/test/java/org/hkijena/jast/DownloadBundleModeTest.java
git commit -m "Add DownloadBundleMode enum, entity fields, payload, and controller mode param"
```

---

### Task 2: Backend — `SPLIT_ZIP` generation in `DownloadBundleService`

**Files:**
- Modify: `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java` (add `splitZipIntoParts`, branch `generateBundle` on mode)
- Test: `backend/src/test/java/org/hkijena/jast/services/DownloadBundleSplitZipTest.java`

**Interfaces:**
- Consumes: `DownloadBundleMode` from Task 1, `DownloadConfig.getMaxPartSizeMb()` from configurable-splitting feature.
- Produces: `DownloadBundleService.generateBundle` now branches on `bundle.getMode()`. `SPLIT_ZIP` mode produces byte-split parts of a single ZIP. `SEPARATE_ZIPS` mode is unchanged.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/org/hkijena/jast/services/DownloadBundleSplitZipTest.java`:

```java
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

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.DownloadConfig;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DownloadBundleSplitZipTest {

    @TempDir
    Path tempDir;

    @Test
    void splitZipIntoPartsProducesCorrectByteSplits() throws Exception {
        DownloadConfig config = new DownloadConfig();
        config.setMaxPartSizeMb(1); // 1 MB = 1,000,000 bytes per part

        DownloadBundleService service = new DownloadBundleService(
                mock(DownloadBundleRepository.class),
                mock(ResultRepository.class),
                mock(FileStorageService.class),
                mock(AccountConfig.class),
                mock(UserService.class),
                mock(JobScheduler.class),
                config
        );

        // Create a fake ZIP of 2.5 MB to test splitting at 1 MB
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("data.bin"));
            byte[] data = new byte[2_500_000];
            zos.write(data);
            zos.closeEntry();
        }
        long zipSize = Files.size(zipFile);

        // Use the package-private splitZipIntoParts method
        // FileStorageService is mocked, so we need to test via a real storage dir
        // The method writes to storageDir.resolve(UUID) — we pass tempDir as storageDir
        List<DownloadBundlePart> parts = service.splitZipIntoParts(zipFile, "MyResult", tempDir, zipSize);

        // 2.5 MB / 1 MB = 3 parts (1 MB, 1 MB, 0.5 MB)
        assertEquals(3, parts.size());
        assertEquals("MyResult.zip.part1", parts.get(0).getFileName());
        assertEquals("MyResult.zip.part2", parts.get(1).getFileName());
        assertEquals("MyResult.zip.part3", parts.get(2).getFileName());

        // Verify total size equals original ZIP size
        long totalSize = parts.stream().mapToLong(DownloadBundlePart::getSize).sum();
        assertEquals(zipSize, totalSize);

        // Verify each part (except possibly the last) is exactly maxPartSize
        assertEquals(1_000_000L, parts.get(0).getSize());
        assertEquals(1_000_000L, parts.get(1).getSize());
        assertTrue(parts.get(2).getSize() <= 1_000_000L);

        // Verify that concatenating parts reproduces the original ZIP
        ByteArrayOutputStream reassembled = new ByteArrayOutputStream();
        for (DownloadBundlePart part : parts) {
            Path partFile = tempDir.resolve(part.getFileId());
            reassembled.write(Files.readAllBytes(partFile));
        }
        assertArrayEquals(Files.readAllBytes(zipFile), reassembled.toByteArray());
    }

    @Test
    void splitZipIntoPartsSinglePartWhenZipSmallerThanLimit() throws Exception {
        DownloadConfig config = new DownloadConfig();
        config.setMaxPartSizeMb(10); // 10 MB limit

        DownloadBundleService service = new DownloadBundleService(
                mock(DownloadBundleRepository.class),
                mock(ResultRepository.class),
                mock(FileStorageService.class),
                mock(AccountConfig.class),
                mock(UserService.class),
                mock(JobScheduler.class),
                config
        );

        Path zipFile = tempDir.resolve("small.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("data.bin"));
            zos.write(new byte[1000]);
            zos.closeEntry();
        }
        long zipSize = Files.size(zipFile);

        List<DownloadBundlePart> parts = service.splitZipIntoParts(zipFile, "Small", tempDir, zipSize);

        assertEquals(1, parts.size());
        assertEquals("Small.zip.part1", parts.get(0).getFileName());
        assertEquals(zipSize, parts.get(0).getSize());

        // Verify file content matches
        Path partFile = tempDir.resolve(parts.get(0).getFileId());
        assertArrayEquals(Files.readAllBytes(zipFile), Files.readAllBytes(partFile));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true -Dtest=DownloadBundleSplitZipTest`
Expected: FAIL — compilation error (`splitZipIntoParts` method does not exist)

- [ ] **Step 3: Add `splitZipIntoParts` method to `DownloadBundleService`**

In `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`, add this method after the existing `splitIntoParts` method (after line 331):

```java
    List<DownloadBundlePart> splitZipIntoParts(Path zipFile, String baseName, Path storageDir, long zipSize) throws IOException {
        List<DownloadBundlePart> parts = new ArrayList<>();
        long maxPartSize = downloadConfig.getMaxPartSizeMb() * 1_000_000L;
        int partNumber = 1;
        long remaining = zipSize;

        try (var inputStream = Files.newInputStream(zipFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (remaining > 0) {
                String fileId = UUID.randomUUID().toString();
                Path partPath = storageDir.resolve(fileId);
                long partSize = Math.min(maxPartSize, remaining);

                try (var outputStream = Files.newOutputStream(partPath)) {
                    long written = 0;
                    while (written < partSize) {
                        int toRead = (int) Math.min(buffer.length, partSize - written);
                        int read = inputStream.read(buffer, 0, toRead);
                        if (read <= 0) break;
                        outputStream.write(buffer, 0, read);
                        written += read;
                    }
                }

                DownloadBundlePart part = new DownloadBundlePart();
                part.setFileId(fileId);
                part.setFileName(baseName + ".zip.part" + partNumber);
                part.setSize(Files.size(partPath));
                parts.add(part);

                remaining -= partSize;
                partNumber++;
            }
        }

        return parts;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true -Dtest=DownloadBundleSplitZipTest`
Expected: PASS — both tests green

- [ ] **Step 5: Add `SPLIT_ZIP` branch to `generateBundle`**

In `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`, inside the `generateBundle` method, after the items are sorted (line 196) and before the current `splitIntoParts` call (line 198), add a branch:

Replace lines 198-268 (from `List<List<ResultItem>> parts = splitIntoParts(matchingItems);` through the end of the part-generation loop) with:

```java
            if (bundle.getMode() == DownloadBundleMode.SPLIT_ZIP) {
                // Generate one ZIP, then split raw bytes into parts
                Path tempZip = Paths.get(fileStorageService.getStorageLocation())
                        .resolve(UUID.randomUUID().toString() + ".tmp");
                String baseName = sanitizeFileName(result.getName());
                bundle.setOutputFileName(baseName + ".zip");

                try {
                    updateProgress(bundleId, 0, "Creating ZIP");

                    // Write all items into one ZIP
                    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZip.toFile()))) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        for (int i = 0; i < matchingItems.size(); i++) {
                            ResultItem item = matchingItems.get(i);
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

                            int percent = (int) ((i * 50L) / matchingItems.size());
                            updateProgress(bundleId, percent, "Creating ZIP (" + (i + 1) + "/" + matchingItems.size() + " files)");
                        }
                    }

                    long zipSize = Files.size(tempZip);
                    updateProgress(bundleId, 50, "Splitting into parts (" + formatFileSize(zipSize) + ")");

                    // Split raw bytes into part files
                    List<DownloadBundlePart> completedParts = splitZipIntoParts(tempZip, baseName,
                            Paths.get(fileStorageService.getStorageLocation()), zipSize);

                    bundle = downloadBundleRepository.findById(bundleId).orElseThrow();
                    bundle.setParts(completedParts);
                    bundle.setPartCount(completedParts.size());
                    bundle.setStatus(DownloadBundleStatus.Ready);
                    bundle.setProgressPercent(100);
                    bundle.setProgressMessage("Ready");
                    downloadBundleRepository.save(bundle);

                    // Persist completed parts incrementally for crash recovery
                    DownloadBundle progressBundle = downloadBundleRepository.findById(bundleId).orElse(null);
                    if (progressBundle != null) {
                        progressBundle.setParts(new ArrayList<>(completedParts));
                        downloadBundleRepository.save(progressBundle);
                    }

                } finally {
                    Files.deleteIfExists(tempZip);
                }

            } else {
                // SEPARATE_ZIPS mode: existing behavior
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
            }
```

Note: The `completedParts` variable declaration (line 170) and the `catch` block (lines 279-293) remain unchanged — they work for both modes since `completedParts` is populated in the `else` branch and the `catch` handles failures for both.

Important: the `formatFileSize` method call in the SPLIT_ZIP progress message needs a helper. Add this private method after `sanitizeFileName`:

```java
    private String formatFileSize(long bytes) {
        if (bytes < 1_000_000) return (bytes / 1000) + " KB";
        if (bytes < 1_000_000_000) return (bytes / 1_000_000) + " MB";
        return (bytes / 1_000_000_000) + " GB";
    }
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true -Dtest=DownloadBundleSplitZipTest`
Expected: PASS — both tests still green (the `splitZipIntoParts` method is unchanged)

- [ ] **Step 7: Run full test suite to verify no regressions**

Run: `cd backend && mvn test -Dnet.bytebuddy.experimental=true`
Expected: All tests pass

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java backend/src/test/java/org/hkijena/jast/services/DownloadBundleSplitZipTest.java
git commit -m "Add SPLIT_ZIP mode with byte-split generation in DownloadBundleService"
```

---

### Task 3: Frontend — Types, feature detection, and streaming download flow

**Files:**
- Modify: `frontend/src/types/downloadBundle.ts` (add `DownloadBundleMode` enum, `mode` + `outputFileName` fields)
- Modify: `frontend/src/layouts/ResultsViewLayout.vue:412-527` (feature detection, mode param, streaming download, Firefox notice)

**Interfaces:**
- Consumes: Backend `prepareDownload?mode=SPLIT_ZIP` endpoint from Task 1, `DownloadBundlePayload` with `mode` + `outputFileName` from Task 1.
- Produces: `supportsFileSystemAccess()` function, `streamPartsToFile()` async function. Updated `downloadZip` and `pollDownloadBundle` functions.

**Verification:** Frontend has no test framework. Verify with `cd frontend && npm run build` (type-check + Vite build must succeed).

- [ ] **Step 1: Update frontend types**

In `frontend/src/types/downloadBundle.ts`, add the enum after `DownloadBundleStatus` (after line 19):

```typescript
export enum DownloadBundleMode {
  SEPARATE_ZIPS = "SEPARATE_ZIPS",
  SPLIT_ZIP = "SPLIT_ZIP",
}
```

Add `mode` and `outputFileName` fields to `DownloadBundlePayload` (after line 34, `errorMessage`):

```typescript
  mode: DownloadBundleMode = DownloadBundleMode.SEPARATE_ZIPS;
  outputFileName: string = "";
```

- [ ] **Step 2: Add feature detection helper**

In `frontend/src/layouts/ResultsViewLayout.vue`, add this function after the imports (after line 141, before `const filesViewPagination`):

```typescript
function supportsFileSystemAccess(): boolean {
  return typeof window !== 'undefined' && 'showSaveFilePicker' in window;
}
```

- [ ] **Step 3: Modify `downloadZip` to pass mode and show Firefox notice**

In `frontend/src/layouts/ResultsViewLayout.vue`, replace the `downloadZip` function (lines 412-445) with:

```typescript
function downloadZip(path: string) {
  const toDownload: Array<ResultItemPayload> = []
  for (const item of resultPayload.value.items) {
    const displayPath = "/" + item.path;
    if (displayPath.startsWith(path)) {
      toDownload.push(item)
    }
  }
  if (toDownload.length == 0) {
    sendFailureNotification("Nothing to download.")
    return
  }
  let downloadSizeBytes = 0
  for (const resultItem of toDownload) {
    downloadSizeBytes += resultItem.size
  }

  const useSplitZip = supportsFileSystemAccess();
  const mode = useSplitZip ? DownloadBundleMode.SPLIT_ZIP : DownloadBundleMode.SEPARATE_ZIPS;

  let message = `You are about to download ${toDownload.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?`;
  if (!useSplitZip) {
    message += `<br/><br/><small>Tip: For large downloads, Chrome, Edge, or the desktop app provide a better experience with automatic file assembly and save-location selection.</small>`;
  }

  $q.dialog({
    title: 'Download results',
    message: message,
    html: true,
    cancel: true,
    persistent: true
  }).onOk(() => {
    const authStore = useAuthStore();
    api.post(`/result/${resultId}/prepare-download`, null, {
      params: { path: path, mode: mode }
    }).then((response) => {
      const bundle: DownloadBundlePayload = response.data;
      pollDownloadBundle(bundle, authStore.accessToken);
    }).catch(() => {
      sendFailureNotification("Failed to start download preparation.");
    });
  })
}
```

Also add `DownloadBundleMode` to the import on line 136:

```typescript
import {DownloadBundlePayload, DownloadBundleStatus, DownloadBundleMode} from "src/types/downloadBundle";
```

- [ ] **Step 4: Add `streamPartsToFile` function**

In `frontend/src/layouts/ResultsViewLayout.vue`, add this function after `triggerDownload` (after line 527):

```typescript
async function streamPartsToFile(
  bundle: DownloadBundlePayload,
  accessToken: string,
  onProgress: (part: number, totalParts: number, partPercent: number, overallPercent: number) => void,
  shouldCancel: Ref<boolean>
): Promise<void> {
  const fileHandle = await (window as any).showSaveFilePicker({
    suggestedName: bundle.outputFileName || 'download.zip'
  });
  const writable = await fileHandle.createWritable();
  const totalParts = bundle.parts.length;
  const totalSize = bundle.parts.reduce((sum, p) => sum + p.size, 0);
  let receivedTotal = 0;

  try {
    for (let i = 0; i < totalParts; i++) {
      if (shouldCancel.value) {
        await writable.abort();
        return;
      }

      const response = await fetch(
        `${apiBase}/download-bundle/${bundle.id}/part/${i}?token=${encodeURIComponent(accessToken)}`
      );
      const reader = response.body!.getReader();
      let received = 0;
      const partSize = bundle.parts[i].size;

      while (true) {
        if (shouldCancel.value) {
          await reader.cancel();
          await writable.abort();
          return;
        }
        const { done, value } = await reader.read();
        if (done) break;
        await writable.write(value);
        received += value.byteLength;
        receivedTotal += value.byteLength;
        onProgress(
          i + 1,
          totalParts,
          partSize > 0 ? Math.round((received / partSize) * 100) : 100,
          totalSize > 0 ? Math.round((receivedTotal / totalSize) * 100) : 100
        );
      }
    }
    await writable.close();
  } catch (e) {
    await writable.abort();
    throw e;
  }
}
```

- [ ] **Step 5: Modify `pollDownloadBundle` to branch on mode**

In `frontend/src/layouts/ResultsViewLayout.vue`, replace the `Ready` branch inside `pollDownloadBundle` (lines 479-501) with:

```typescript
      } else if (updated.status === DownloadBundleStatus.Ready) {
        clearInterval(pollInterval);

        if (updated.mode === DownloadBundleMode.SPLIT_ZIP && supportsFileSystemAccess()) {
          // Chromium: stream parts into one file via File System Access API
          progressDialog.update({
            title: 'Downloading ...',
            message: 'Starting download ...',
            progress: { spinner: QSpinnerHourglass },
            ok: false,
            cancel: true,
          });

          streamPartsToFile(updated, accessToken, (part, totalParts, partPercent, overallPercent) => {
            progressDialog.update({
              message: `Part ${part}/${totalParts} — ${partPercent}%<br/>Overall: ${overallPercent}%`,
              html: true,
            });
          }, shouldCancel).then(() => {
            if (!shouldCancel.value) {
              progressDialog.update({
                title: 'Download complete!',
                message: 'Your file has been saved.',
                progress: false,
                ok: 'Done',
                cancel: false,
              });
            }
          }).catch((e) => {
            progressDialog.hide();
            if (e instanceof DOMException && e.name === 'AbortError') {
              // User cancelled the save-file dialog — not an error
              return;
            }
            sendFailureNotification("Download failed: " + (e.message || "Unknown error"));
          });

        } else {
          // SEPARATE_ZIPS or Firefox: existing behavior
          progressDialog.update({
            title: 'Download ready!',
            message: '',
            progress: false,
            ok: 'Done',
            cancel: false,
          });

          if (updated.parts.length === 1) {
            triggerDownload(bundle.id, 0, accessToken);
          } else {
            let message = `Download ready! (${updated.parts.length} parts)<br/><br/>`;
            for (let i = 0; i < updated.parts.length; i++) {
              const part = updated.parts[i];
              message += `<a href="${apiBase}/download-bundle/${bundle.id}/part/${i}?token=${encodeURIComponent(accessToken)}" download="${part.fileName}">Download ${part.fileName} (${formatFileSize(part.size)})</a><br/>`;
            }
            progressDialog.update({
              message: message,
              html: true,
            });
          }
        }
```

- [ ] **Step 6: Verify frontend build succeeds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no type errors

- [ ] **Step 7: Commit**

```bash
git add frontend/src/types/downloadBundle.ts frontend/src/layouts/ResultsViewLayout.vue
git commit -m "Add Chromium File System Access API streaming download with Firefox fallback"
```
