package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.User;

import java.util.List;

public class UserAuthenticationResponse {
    private String token;
    private String username;
    private User.Role role;
    private List<String> authorities;
    private long guestExpireSeconds;

    @JsonGetter("guestExpireSeconds")
    public long getGuestExpireSeconds() {
        return guestExpireSeconds;
    }

    @JsonSetter("guestExpireSeconds")
    public void setGuestExpireSeconds(long guestExpireSeconds) {
        this.guestExpireSeconds = guestExpireSeconds;
    }

    @JsonGetter("token")
    public String getToken() {
        return token;
    }

    @JsonSetter("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonGetter("username")
    public String getUsername() {
        return username;
    }

    @JsonSetter("username")
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonGetter("role")
    public User.Role getRole() {
        return role;
    }

    @JsonSetter("role")
    public void setRole(User.Role role) {
        this.role = role;
    }

    @JsonGetter("authorities")
    public List<String> getAuthorities() {
        return authorities;
    }

    @JsonSetter("authorities")
    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
