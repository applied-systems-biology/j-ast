package org.hkijena.jast.model.messages;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.TimeSeries;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.utils.StringUtils;

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

        public void update(TimeSeries timeSeries) {
            timeSeries.setGrowthReductionThresholds(growthReductionThresholds);
            timeSeries.setTimePointEarly(StringUtils.nullToEmpty(earlyTimePoint));
            timeSeries.setTimePointLate(StringUtils.nullToEmpty(lateTimePoint));
            timeSeries.setDdaDiskMinDiameter(ddaDiskMinDiameter);
            timeSeries.setDdaDiskMaxDiameter(ddaDiskMaxDiameter);
            timeSeries.setDdaDiskMinCircularity(ddaDiskMinCircularity);
            timeSeries.setContrastMinValue(contrastMinValue);
            timeSeries.setContrastMaxValue(contrastMaxValue);
            timeSeries.setEnsureCircularPlate(ensureCircularPlate);
        }
    }

    public static class InputDataUpdateMessage {
        private long id;
        private String experiment;
        private String sample;
        private String timePoint;
        private double plateDiameter;
        private String assayType;

        private String eTestZOIShapePresetName = "Auto";

        private double eTestZOIShapePresetMcaIntercept = 0;

        private double eTestZOIShapePresetMcaRate = 0;

        private double eTestZOIShapePresetStripIntercept = 0;

        private double eTestZOIShapePresetStripRate = 0;

        @JsonGetter("etest-zoi-shape-preset-name")
        public String geteTestZOIShapePresetName() {
            return eTestZOIShapePresetName;
        }

        @JsonSetter("etest-zoi-shape-preset-name")
        public void seteTestZOIShapePresetName(String eTestZOIShapePresetName) {
            this.eTestZOIShapePresetName = eTestZOIShapePresetName;
        }

        @JsonGetter("etest-zoi-shape-preset-mca-intercept")
        public double geteTestZOIShapePresetMcaIntercept() {
            return eTestZOIShapePresetMcaIntercept;
        }

        @JsonSetter("etest-zoi-shape-preset-mca-intercept")
        public void seteTestZOIShapePresetMcaIntercept(double eTestZOIShapePresetMcaIntercept) {
            this.eTestZOIShapePresetMcaIntercept = eTestZOIShapePresetMcaIntercept;
        }

        @JsonGetter("etest-zoi-shape-preset-mca-rate")
        public double geteTestZOIShapePresetMcaRate() {
            return eTestZOIShapePresetMcaRate;
        }

        @JsonSetter("etest-zoi-shape-preset-mca-rate")
        public void seteTestZOIShapePresetMcaRate(double eTestZOIShapePresetMcaRate) {
            this.eTestZOIShapePresetMcaRate = eTestZOIShapePresetMcaRate;
        }

        @JsonGetter("etest-zoi-shape-preset-strip-intercept")
        public double geteTestZOIShapePresetStripIntercept() {
            return eTestZOIShapePresetStripIntercept;
        }

        @JsonSetter("etest-zoi-shape-preset-strip-intercept")
        public void seteTestZOIShapePresetStripIntercept(double eTestZOIShapePresetStripIntercept) {
            this.eTestZOIShapePresetStripIntercept = eTestZOIShapePresetStripIntercept;
        }

        @JsonGetter("etest-zoi-shape-preset-strip-rate")
        public double geteTestZOIShapePresetStripRate() {
            return eTestZOIShapePresetStripRate;
        }

        @JsonSetter("etest-zoi-shape-preset-strip-rate")
        public void seteTestZOIShapePresetStripRate(double eTestZOIShapePresetStripRate) {
            this.eTestZOIShapePresetStripRate = eTestZOIShapePresetStripRate;
        }

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

        public void update(Image image) {
            image.setSample(StringUtils.nullToEmpty(sample));
            image.setExperiment(StringUtils.nullToEmpty(experiment));
            image.setTimePoint(StringUtils.nullToEmpty(timePoint));
            image.setPlateDiameter(plateDiameter);
            image.setAssayType(AssayType.valueOf(StringUtils.orElse(assayType,AssayType.DDA.name())));
            image.seteTestZOIShapePresetName(eTestZOIShapePresetName);
            image.seteTestZOIShapePresetMcaRate(eTestZOIShapePresetMcaRate);
            image.seteTestZOIShapePresetMcaIntercept(eTestZOIShapePresetMcaIntercept);
            image.seteTestZOIShapePresetStripRate(eTestZOIShapePresetStripRate);
            image.seteTestZOIShapePresetStripIntercept(eTestZOIShapePresetStripIntercept);
        }
    }

}
