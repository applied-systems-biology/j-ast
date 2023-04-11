package com.example.growthassayanalyzerwebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webapp")
public class WebAppConfig {
    private String customTempDirectory;

    public String getCustomTempDirectory() {
        return customTempDirectory;
    }

    public void setCustomTempDirectory(String customTempDirectory) {
        this.customTempDirectory = customTempDirectory;
    }
}
