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

import org.hkijena.jast.payloads.StripPresetPayload;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "presets")
public class PresetsConfig {
    private List<StripPresetPayload> stripPresets = new ArrayList<>();

    public List<StripPresetPayload> getStripPresets() {
        return stripPresets;
    }

    public void setStripPresets(List<StripPresetPayload> stripPresets) {
        this.stripPresets = stripPresets;
    }
}
