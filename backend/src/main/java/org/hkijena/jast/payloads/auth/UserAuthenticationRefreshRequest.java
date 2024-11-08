package org.hkijena.jast.payloads.auth;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class UserAuthenticationRefreshRequest {
    private String accessToken;
    private String refreshToken;

    @JsonGetter("refreshToken")
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonSetter("refreshToken")
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonGetter("accessToken")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonSetter("accessToken")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
