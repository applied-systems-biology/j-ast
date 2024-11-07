package org.hkijena.jast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security")
@Validated
public class JwtConfig {
    private String jwtSecret = "8TJGD0tLb0M20pxRyasR3ajktjYVfhzqVauIleiKfRJdy3SVKr95JYpC7enixXeD";
    private int jwtExpirationInMinutes = 60;

    public int getJwtExpirationInMinutes() {
        return jwtExpirationInMinutes;
    }

    public void setJwtExpirationInMinutes(int jwtExpirationInMinutes) {
        this.jwtExpirationInMinutes = jwtExpirationInMinutes;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
