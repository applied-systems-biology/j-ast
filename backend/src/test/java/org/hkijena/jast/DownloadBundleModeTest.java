/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-HKI.de/en/applied-systems-biology.html
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
