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
