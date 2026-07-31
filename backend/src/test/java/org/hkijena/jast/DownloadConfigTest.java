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
        "download.expiry-hours=6",
        "org.jobrunr.dashboard.enabled=false"
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
