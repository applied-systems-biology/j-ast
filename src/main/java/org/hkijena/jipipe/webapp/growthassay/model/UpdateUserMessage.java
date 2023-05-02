package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class UpdateUserMessage {
    private String firstName = "";
    private String lastName = "";
    private String newPassword;
    private String newPasswordRepeat;

    @JsonGetter("first-name")
    public String getFirstName() {
        return firstName;
    }

    @JsonSetter("first-name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonGetter("last-name")
    public String getLastName() {
        return lastName;
    }

    @JsonSetter("last-name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonGetter("new-password")
    public String getNewPassword() {
        return newPassword;
    }

    @JsonSetter("new-password")
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @JsonGetter("new-password-repeat")
    public String getNewPasswordRepeat() {
        return newPasswordRepeat;
    }

    @JsonSetter("new-password-repeat")
    public void setNewPasswordRepeat(String newPasswordRepeat) {
        this.newPasswordRepeat = newPasswordRepeat;
    }
}
