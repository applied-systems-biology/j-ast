package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private final String customTempDirectory;
    private final String fijiPath;

    private final String fijiExecutablePath;

    private final String fijiWrapper;

    private final List<String> fijiArgs;

    private final List<String> fijiWrapperArgs;

    public RuntimeConfig(String customTempDirectory, String fijiPath, String fijiExecutablePath, String fijiWrapper, List<String> fijiArgs, List<String> fijiWrapperArgs) {
        this.customTempDirectory = customTempDirectory;
        this.fijiPath = fijiPath;
        this.fijiExecutablePath = fijiExecutablePath;
        this.fijiWrapper = fijiWrapper;
        this.fijiArgs = fijiArgs;
        this.fijiWrapperArgs = fijiWrapperArgs;
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

    public List<String> getFijiWrapperArgs() {
        return fijiWrapperArgs;
    }

    public String getFijiWrapper() {
        return fijiWrapper;
    }

    public List<String> getFijiArgs() {
        return fijiArgs;
    }
}
