package org.hkijena.jast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime-parameters")
public class RuntimeParametersConfig {
    private String inputFolderListParameterKey;
    private String timePointEarlyFilterParameterKey;
    private String growthReductionThresholdsParameterKey;
    private String ddaMinDiameterParameterKey;
    private String ddaMaxDiameterParameterKey;
    private String ddaMinCircularityParameterKey;
    private String contrastMinValueParameterKey;
    private String contrastMaxValueParameterKey;
    private String ensureCircularPlateParameterKey;

    public String getEnsureCircularPlateParameterKey() {
        return ensureCircularPlateParameterKey;
    }

    public void setEnsureCircularPlateParameterKey(String ensureCircularPlateParameterKey) {
        this.ensureCircularPlateParameterKey = ensureCircularPlateParameterKey;
    }

    public void setTimePointEarlyFilterParameterKey(String timePointEarlyFilterParameterKey) {
        this.timePointEarlyFilterParameterKey = timePointEarlyFilterParameterKey;
    }

    public void setGrowthReductionThresholdsParameterKey(String growthReductionThresholdsParameterKey) {
        this.growthReductionThresholdsParameterKey = growthReductionThresholdsParameterKey;
    }

    public String getInputFolderListParameterKey() {
        return inputFolderListParameterKey;
    }

    public void setInputFolderListParameterKey(String inputFolderListParameterKey) {
        this.inputFolderListParameterKey = inputFolderListParameterKey;
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

    public String getTimePointEarlyFilterParameterKey() {
        return timePointEarlyFilterParameterKey;
    }

    public String getGrowthReductionThresholdsParameterKey() {
        return growthReductionThresholdsParameterKey;
    }

    public String getContrastMinValueParameterKey() {
        return contrastMinValueParameterKey;
    }

    public void setContrastMinValueParameterKey(String contrastMinValueParameterKey) {
        this.contrastMinValueParameterKey = contrastMinValueParameterKey;
    }

    public String getContrastMaxValueParameterKey() {
        return contrastMaxValueParameterKey;
    }

    public void setContrastMaxValueParameterKey(String contrastMaxValueParameterKey) {
        this.contrastMaxValueParameterKey = contrastMaxValueParameterKey;
    }
}
