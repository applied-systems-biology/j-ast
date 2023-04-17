package org.hkijena.jipipe.webapp.growthassay.config;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "runtime")
public class RuntimeConfig {
    private final String customTempDirectory;
    private final String fijiPath;

    public RuntimeConfig(String customTempDirectory, String fijiPath) {
        this.customTempDirectory = customTempDirectory;
        this.fijiPath = fijiPath;
    }

    public String getFijiPath() {
        return fijiPath;
    }

    public String getCustomTempDirectory() {
        return customTempDirectory;
    }

    public Path getFijiRoot() {
        return Paths.get(fijiPath);
    }

    public Path getFijiExecutablePath() {
        if(SystemUtils.IS_OS_WINDOWS) {
            return getFijiRoot().resolve("ImageJ-win64.exe");
        }
        else if(SystemUtils.IS_OS_LINUX) {
            return getFijiRoot().resolve("ImageJ-linux64");
        }
        else if(SystemUtils.IS_OS_MAC_OSX) {
            return getFijiRoot().resolve("Contents").resolve("MacOS").resolve("ImageJ-macosx");
        }
        else {
            throw new UnsupportedOperationException("Unknown operating system!");
        }
    }
}
