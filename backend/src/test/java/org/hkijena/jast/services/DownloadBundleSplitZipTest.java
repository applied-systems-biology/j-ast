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
        // Use random data so it doesn't compress (all-zeros would compress to ~2KB)
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("data.bin"));
            byte[] data = new byte[2_500_000];
            new java.util.Random(42).nextBytes(data);
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
