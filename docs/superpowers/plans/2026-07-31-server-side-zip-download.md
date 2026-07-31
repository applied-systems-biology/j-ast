# Server-Side ZIP Download Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace in-browser JSZip generation (which fails on 6GB+ results) with a server-side background ZIP generation system that streams files from disk, splits into 2GB parts, and serves with Content-Length + Range support for reliable, resumable downloads.

**Architecture:** A new `DownloadBundle` JPA entity tracks ZIP generation jobs enqueued via JobRunr. The frontend polls a status endpoint for progress, then downloads ready ZIP parts via browser-managed anchor-tag downloads (with JWT token in query parameter). A scheduled task cleans up expired bundles (1-hour TTL).

**Tech Stack:** Java 21, Spring Boot, JobRunr, H2/Hibernate, Vue 3 + Quasar + TypeScript

## Global Constraints

- Max ZIP part size: 1.9GB (2_048_000_000 bytes) to account for ZIP overhead under the 2GB limit
- Bundle expiry: 1 hour from creation
- Cleanup schedule: every 5 minutes
- Polling interval: 2 seconds
- Auth for browser downloads: JWT token passed as `?token=` query parameter (JwtTokenFilter already modified to support this)
- H2 database with `ddl-auto: update` — Hibernate auto-creates tables, no migration scripts needed
- FileStorageService stores files as UUID-named blobs in a flat directory; `getFilePath(fileId)` returns the Path (already implemented)
- JSON columns use `io.hypersistence.utils.hibernate.type.json.JsonType` with `@Type(JsonType.class)` and `columnDefinition = "JSON"`
- Payloads use `@JsonProperty` annotations and plain getter/setter pattern (no Lombok)
- `@EnableScheduling` is already present on `JASTWebApplicationServer`
- `JobScheduler` from JobRunr is injected via constructor (same pattern as `BackendTaskService`)
- File header copyright block is required on all new Java files (see existing files for template)

---

### Task 1: Backend Data Model + Repository

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/model/DownloadBundleStatus.java`
- Create: `backend/src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java`
- Create: `backend/src/main/java/org/hkijena/jast/repositories/DownloadBundleRepository.java`
- Test: `backend/src/test/java/org/hkijena/jast/DownloadBundleEntityTest.java`

**Interfaces:**
- Produces: `DownloadBundle` entity (fields: id, result, status, path, createdAt, expiresAt, totalSize, partCount, parts, progressPercent, progressMessage, errorMessage)
- Produces: `DownloadBundleStatus` enum (Preparing, Ready, Failed, Expired)
- Produces: `DownloadBundleRepository` with `findById(String)`, `findByExpiresAtBefore(LocalDateTime)`, `findByStatus(DownloadBundleStatus)`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/org/hkijena/jast/DownloadBundleEntityTest.java`:

```java
package org.hkijena.jast;

import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.services.FileStorageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DownloadBundleEntityTest {

    @Autowired
    private DownloadBundleRepository downloadBundleRepository;

    @Test
    void downloadBundleCanBePersistedAndRetrieved() {
        DownloadBundle bundle = new DownloadBundle();
        bundle.setId(java.util.UUID.randomUUID().toString());
        bundle.setStatus(DownloadBundleStatus.Preparing);
        bundle.setPath("/");
        bundle.setCreatedAt(LocalDateTime.now());
        bundle.setExpiresAt(LocalDateTime.now().plusHours(1));
        bundle.setTotalSize(1024);
        bundle.setPartCount(1);
        bundle.setProgressPercent(0);
        bundle.setProgressMessage("Starting");

        List<DownloadBundlePart> parts = new ArrayList<>();
        DownloadBundlePart part = new DownloadBundlePart();
        part.setFileId("test-file-id");
        part.setFileName("result.zip");
        part.setSize(1024);
        parts.add(part);
        bundle.setParts(parts);

        downloadBundleRepository.save(bundle);

        Optional<DownloadBundle> retrieved = downloadBundleRepository.findById(bundle.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(DownloadBundleStatus.Preparing, retrieved.get().getStatus());
        assertEquals(1, retrieved.get().getParts().size());
        assertEquals("result.zip", retrieved.get().getParts().get(0).getFileName());
        assertEquals(1024, retrieved.get().getParts().get(0).getSize());

        downloadBundleRepository.delete(bundle);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=DownloadBundleEntityTest -v`
