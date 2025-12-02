package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProviderInfoPayload {
    @JsonProperty("providerName")
    private String providerName;

    @JsonProperty("providerUrl")
    private String providerUrl;

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
}
