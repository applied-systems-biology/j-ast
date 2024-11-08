package org.hkijena.jast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security")
@Validated
public class JwtConfig {
    private String jwtSecret = "8TJGD0tLb0M20pxRyasR3ajktjYVfhzqVauIleiKfRJdy3SVKr95JYpC7enixXeD";
    private int jwtAccessTokenExpirationInMinutes = 60;
    private int jwtRefreshTokenExpirationInMinutes = 24 * 60;

    public int getJwtRefreshTokenExpirationInMinutes() {
        return jwtRefreshTokenExpirationInMinutes;
    }

    public void setJwtRefreshTokenExpirationInMinutes(int jwtRefreshTokenExpirationInMinutes) {
        this.jwtRefreshTokenExpirationInMinutes = jwtRefreshTokenExpirationInMinutes;
    }

    public int getJwtAccessTokenExpirationInMinutes() {
        return jwtAccessTokenExpirationInMinutes;
    }

    public void setJwtAccessTokenExpirationInMinutes(int jwtAccessTokenExpirationInMinutes) {
        this.jwtAccessTokenExpirationInMinutes = jwtAccessTokenExpirationInMinutes;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