Expected: FAIL with compilation error (DownloadBundle, DownloadBundleStatus, DownloadBundleRepository do not exist)

- [ ] **Step 3: Create DownloadBundleStatus enum**

Create `backend/src/main/java/org/hkijena/jast/model/DownloadBundleStatus.java`:

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

public enum DownloadBundleStatus {
    Preparing,
    Ready,
    Failed,
    Expired
}
```

- [ ] **Step 4: Create DownloadBundle entity**

Create `backend/src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java`:

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

package org.hkijena.jast.model.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.DownloadBundleStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "download_bundles")
public class DownloadBundle {

    @Id
    @Column(name = "id", columnDefinition = "TEXT")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Result result;

    @Enumerated(EnumType.STRING)
    private DownloadBundleStatus status = DownloadBundleStatus.Preparing;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path = "/";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "total_size")
    private long totalSize;

    @Column(name = "part_count")
    private int partCount;

    @Column(name = "parts", columnDefinition = "JSON")
    @Type(JsonType.class)
    private List<DownloadBundlePart> parts = new ArrayList<>();

    @Column(name = "progress_percent")
    private int progressPercent;

    @Column(name = "progress_message", columnDefinition = "TEXT")
    private String progressMessage = "";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static class DownloadBundlePart {
        private String fileId;
        private String fileName;
        private long size;

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public DownloadBundleStatus getStatus() { return status; }
    public void setStatus(DownloadBundleStatus status) { this.status = status; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public List<DownloadBundlePart> getParts() { return parts; }
    public void setParts(List<DownloadBundlePart> parts) { this.parts = parts; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
```

Note: `DownloadBundlePart` is `@Embeddable` but stored as JSON via `@Type(JsonType.class)` on the list field. Hibernate serializes the list of embeddable objects to JSON.

- [ ] **Step 5: Create DownloadBundleRepository**

Create `backend/src/main/java/org/hkijena/jast/repositories/DownloadBundleRepository.java`:

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

package org.hkijena.jast.repositories;

