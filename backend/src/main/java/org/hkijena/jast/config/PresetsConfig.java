package org.hkijena.jast.config;

import org.hkijena.jast.payloads.StripPresetPayload;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "presets")
public class PresetsConfig {
    private List<StripPresetPayload>  stripPresets =  new ArrayList<>();

    public List<StripPresetPayload> getStripPresets() {
        return stripPresets;
    }

    public void setStripPresets(List<StripPresetPayload> stripPresets) {
        this.stripPresets = stripPresets;
    }
}
