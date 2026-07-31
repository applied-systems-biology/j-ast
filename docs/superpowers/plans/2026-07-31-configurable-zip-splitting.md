# Configurable ZIP Splitting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the ZIP download part-split size and bundle expiry time configurable via `application.yml` so operators can tune for slow network links.

**Architecture:** New `DownloadConfig` class (`@ConfigurationProperties(prefix = "download")`) following the existing `AccountConfig` pattern. `DownloadBundleService` constructor-injects it and replaces two hardcoded values: `MAX_PART_SIZE` (static final) and `plusHours(1)` (literal).

**Tech Stack:** Java 21, Spring Boot, Spring Boot Configuration Properties, Jakarta Validation, JUnit 5, Mockito (bundled in spring-boot-starter-test)

## Global Constraints

- Config classes use `@ConfigurationProperties` + `@Validated` + `@Component` — never `@Value` (codebase convention)
- MB unit uses decimal multiplier `1_000_000` (matches existing `2_048_000_000L = 2048 * 1_000_000`)
- Defaults must preserve current behavior: `maxPartSizeMb = 2048`, `expiryHours = 1`
- Tests run from `backend/` directory: `cd backend && mvn test`
- Database target is MariaDB; tests run on H2

---

### Task 1: Create `DownloadConfig` class and register it

**Files:**
- Create: `backend/src/main/java/org/hkijena/jast/config/DownloadConfig.java`
- Modify: `backend/src/main/java/org/hkijena/jast/JASTWebApplicationServer.java:24`
- Modify: `backend/src/main/resources/application.yml` (add `download:` block after `accounts:` block, before `security:`)
- Test: `backend/src/test/java/org/hkijena/jast/DownloadConfigTest.java`

**Interfaces:**
- Produces: `DownloadConfig` bean with `getMaxPartSizeMb()` returning `long` and `getExpiryHours()` returning `int`. Constructor-injected into `DownloadBundleService` in Task 2.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/org/hkijena/jast/DownloadConfigTest.java`:

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

import org.hkijena.jast.config.DownloadConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "download.max-part-size-mb=500",
        "download.expiry-hours=6"
})
class DownloadConfigTest {

    @Autowired
    private DownloadConfig downloadConfig;

    @Test
    void configLoadsOverriddenValues() {
        assertEquals(500, downloadConfig.getMaxPartSizeMb());
        assertEquals(6, downloadConfig.getExpiryHours());
    }

    @Test
    void defaultsAreCorrectWhenNotOverridden() {
        DownloadConfig config = new DownloadConfig();
        assertEquals(2048, config.getMaxPartSizeMb());
        assertEquals(1, config.getExpiryHours());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=DownloadConfigTest`
Expected: FAIL — compilation error (`DownloadConfig` class does not exist)

- [ ] **Step 3: Create the `DownloadConfig` class**

Create `backend/src/main/java/org/hkijena/jast/config/DownloadConfig.java`:

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

package org.hkijena.jast.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "download")
@Validated
@Component
public class DownloadConfig {

    @Min(1)
    private long maxPartSizeMb = 2048;

    @Min(1)
    private int expiryHours = 1;

    public long getMaxPartSizeMb() {
        return maxPartSizeMb;
    }

    public void setMaxPartSizeMb(long maxPartSizeMb) {
        this.maxPartSizeMb = maxPartSizeMb;
    }

    public int getExpiryHours() {
        return expiryHours;
    }

    public void setExpiryHours(int expiryHours) {
        this.expiryHours = expiryHours;
    }
}
```

- [ ] **Step 4: Register in `@EnableConfigurationProperties`**

In `backend/src/main/java/org/hkijena/jast/JASTWebApplicationServer.java`, change line 24 from:

```java
@EnableConfigurationProperties({RuntimeConfig.class, AccountConfig.class, JwtConfig.class, ProviderConfig.class, PresetsConfig.class})
```

to:

```java
@EnableConfigurationProperties({RuntimeConfig.class, AccountConfig.class, JwtConfig.class, ProviderConfig.class, PresetsConfig.class, DownloadConfig.class})
```

- [ ] **Step 5: Add YAML config block**

In `backend/src/main/resources/application.yml`, add after the `accounts:` block (after line 65) and before `security:`:

```yaml
download:
  max-part-size-mb: 2048
  expiry-hours: 1
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && mvn test -pl . -Dtest=DownloadConfigTest`
Expected: PASS — both tests green

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/org/hkijena/jast/config/DownloadConfig.java backend/src/main/java/org/hkijena/jast/JASTWebApplicationServer.java backend/src/main/resources/application.yml backend/src/test/java/org/hkijena/jast/DownloadConfigTest.java
git commit -m "Add DownloadConfig for configurable ZIP part size and expiry"
```

---

### Task 2: Wire `DownloadConfig` into `DownloadBundleService`

**Files:**
- Modify: `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java` (lines 18, 55, 61-78, 108, 306-331)
- Test: `backend/src/test/java/org/hkijena/jast/DownloadBundleSplitTest.java`

