package org.hkijena.jast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private String customTempDirectory;
    private String dataDirectory;
    private String fijiPath;
    private String fijiExecutablePath;
    private String fijiWrapper;
    private boolean fijiWrapperEnabled;
    private List<String> fijiArgs;
    private List<String> fijiWrapperArgs;
    private String sharedResourcesDirectory;
    private boolean keepTmp;

    public String getSharedResourcesDirectory() {
        return sharedResourcesDirectory;
    }

    public void setSharedResourcesDirectory(String sharedResourcesDirectory) {
        this.sharedResourcesDirectory = sharedResourcesDirectory;
    }

    public boolean isFijiWrapperEnabled() {
        return fijiWrapperEnabled;
    }

    public void setFijiWrapperEnabled(boolean fijiWrapperEnabled) {
        this.fijiWrapperEnabled = fijiWrapperEnabled;
    }

    public String getFijiPath() {
        return fijiPath;
    }

    public void setFijiPath(String fijiPath) {
        this.fijiPath = fijiPath;
    }

    public String getCustomTempDirectory() {
        return customTempDirectory;
    }

    public void setCustomTempDirectory(String customTempDirectory) {
        this.customTempDirectory = customTempDirectory;
    }

    public String getFijiExecutablePath() {
        return fijiExecutablePath;
    }

    public void setFijiExecutablePath(String fijiExecutablePath) {
        this.fijiExecutablePath = fijiExecutablePath;
    }

    public List<String> getFijiWrapperArgs() {
        return fijiWrapperArgs;
    }

    public void setFijiWrapperArgs(List<String> fijiWrapperArgs) {
        this.fijiWrapperArgs = fijiWrapperArgs;
    }

    public String getFijiWrapper() {
        return fijiWrapper;
    }

    public void setFijiWrapper(String fijiWrapper) {
        this.fijiWrapper = fijiWrapper;
    }

    public List<String> getFijiArgs() {
        return fijiArgs;
    }

    public void setFijiArgs(List<String> fijiArgs) {
        this.fijiArgs = fijiArgs;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public boolean isKeepTmp() {
        return keepTmp;
    }

    public void setKeepTmp(boolean keepTmp) {
        this.keepTmp = keepTmp;
    }
}
