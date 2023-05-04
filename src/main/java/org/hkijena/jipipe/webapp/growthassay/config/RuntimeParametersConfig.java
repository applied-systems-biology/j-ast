package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime-parameters")
public class RuntimeParametersConfig {
    private String timePointEarlyParameterKey;

    private String minRelDiffThresholdParameterKey;

    public void setTimePointEarlyParameterKey(String timePointEarlyParameterKey) {
        this.timePointEarlyParameterKey = timePointEarlyParameterKey;
    }

    public void setMinRelDiffThresholdParameterKey(String minRelDiffThresholdParameterKey) {
        this.minRelDiffThresholdParameterKey = minRelDiffThresholdParameterKey;
    }

    public String getTimePointEarlyParameterKey() {
        return timePointEarlyParameterKey;
    }

    public String getMinRelDiffThresholdParameterKey() {
        return minRelDiffThresholdParameterKey;
    }
}
