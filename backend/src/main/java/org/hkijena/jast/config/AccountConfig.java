package org.hkijena.jast.config;

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
    private int guestProjectLimit = 1;
    private int guestImageLimit = 10;
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

    public int getGuestProjectLimit() {
        return guestProjectLimit;
    }

    public void setGuestProjectLimit(int guestProjectLimit) {
        this.guestProjectLimit = guestProjectLimit;
    }

    public int getGuestImageLimit() {
        return guestImageLimit;
    }

    public void setGuestImageLimit(int guestImageLimit) {
        this.guestImageLimit = guestImageLimit;
    }

    public int getGuestAccountExpireMinutes() {
        return guestAccountExpireMinutes;
    }

    public void setGuestAccountExpireMinutes(int guestAccountExpireMinutes) {
        this.guestAccountExpireMinutes = guestAccountExpireMinutes;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
