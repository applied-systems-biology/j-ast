package org.hkijena.jipipe.webapp.growthassay.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "accounts")
@Validated
public class AccountConfig {
    @NotEmpty
    private String adminUsername;
    @NotEmpty
    private String adminPassword;
    private String adminContact = "<Admin contact not provided>";
    private boolean allowSelfRegister = false;
    private boolean allowGuestAccounts = true;
    private int guestDatasetLimit = 1;
    private int guestInputDataLimit = 10;
    private int guestAccountExpireMinutes = 60 * 24 * 3;

    public String getAdminContact() {
        return adminContact;
    }

    public void setAdminContact(String adminContact) {
        this.adminContact = adminContact;
    }

    public boolean isAllowSelfRegister() {
        return allowSelfRegister;
    }

    public void setAllowSelfRegister(boolean allowSelfRegister) {
        this.allowSelfRegister = allowSelfRegister;
    }

    public boolean isAllowGuestAccounts() {
        return allowGuestAccounts;
    }

    public void setAllowGuestAccounts(boolean allowGuestAccounts) {
        this.allowGuestAccounts = allowGuestAccounts;
    }

    public int getGuestDatasetLimit() {
        return guestDatasetLimit;
    }

    public void setGuestDatasetLimit(int guestDatasetLimit) {
        this.guestDatasetLimit = guestDatasetLimit;
    }

    public int getGuestInputDataLimit() {
        return guestInputDataLimit;
    }

    public void setGuestInputDataLimit(int guestInputDataLimit) {
        this.guestInputDataLimit = guestInputDataLimit;
    }

    public int getGuestAccountExpireMinutes() {
        return guestAccountExpireMinutes;
    }

    public void setGuestAccountExpireMinutes(int guestAccountExpireMinutes) {
        this.guestAccountExpireMinutes = guestAccountExpireMinutes;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
}
