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
