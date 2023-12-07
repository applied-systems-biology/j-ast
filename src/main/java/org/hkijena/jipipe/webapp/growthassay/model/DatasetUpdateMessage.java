package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.Column;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DatasetUpdateMessage {

    private Map<String, InputDataUpdateMessage> inputDataUpdateMessageMap = new HashMap<>();

    private ParametersUpdateMessage parametersUpdateMessage = new ParametersUpdateMessage();

    @JsonGetter("inputData")
    public Map<String, InputDataUpdateMessage> getInputDataUpdateMessageMap() {
        return inputDataUpdateMessageMap;
    }
    @JsonSetter("inputData")
    public void setInputDataUpdateMessageMap(Map<String, InputDataUpdateMessage> inputDataUpdateMessageMap) {
        this.inputDataUpdateMessageMap = inputDataUpdateMessageMap;
    }

    @JsonGetter("parameters")
    public ParametersUpdateMessage getParametersUpdateMessage() {
        return parametersUpdateMessage;
    }

    @JsonSetter("parameters")
    public void setParametersUpdateMessage(ParametersUpdateMessage parametersUpdateMessage) {
        this.parametersUpdateMessage = parametersUpdateMessage;
    }

    public static class ParametersUpdateMessage {
        private String growthReductionThresholds;
        private String earlyTimePoint;
        private String lateTimePoint;

        private double ddaDiskMinDiameter;
        private double ddaDiskMaxDiameter;
        private double ddaDiskMinCircularity;
        private double contrastMinValue;
        private double contrastMaxValue;
        private boolean ensureCircularPlate;

        @JsonGetter("dda-disk-min-diameter")
        public double getDdaDiskMinDiameter() {
            return ddaDiskMinDiameter;
        }

        @JsonSetter("dda-disk-min-diameter")
        public void setDdaDiskMinDiameter(double ddaDiskMinDiameter) {
            this.ddaDiskMinDiameter = ddaDiskMinDiameter;
        }

        @JsonGetter("dda-disk-max-diameter")
        public double getDdaDiskMaxDiameter() {
            return ddaDiskMaxDiameter;
        }

        @JsonSetter("dda-disk-max-diameter")
        public void setDdaDiskMaxDiameter(double ddaDiskMaxDiameter) {
            this.ddaDiskMaxDiameter = ddaDiskMaxDiameter;
        }

        @JsonGetter("dda-disk-min-circularity")
        public double getDdaDiskMinCircularity() {
            return ddaDiskMinCircularity;
        }

        @JsonSetter("dda-disk-min-circularity")
        public void setDdaDiskMinCircularity(double ddaDiskMinCircularity) {
            this.ddaDiskMinCircularity = ddaDiskMinCircularity;
        }

        @JsonGetter("growth-reduction-thresholds")
        public String getGrowthReductionThresholds() {
            return growthReductionThresholds;
        }

        @JsonSetter("growth-reduction-thresholds")
        public void setGrowthReductionThresholds(String growthReductionThresholds) {
            this.growthReductionThresholds = growthReductionThresholds;
        }

        @JsonGetter("time-point-early")
        public String getEarlyTimePoint() {
            return earlyTimePoint;
        }

        @JsonSetter("time-point-early")
        public void setEarlyTimePoint(String earlyTimePoint) {
            this.earlyTimePoint = earlyTimePoint;
        }

        @JsonGetter("time-point-late")
        public String getLateTimePoint() {
            return lateTimePoint;
        }

        @JsonSetter("time-point-late")
        public void setLateTimePoint(String lateTimePoint) {
            this.lateTimePoint = lateTimePoint;
        }

        @JsonGetter("contrast-min-value")
        public double getContrastMinValue() {
            return contrastMinValue;
        }

        @JsonSetter("contrast-min-value")
        public void setContrastMinValue(double contrastMinValue) {
            this.contrastMinValue = contrastMinValue;
        }

        @JsonGetter("contrast-max-value")
        public double getContrastMaxValue() {
            return contrastMaxValue;
        }

        @JsonSetter("contrast-max-value")
        public void setContrastMaxValue(double contrastMaxValue) {
            this.contrastMaxValue = contrastMaxValue;
        }

        @JsonGetter("ensure-circular-plate")
        public boolean isEnsureCircularPlate() {
            return ensureCircularPlate;
        }
        @JsonSetter("ensure-circular-plate")
        public void setEnsureCircularPlate(boolean ensureCircularPlate) {
            this.ensureCircularPlate = ensureCircularPlate;
        }

        public void update(Dataset dataset) {
            dataset.setGrowthReductionThresholds(growthReductionThresholds);
            dataset.setTimePointEarly(StringUtils.nullToEmpty(earlyTimePoint));
            dataset.setTimePointLate(StringUtils.nullToEmpty(lateTimePoint));
            dataset.setDdaDiskMinDiameter(ddaDiskMinDiameter);
            dataset.setDdaDiskMaxDiameter(ddaDiskMaxDiameter);
            dataset.setDdaDiskMinCircularity(ddaDiskMinCircularity);
            dataset.setContrastMinValue(contrastMinValue);
            dataset.setContrastMaxValue(contrastMaxValue);
            dataset.setEnsureCircularPlate(ensureCircularPlate);
        }
    }

    public static class InputDataUpdateMessage {
        private long id;
        private String experiment;
        private String sample;
        private String timePoint;
        private double plateDiameter;
        private String assayType;

        @JsonGetter("id")
        public long getId() {
            return id;
        }

        @JsonSetter("id")
        public void setId(long id) {
            this.id = id;
        }

        @JsonGetter("experiment")
        public String getExperiment() {
            return experiment;
        }

        @JsonSetter("experiment")
        public void setExperiment(String experiment) {
            this.experiment = experiment;
        }

        @JsonGetter("sample")
        public String getSample() {
            return sample;
        }

        @JsonSetter("sample")
        public void setSample(String sample) {
            this.sample = sample;
        }

        @JsonGetter("timePoint")
        public String getTimePoint() {
            return timePoint;
        }

        @JsonSetter("timePoint")
        public void setTimePoint(String timePoint) {
            this.timePoint = timePoint;
        }

        @JsonGetter("plateDiameter")
        public double getPlateDiameter() {
            return plateDiameter;
        }

        @JsonSetter("plateDiameter")
        public void setPlateDiameter(double plateDiameter) {
            this.plateDiameter = plateDiameter;
        }

        @JsonGetter("assayType")
        public String getAssayType() {
            return assayType;
        }

        @JsonSetter("assayType")
        public void setAssayType(String assayType) {
            this.assayType = assayType;
        }

        public void update(InputData inputData) {
            inputData.setSample(StringUtils.nullToEmpty(sample));
            inputData.setExperiment(StringUtils.nullToEmpty(experiment));
            inputData.setTimePoint(StringUtils.nullToEmpty(timePoint));
            inputData.setPlateDiameter(plateDiameter);
            inputData.setAssayType(AssayType.valueOf(StringUtils.orElse(assayType,AssayType.DDA.name())));
        }
    }

}
