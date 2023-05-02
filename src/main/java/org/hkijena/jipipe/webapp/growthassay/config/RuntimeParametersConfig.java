package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime-parameters")
public class RuntimeParametersConfig {
    private final String timePointEarlyParameterKey;

    private final String minRelDiffThresholdParameterKey;

    public RuntimeParametersConfig(String timePointEarlyParameterKey, String minRelDiffThresholdParameterKey) {
        this.timePointEarlyParameterKey = timePointEarlyParameterKey;
        this.minRelDiffThresholdParameterKey = minRelDiffThresholdParameterKey;
    }

    public String getTimePointEarlyParameterKey() {
        return timePointEarlyParameterKey;
    }

    public String getMinRelDiffThresholdParameterKey() {
        return minRelDiffThresholdParameterKey;
    }
}
