package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
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
        private double percentageOfInhibition;
        private String earlyTimePoint;
        private String lateTimePoint;

        @JsonGetter("percentage-of-inhibition")
        public double getPercentageOfInhibition() {
            return percentageOfInhibition;
        }

        @JsonSetter("percentage-of-inhibition")
        public void setPercentageOfInhibition(double percentageOfInhibition) {
            this.percentageOfInhibition = percentageOfInhibition;
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

        public void update(Dataset dataset) {
            dataset.setPercentageOfInhibition(percentageOfInhibition);
            dataset.setTimePointEarly(StringUtils.nullToEmpty(earlyTimePoint));
            dataset.setTimePointLate(StringUtils.nullToEmpty(lateTimePoint));
        }
    }

    public static class InputDataUpdateMessage {
        private long id;
        private String experiment;
        private String sample;
        private String timePoint;

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

        public void update(InputData inputData) {
            inputData.setSample(StringUtils.nullToEmpty(sample));
            inputData.setExperiment(StringUtils.nullToEmpty(experiment));
            inputData.setTimePoint(StringUtils.nullToEmpty(timePoint));
        }
    }

}