**Interfaces:**
- Consumes: `DownloadConfig` from Task 1 — `getMaxPartSizeMb()` returns `long` (MB), `getExpiryHours()` returns `int` (hours)
- Produces: `DownloadBundleService` constructor now accepts `DownloadConfig` as 7th parameter. `splitIntoParts` changed from `private` to package-private for testing.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/org/hkijena/jast/DownloadBundleSplitTest.java`:

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

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.DownloadConfig;
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.DownloadBundleService;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.UserService;
import org.junit.jupiter.api.Test;
import org.jobrunr.scheduling.JobScheduler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DownloadBundleSplitTest {

    @Test
    void splitRespectsConfiguredMaxPartSize() {
        DownloadConfig config = new DownloadConfig();
        config.setMaxPartSizeMb(1); // 1 MB = 1,000,000 bytes

        DownloadBundleService service = new DownloadBundleService(
                mock(DownloadBundleRepository.class),
                mock(ResultRepository.class),
                mock(FileStorageService.class),
                mock(AccountConfig.class),
                mock(UserService.class),
                mock(JobScheduler.class),
                config
        );

        // Three items: 0.4 MB, 0.4 MB, 0.4 MB = 1.2 MB total
        // With 1 MB limit: first two in part 1 (0.8 MB), third in part 2 (0.4 MB)
        List<ResultItem> items = new ArrayList<>();
        items.add(makeItem(400_000));
        items.add(makeItem(400_000));
        items.add(makeItem(400_000));

        List<List<ResultItem>> parts = service.splitIntoParts(items);
        assertEquals(2, parts.size());
        assertEquals(2, parts.get(0).size());
        assertEquals(1, parts.get(1).size());
    }

    @Test
    void singleLargeItemGoesInItsOwnPart() {
        DownloadConfig config = new DownloadConfig();
        config.setMaxPartSizeMb(1);

        DownloadBundleService service = new DownloadBundleService(
                mock(DownloadBundleRepository.class),
                mock(ResultRepository.class),
                mock(FileStorageService.class),
                mock(AccountConfig.class),
                mock(UserService.class),
                mock(JobScheduler.class),
                config
        );

        // One item larger than the limit gets its own part (no per-file splitting)
        List<ResultItem> items = new ArrayList<>();
        items.add(makeItem(2_000_000));

        List<List<ResultItem>> parts = service.splitIntoParts(items);
        assertEquals(1, parts.size());
        assertEquals(1, parts.get(0).size());
    }

    @Test
    void emptyItemsProduceSingleEmptyPart() {
        DownloadConfig config = new DownloadConfig();

        DownloadBundleService service = new DownloadBundleService(
                mock(DownloadBundleRepository.class),
                mock(ResultRepository.class),
                mock(FileStorageService.class),
                mock(AccountConfig.class),
                mock(UserService.class),
                mock(JobScheduler.class),
                config
        );

        List<List<ResultItem>> parts = service.splitIntoParts(new ArrayList<>());
        assertEquals(1, parts.size());
        assertTrue(parts.get(0).isEmpty());
    }

    private ResultItem makeItem(long size) {
        ResultItem item = new ResultItem();
        item.setRawDataFileSize(size);
        return item;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=DownloadBundleSplitTest`
Expected: FAIL — compilation error (constructor does not accept `DownloadConfig`, `splitIntoParts` is `private`)

- [ ] **Step 3: Add `DownloadConfig` import to `DownloadBundleService`**

In `backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java`, add after line 18 (`import org.hkijena.jast.config.AccountConfig;`):

```java
import org.hkijena.jast.config.DownloadConfig;
```

- [ ] **Step 4: Remove `MAX_PART_SIZE` constant**

In `DownloadBundleService.java`, remove line 55:

```java
    private static final long MAX_PART_SIZE = 2_048_000_000L; // ~1.9GB to account for ZIP overhead
```

Keep `BUFFER_SIZE` on line 56.

- [ ] **Step 5: Add `DownloadConfig` field and constructor parameter**

In `DownloadBundleService.java`, add the field after line 62 (`private final AccountConfig accountConfig;`):

```java
    private final DownloadConfig downloadConfig;
```

Update the constructor (lines 66-78) to add `DownloadConfig downloadConfig` as the last parameter and assign it. The constructor should become:

```java
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
```

- [ ] **Step 6: Replace hardcoded expiry in `prepareDownload`**

In `DownloadBundleService.java` line 108, change:

```java
        bundle.setExpiresAt(LocalDateTime.now().plusHours(1));
```

to:

```java
        bundle.setExpiresAt(LocalDateTime.now().plusHours(downloadConfig.getExpiryHours()));
```

- [ ] **Step 7: Replace `MAX_PART_SIZE` in `splitIntoParts` and make it package-private**

In `DownloadBundleService.java`, change the method signature on line 306 from:

```java
    private List<List<ResultItem>> splitIntoParts(List<ResultItem> items) {
```

to:

```java
    List<List<ResultItem>> splitIntoParts(List<ResultItem> items) {
```

Then on line 313, change:

```java
            if (currentSize + itemSize > MAX_PART_SIZE && !currentPart.isEmpty()) {
```

to:

```java
            if (currentSize + itemSize > downloadConfig.getMaxPartSizeMb() * 1_000_000L && !currentPart.isEmpty()) {
```

- [ ] **Step 8: Run test to verify it passes**

Run: `cd backend && mvn test -pl . -Dtest=DownloadBundleSplitTest`
Expected: PASS — all three tests green

- [ ] **Step 9: Run full test suite to verify no regressions**

Run: `cd backend && mvn test`
Expected: All tests pass

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/org/hkijena/jast/services/DownloadBundleService.java backend/src/test/java/org/hkijena/jast/DownloadBundleSplitTest.java
git commit -m "Wire DownloadConfig into DownloadBundleService for configurable splitting"
```
