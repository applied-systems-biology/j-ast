package org.hkijena.jast.payloads.auth;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.User;

import java.util.List;

public class UserAuthenticationLoginResponse {
    @JsonProperty
    private String accessToken;

    @JsonProperty
    private String refreshToken;

    @JsonProperty
    private String username;

    @JsonProperty
    private User.Role role;

    @JsonProperty
    private List<String> authorities;

    @JsonProperty
    private long guestExpireSeconds;

    @JsonProperty
    private int guestMaxProjects;

    @JsonProperty
    private int guestMaxImages;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public long getGuestExpireSeconds() {
        return guestExpireSeconds;
    }

    public void setGuestExpireSeconds(long guestExpireSeconds) {
        this.guestExpireSeconds = guestExpireSeconds;
    }

    public int getGuestMaxProjects() {
        return guestMaxProjects;
    }

    public void setGuestMaxProjects(int guestMaxProjects) {
        this.guestMaxProjects = guestMaxProjects;
    }

    public int getGuestMaxImages() {
        return guestMaxImages;
    }

    public void setGuestMaxImages(int guestMaxImages) {
        this.guestMaxImages = guestMaxImages;
    }
}
