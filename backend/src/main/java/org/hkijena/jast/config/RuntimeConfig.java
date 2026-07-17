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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
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
    private boolean verbose;
    private boolean preferSystemPackages;
    private List<SystemPackage> systemPackages = new ArrayList<>();

    public RuntimeConfig() {
    }

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

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isPreferSystemPackages() {
        return preferSystemPackages;
    }

    public void setPreferSystemPackages(boolean preferSystemPackages) {
        this.preferSystemPackages = preferSystemPackages;
    }

    public List<SystemPackage> getSystemPackages() {
        return systemPackages;
    }

    public void setSystemPackages(List<SystemPackage> systemPackages) {
        this.systemPackages = systemPackages;
    }
}
