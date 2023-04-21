package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private final String customTempDirectory;
    private final String fijiPath;

    private final String fijiExecutablePath;

    private final String fijiWrapper;

    public RuntimeConfig(String customTempDirectory, String fijiPath, String fijiExecutablePath, String fijiWrapper) {
        this.customTempDirectory = customTempDirectory;
        this.fijiPath = fijiPath;
        this.fijiExecutablePath = fijiExecutablePath;
        this.fijiWrapper = fijiWrapper;
    }

    public String getFijiPath() {
        return fijiPath;
    }

    public String getCustomTempDirectory() {
        return customTempDirectory;
    }

    public String getFijiExecutablePath() {
        return fijiExecutablePath;
    }

    public String getFijiWrapper() {
        return fijiWrapper;
    }
}
