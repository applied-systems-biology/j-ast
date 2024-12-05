package org.hkijena.jast.payloads.register;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserRegistrationAllowedFeaturesPayload {
    @JsonProperty
    private boolean allowSelfRegister = false;

    @JsonProperty
    private boolean allowGuestAccounts = true;

    @JsonProperty
    private int guestProjectLimit = 1;

    @JsonProperty
    private int guestImageLimit = 10;

    @JsonProperty
    private int guestAccountExpireMinutes = 60 * 24 * 3;

    @JsonProperty
    private String adminContact = "<Not provided>";

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
}