import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadBundleRepository extends JpaRepository<DownloadBundle, String> {
    List<DownloadBundle> findByExpiresAtBefore(LocalDateTime dateTime);
    List<DownloadBundle> findByStatus(DownloadBundleStatus status);

    @Query("SELECT DISTINCT b FROM DownloadBundle b JOIN FETCH b.result r LEFT JOIN FETCH r.resultItems WHERE b.id = :id")
    Optional<DownloadBundle> findByIdWithData(@Param("id") String id);
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && mvn test -pl . -Dtest=DownloadBundleEntityTest -v`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
cd backend && git add src/main/java/org/hkijena/jast/model/DownloadBundleStatus.java src/main/java/org/hkijena/jast/model/entities/DownloadBundle.java src/main/java/org/hkijena/jast/repositories/DownloadBundleRepository.java src/test/java/org/hkijena/jast/DownloadBundleEntityTest.java
git commit -m "feat: add DownloadBundle entity, status enum, and repository"
```

---

### Task 2: Backend Payloads

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePayload.java`
- Create: `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePartPayload.java`

**Interfaces:**
- Consumes: `DownloadBundle` entity (from Task 1)
- Produces: `DownloadBundlePayload` (fields: id, status, progressPercent, progressMessage, totalSize, partCount, parts[], errorMessage)
- Produces: `DownloadBundlePartPayload` (fields: fileName, size)

- [ ] **Step 1: Create DownloadBundlePartPayload**

Create `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePartPayload.java`:

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

package org.hkijena.jast.payloads.downloadbundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;

public class DownloadBundlePartPayload {
    @JsonProperty
    private String fileName;

    @JsonProperty
    private long size;

    public DownloadBundlePartPayload() {
    }

    public DownloadBundlePartPayload(DownloadBundlePart part) {
        this.fileName = part.getFileName();
        this.size = part.getSize();
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
```

- [ ] **Step 2: Create DownloadBundlePayload**

Create `backend/src/main/java/org/hkijena/jast/payloads/downloadbundle/DownloadBundlePayload.java`:

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

package org.hkijena.jast.payloads.downloadbundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;

import java.util.ArrayList;
import java.util.List;

public class DownloadBundlePayload {
    @JsonProperty
    private String id;

    @JsonProperty
    private DownloadBundleStatus status;

    @JsonProperty
    private int progressPercent;

    @JsonProperty
    private String progressMessage;

    @JsonProperty
    private long totalSize;

    @JsonProperty
    private int partCount;

    @JsonProperty
    private List<DownloadBundlePartPayload> parts = new ArrayList<>();

    @JsonProperty
    private String errorMessage;

    public DownloadBundlePayload() {
    }

    public DownloadBundlePayload(DownloadBundle bundle) {
        this.id = bundle.getId();
        this.status = bundle.getStatus();
        this.progressPercent = bundle.getProgressPercent();
        this.progressMessage = bundle.getProgressMessage();
        this.totalSize = bundle.getTotalSize();
        this.partCount = bundle.getPartCount();
        this.errorMessage = bundle.getErrorMessage();
        for (DownloadBundle.DownloadBundlePart part : bundle.getParts()) {
            this.parts.add(new DownloadBundlePartPayload(part));
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public DownloadBundleStatus getStatus() { return status; }
    public void setStatus(DownloadBundleStatus status) { this.status = status; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public List<DownloadBundlePartPayload> getParts() { return parts; }
    public void setParts(List<DownloadBundlePartPayload> parts) { this.parts = parts; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
cd backend && git add src/main/java/org/hkijena/jast/payloads/downloadbundle/
git commit -m "feat: add DownloadBundle API payloads"
```

---

### Task 3: Backend Service — ZIP Generation

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`

**Interfaces:**
- Consumes: `DownloadBundle` entity, `DownloadBundleRepository`, `FileStorageService`, `ResultRepository`, `JobScheduler`, `AccountConfig`, `UserService`
- Consumes: `FileStorageService.getFilePath(String)` → `Path`, `FileStorageService.getStorageLocation()` → `String`, `FileStorageService.delete(String)` → `boolean`
- Consumes: `Result.getResultItems()` → `List<ResultItem>`, `ResultItem.getPath()`, `ResultItem.getName()`, `ResultItem.getRawDataFileId()`, `ResultItem.getRawDataFileSize()`
- Consumes: `Result.getProject().canAccess(Authentication, AccountConfig)` → `boolean`
- Produces: `DownloadBundleService.prepareDownload(long resultId, String path, Authentication)` → `DownloadBundle`
- Produces: `DownloadBundleService.getBundle(String bundleId, Authentication)` → `DownloadBundle`
- Produces: `DownloadBundleService.servePart(String bundleId, int partIndex, Authentication)` → `Path` (the file path to stream)
- Produces: `DownloadBundleService.generateBundle(String bundleId)` — called by JobRunr
- Produces: `DownloadBundleService.cleanupExpired()` — called by scheduler

- [ ] **Step 1: Create DownloadBundleService**

Create `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`:

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

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;
import org.hkijena.jast.model.entities.Result;
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.jobrunr.jobs.lambdas.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
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
    private static final long MAX_PART_SIZE = 2_048_000_000L; // ~1.9GB to account for ZIP overhead
    private static final int BUFFER_SIZE = 8192;

    private final DownloadBundleRepository downloadBundleRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final AccountConfig accountConfig;
    private final UserService userService;
    private final JobScheduler jobScheduler;

    @Autowired
    public DownloadBundleService(DownloadBundleRepository downloadBundleRepository,
                                  ResultRepository resultRepository,
                                  FileStorageService fileStorageService,
                                  AccountConfig accountConfig,
                                  UserService userService,
                                  JobScheduler jobScheduler) {
        this.downloadBundleRepository = downloadBundleRepository;
        this.resultRepository = resultRepository;
        this.fileStorageService = fileStorageService;
        this.accountConfig = accountConfig;
        this.userService = userService;
        this.jobScheduler = jobScheduler;
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

    public DownloadBundle prepareDownload(long resultId, String path, Authentication authentication) {
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
        bundle.setCreatedAt(LocalDateTime.now());
        bundle.setExpiresAt(LocalDateTime.now().plusHours(1));
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
                    filterPath = "/" + filterPath;
                }
                if (displayPath.startsWith(filterPath)) {
                    matchingItems.add(item);
                }
            }

            matchingItems.sort(Comparator.comparing(item -> (item.getPath() == null ? "" : item.getPath()) + "/" + (item.getName() == null ? "" : item.getName())));

            List<List<ResultItem>> parts = splitIntoParts(matchingItems);
            bundle.setPartCount(parts.size());

            String baseName = sanitizeFileName(result.getName());
            List<DownloadBundlePart> completedParts = new ArrayList<>();
            int totalItems = matchingItems.size();
            int processedItems = 0;
            Path storageDir = Paths.get(fileStorageService.getStorageLocation());

            for (int partIdx = 0; partIdx < parts.size(); partIdx++) {
                String fileId = UUID.randomUUID().toString();
                Path zipPath = storageDir.resolve(fileId);
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

                updateProgress(bundleId, (int) ((partIdx + 1) * 100L / parts.size()),
                        "Completed part " + (partIdx + 1) + " of " + parts.size());
            }

            bundle = downloadBundleRepository.findById(bundleId).orElseThrow();
            bundle.setParts(completedParts);
            bundle.setStatus(DownloadBundleStatus.Ready);
            bundle.setProgressPercent(100);
            bundle.setProgressMessage("Ready");
            downloadBundleRepository.save(bundle);

        } catch (Exception e) {
            LOGGER.error("Failed to generate download bundle {}", bundleId, e);
            bundle = downloadBundleRepository.findById(bundleId).orElseThrow();
            bundle.setStatus(DownloadBundleStatus.Failed);
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

    private List<List<ResultItem>> splitIntoParts(List<ResultItem> items) {
        List<List<ResultItem>> parts = new ArrayList<>();
        List<ResultItem> currentPart = new ArrayList<>();
        long currentSize = 0;

        for (ResultItem item : items) {
            long itemSize = item.getRawDataFileSize();
            if (currentSize + itemSize > MAX_PART_SIZE && !currentPart.isEmpty()) {
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
```

Note: `generateBundle` is intentionally NOT `@Transactional`. It uses `findByIdWithData` (a `JOIN FETCH` query) to eagerly load the bundle + result + result items in a single auto-transactional repository call. After that, progress updates via `updateProgress` → `downloadBundleRepository.save()` each create their own short transactions (Spring Data JPA auto-commits `save()`). This ensures progress is visible to polling mid-generation. If `generateBundle` were `@Transactional`, all progress updates would be held in one transaction and only visible after the method returns.

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Run existing tests to verify nothing is broken**

Run: `cd backend && mvn test -v`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
cd backend && git add src/main/java/org/hkijena/jast/services/DownloadBundleService.java
git commit -m "feat: add DownloadBundleService with ZIP generation, splitting, and cleanup"
```

---

### Task 4: Backend Controller + Remove Streaming ZIP Endpoint

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/controller/DownloadBundleController.java`
- Modify: `backend/src/main/java/org/hkijena/jast/controller/ResultsController.java` — remove `downloadResultZip`, `sanitizeFileName`, and unused imports (lines 161-241 in current file)

**Interfaces:**
- Consumes: `DownloadBundleService.prepareDownload(long, String, Authentication)` → `DownloadBundle`
- Consumes: `DownloadBundleService.getBundle(String, Authentication)` → `DownloadBundle`
- Consumes: `DownloadBundleService.getPartFilePath(String, int, Authentication)` → `Path`
- Consumes: `DownloadBundleService.getPartFileName(String, int, Authentication)` → `String`
- Consumes: `DownloadBundlePayload` (from Task 2)
- Produces: 3 REST endpoints (see below)

- [ ] **Step 1: Create DownloadBundleController**

Create `backend/src/main/java/org/hkijena/jast/controller/DownloadBundleController.java`:

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

package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.payloads.downloadbundle.DownloadBundlePayload;
import org.hkijena.jast.services.DownloadBundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class DownloadBundleController {

    private final DownloadBundleService downloadBundleService;

    @Autowired
    public DownloadBundleController(DownloadBundleService downloadBundleService) {
        this.downloadBundleService = downloadBundleService;
    }

    @PostMapping("/api/result/{id}/prepare-download")
    public ResponseEntity<DownloadBundlePayload> prepareDownload(
            Authentication authentication,
            @PathVariable("id") long id,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path
    ) {
        DownloadBundle bundle = downloadBundleService.prepareDownload(id, path, authentication);
        return ResponseEntity.ok(new DownloadBundlePayload(bundle));
    }

    @GetMapping("/api/download-bundle/{bundleId}")
    public ResponseEntity<DownloadBundlePayload> getBundle(
            Authentication authentication,
            @PathVariable String bundleId
    ) {
        DownloadBundle bundle = downloadBundleService.getBundle(bundleId, authentication);
        return ResponseEntity.ok(new DownloadBundlePayload(bundle));
    }

    @GetMapping("/api/download-bundle/{bundleId}/part/{partIndex}")
    public void downloadPart(
            HttpServletResponse response,
            Authentication authentication,
            @PathVariable String bundleId,
            @PathVariable int partIndex
    ) throws IOException {
        Path filePath = downloadBundleService.getPartFilePath(bundleId, partIndex, authentication);
        String fileName = downloadBundleService.getPartFileName(bundleId, partIndex, authentication);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", Long.toString(Files.size(filePath)));

        OutputStream outputStream = response.getOutputStream();
        try (var inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        }
        outputStream.flush();
    }
}
```

Note: We use manual streaming with `Content-Length` instead of `ResponseEntity<Resource>` because the existing codebase pattern uses `HttpServletResponse` directly, and this gives us explicit control over the response headers. The `Accept-Ranges: bytes` header signals to the browser that Range requests are supported; Spring's `DispatcherServlet` will handle `Range` headers automatically for this type of response when the `Content-Length` is set.

- [ ] **Step 2: Remove the streaming ZIP endpoint from ResultsController**

In `backend/src/main/java/org/hkijena/jast/controller/ResultsController.java`, remove the `downloadResultZip` method, the `sanitizeFileName` method, and the now-unused imports (`FileInputStream`, `OutputStream`, `Files`, `Path`, `RequestParam`, `ZipEntry`, `ZipOutputStream`).

Remove the `downloadResultZip` method (the entire method from `@GetMapping("/api/result/{id}/download-zip")` through its closing brace) and the `sanitizeFileName` private method.

Remove these imports that are no longer needed:
```java
import org.springframework.web.bind.annotation.RequestParam;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
```

Keep these imports that are still used:
```java
import java.io.IOException;  // used by getThumbnail, getRaw
```

After removal, the file should end with the `markResultViewed` method and the closing brace of the class.

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: No errors

- [ ] **Step 4: Run all tests**

Run: `cd backend && mvn test -v`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
cd backend && git add src/main/java/org/hkijena/jast/controller/DownloadBundleController.java src/main/java/org/hkijena/jast/controller/ResultsController.java
git commit -m "feat: add DownloadBundleController, remove streaming ZIP endpoint from ResultsController"
```

---

### Task 5: Frontend — Types + ResultsViewLayout Download Flow

**Files:**
- Create: `frontend/src/types/downloadBundle.ts`
- Modify: `frontend/src/layouts/ResultsViewLayout.vue` — replace the `downloadZip` function with the new polling + download flow

**Interfaces:**
- Consumes: Backend API endpoints from Task 4: `POST /api/result/{id}/prepare-download?path=...`, `GET /api/download-bundle/{bundleId}`, `GET /api/download-bundle/{bundleId}/part/{partIndex}`
- Consumes: `apiBase` from `src/types/api.ts`, `useAuthStore` from `stores/auth-store`
- Produces: Updated `downloadZip(path: string)` function in `ResultsViewLayout.vue` that uses background ZIP generation

- [ ] **Step 1: Create downloadBundle.ts types**

Create `frontend/src/types/downloadBundle.ts`:

```typescript
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

export enum DownloadBundleStatus {
  Preparing = "Preparing",
  Ready = "Ready",
  Failed = "Failed",
  Expired = "Expired",
}

export class DownloadBundlePartPayload {
  fileName: string = "";
  size: number = 0;
}

export class DownloadBundlePayload {
  id: string = "";
  status: DownloadBundleStatus = DownloadBundleStatus.Preparing;
  progressPercent: number = 0;
  progressMessage: string = "";
  totalSize: number = 0;
  partCount: number = 0;
  parts: DownloadBundlePartPayload[] = [];
  errorMessage: string = "";
}
```

- [ ] **Step 2: Update ResultsViewLayout.vue imports**

In `frontend/src/layouts/ResultsViewLayout.vue`, update the script imports section.

Replace the import block that currently has:
```typescript
import {downloadFromApi, ensureExtension, loadPayloadInstanceFromApi} from 'src/types/common';
import {FullResultPayload, ResultItemPayload, showResultItem} from 'src/types/results';
import {formatFileSize, makeFilesystemCompatible, sortPathsByHierarchy} from "src/types/utils";
import {useAuthStore} from "stores/auth-store";
import {apiBase} from "src/types/api";
```

With:
```typescript
import {downloadFromApi, ensureExtension, loadPayloadInstanceFromApi} from 'src/types/common';
import {FullResultPayload, ResultItemPayload, showResultItem} from 'src/types/results';
import {formatFileSize, makeFilesystemCompatible, sortPathsByHierarchy} from "src/types/utils";
import {useAuthStore} from "stores/auth-store";
import {apiBase} from "src/types/api";
import {DownloadBundlePayload, DownloadBundleStatus} from "src/types/downloadBundle";
```

- [ ] **Step 3: Replace the downloadZip function**

In `frontend/src/layouts/ResultsViewLayout.vue`, replace the entire `downloadZip` function (the function that currently starts with `function downloadZip(path: string) {` and ends with the closing `}` after the `.onOk` block) with:

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
  $q.dialog({
    title: 'Download results',
    message: `You are about to download ${toDownload.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?`,
    html: true,
    cancel: true,
    persistent: true
  }).onOk(() => {
    const authStore = useAuthStore();
    api.post(`/result/${resultId}/prepare-download`, null, {
      params: { path: path }
    }).then((response) => {
      const bundle: DownloadBundlePayload = response.data;
      pollDownloadBundle(bundle, authStore.accessToken);
    }).catch(() => {
      sendFailureNotification("Failed to start download preparation.");
    });
  })
}

function pollDownloadBundle(bundle: DownloadBundlePayload, accessToken: string) {
  const shouldCancel = ref<boolean>(false);
  const progressDialog = $q.dialog({
    title: 'Preparing download ...',
    message: 'Starting ...',
    progress: {
      spinner: true,
    },
    persistent: true,
    ok: false,
    cancel: true,
  });

  progressDialog.onCancel(() => {
    shouldCancel.value = true;
  });

  const pollInterval = setInterval(() => {
    if (shouldCancel.value) {
      clearInterval(pollInterval);
      return;
    }

    api.get(`/download-bundle/${bundle.id}`).then((response) => {
      const updated: DownloadBundlePayload = response.data;

      if (updated.status === DownloadBundleStatus.Preparing) {
        progressDialog.update({
          message: `${updated.progressPercent}% - ${updated.progressMessage}`
        });
      } else if (updated.status === DownloadBundleStatus.Ready) {
        clearInterval(pollInterval);
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
      } else if (updated.status === DownloadBundleStatus.Failed) {
        clearInterval(pollInterval);
        progressDialog.hide();
        sendFailureNotification("Download preparation failed: " + (updated.errorMessage || "Unknown error"));
      } else if (updated.status === DownloadBundleStatus.Expired) {
        clearInterval(pollInterval);
        progressDialog.hide();
        sendFailureNotification("Download bundle expired. Please try again.");
      }
    }).catch(() => {
      clearInterval(pollInterval);
      progressDialog.hide();
      sendFailureNotification("Failed to check download status.");
    });
  }, 2000);
}

function triggerDownload(bundleId: string, partIndex: number, accessToken: string) {
  const link = document.createElement('a');
  link.href = `${apiBase}/download-bundle/${bundleId}/part/${partIndex}?token=${encodeURIComponent(accessToken)}`;
  link.download = '';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
```

- [ ] **Step 4: Verify frontend linting**

Run: `cd frontend && npx eslint --ext .js,.ts,.vue src/layouts/ResultsViewLayout.vue src/types/downloadBundle.ts`
Expected: No errors

- [ ] **Step 5: Verify frontend typecheck (source files only)**

Run: `cd frontend && npx vue-tsc --noEmit 2>&1 | grep -v node_modules | head -20`
Expected: No errors from source files (node_modules errors are pre-existing)

- [ ] **Step 6: Run backend build to verify full integration**

Run: `cd backend && mvn test -v`
Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add frontend/src/types/downloadBundle.ts frontend/src/layouts/ResultsViewLayout.vue
git commit -m "feat: replace in-browser JSZip with server-side ZIP download polling"
```
