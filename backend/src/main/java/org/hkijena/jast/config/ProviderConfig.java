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

@ConfigurationProperties(prefix = "provider")
public class ProviderConfig {
    private String providerName = "Unknown";
    private String providerUrl = "";
    private String imprintFile = "";
    private String privacyStatementFile = "";

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public String getImprintFile() {
        return imprintFile;
    }

    public void setImprintFile(String imprintFile) {
        this.imprintFile = imprintFile;
    }

    public String getPrivacyStatementFile() {
        return privacyStatementFile;
    }

    public void setPrivacyStatementFile(String privacyStatementFile) {
        this.privacyStatementFile = privacyStatementFile;
    }
}
