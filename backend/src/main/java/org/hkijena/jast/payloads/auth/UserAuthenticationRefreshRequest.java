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
