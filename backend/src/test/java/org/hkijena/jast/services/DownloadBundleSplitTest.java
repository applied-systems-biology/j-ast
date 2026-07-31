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
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.repositories.DownloadBundleRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

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
