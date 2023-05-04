package org.hkijena.jipipe.webapp.growthassay.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "accounts")
@Validated
public class AccountConfig {
    @NotEmpty
    private final String adminUserName;

    @NotEmpty
    private final String adminPassword;

    public AccountConfig(String adminUserName, String adminPassword) {
        this.adminUserName = adminUserName;
        this.adminPassword = adminPassword;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
}
