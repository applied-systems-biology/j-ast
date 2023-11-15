package org.hkijena.jipipe.webapp.growthassay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime-parameters")
public class RuntimeParametersConfig {
    private String timePointEarlyParameterKey;
    private String growthReductionThresholdsParameterKey;
    private String ddaMinDiameterParameterKey;
    private String ddaMaxDiameterParameterKey;
    private String ddaMinCircularityParameterKey;

    public void setTimePointEarlyParameterKey(String timePointEarlyParameterKey) {
        this.timePointEarlyParameterKey = timePointEarlyParameterKey;
    }

    public void setGrowthReductionThresholdsParameterKey(String growthReductionThresholdsParameterKey) {
        this.growthReductionThresholdsParameterKey = growthReductionThresholdsParameterKey;
    }

    public String getDdaMinDiameterParameterKey() {
        return ddaMinDiameterParameterKey;
    }

    public void setDdaMinDiameterParameterKey(String ddaMinDiameterParameterKey) {
        this.ddaMinDiameterParameterKey = ddaMinDiameterParameterKey;
    }

    public String getDdaMaxDiameterParameterKey() {
        return ddaMaxDiameterParameterKey;
    }

    public void setDdaMaxDiameterParameterKey(String ddaMaxDiameterParameterKey) {
        this.ddaMaxDiameterParameterKey = ddaMaxDiameterParameterKey;
    }

    public String getDdaMinCircularityParameterKey() {
        return ddaMinCircularityParameterKey;
    }

    public void setDdaMinCircularityParameterKey(String ddaMinCircularityParameterKey) {
        this.ddaMinCircularityParameterKey = ddaMinCircularityParameterKey;
    }

    public String getTimePointEarlyParameterKey() {
        return timePointEarlyParameterKey;
    }

    public String getGrowthReductionThresholdsParameterKey() {
        return growthReductionThresholdsParameterKey;
    }
}
