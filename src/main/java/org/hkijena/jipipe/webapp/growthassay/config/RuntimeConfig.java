package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private String customTempDirectory;
    private String fijiPath;

    private String fijiExecutablePath;

    private String fijiWrapper;

    private boolean fijiWrapperEnabled;
    private List<String> fijiArgs;

    private List<String> fijiWrapperArgs;

    public boolean isFijiWrapperEnabled() {
        return fijiWrapperEnabled;
    }

    public void setFijiWrapperEnabled(boolean fijiWrapperEnabled) {
        this.fijiWrapperEnabled = fijiWrapperEnabled;
    }

    public void setCustomTempDirectory(String customTempDirectory) {
        this.customTempDirectory = customTempDirectory;
    }

    public void setFijiPath(String fijiPath) {
        this.fijiPath = fijiPath;
    }

    public void setFijiExecutablePath(String fijiExecutablePath) {
        this.fijiExecutablePath = fijiExecutablePath;
    }

    public void setFijiWrapper(String fijiWrapper) {
        this.fijiWrapper = fijiWrapper;
    }

    public void setFijiArgs(List<String> fijiArgs) {
        this.fijiArgs = fijiArgs;
    }

    public void setFijiWrapperArgs(List<String> fijiWrapperArgs) {
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
