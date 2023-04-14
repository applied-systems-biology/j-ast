package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DatasetUpdateMessage {

    private Map<String, InputDataUpdateMessage> inputDataUpdateMessageMap = new HashMap<>();

    @JsonGetter("inputData")
    public Map<String, InputDataUpdateMessage> getInputDataUpdateMessageMap() {
        return inputDataUpdateMessageMap;
    }
    @JsonSetter("inputData")
    public void setInputDataUpdateMessageMap(Map<String, InputDataUpdateMessage> inputDataUpdateMessageMap) {
        this.inputDataUpdateMessageMap = inputDataUpdateMessageMap;
    }

    public static class InputDataUpdateMessage {
        private long id;
        private String experiment;
        private String name;
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

        @JsonGetter("name")
        public String getName() {
            return name;
        }

        @JsonSetter("name")
        public void setName(String name) {
            this.name = name;
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
            inputData.setName(StringUtils.nullToEmpty(name));
            inputData.setExperiment(StringUtils.nullToEmpty(experiment));
            inputData.setTimePoint(StringUtils.nullToEmpty(timePoint));
        }
    }

}
