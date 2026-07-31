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

import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.services.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
