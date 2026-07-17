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

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "accounts")
@Validated
@Component
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
    private boolean disableAuth = false;

    public boolean isDisableAuth() {
        return disableAuth;
    }

    public void setDisableAuth(boolean disableAuth) {
        this.disableAuth = disableAuth;
    }

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
