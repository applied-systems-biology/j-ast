package org.hkijena.jipipe.webapp.growthassay.config;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private final String customTempDirectory;
    private final String fijiPath;

    private final String fijiExecutablePath;

    public RuntimeConfig(String customTempDirectory, String fijiPath, String fijiExecutablePath) {
        this.customTempDirectory = customTempDirectory;
        this.fijiPath = fijiPath;
        this.fijiExecutablePath = fijiExecutablePath;
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
}
