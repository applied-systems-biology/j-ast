package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class AnalysisStatusMessage {
    private Dataset.Status status = Dataset.Status.Preparing;

    private String log;

    @JsonGetter("status")
    public Dataset.Status getStatus() {
        return status;
    }
    @JsonSetter("status")
    public void setStatus(Dataset.Status status) {
        this.status = status;
    }

    @JsonGetter("log")
    public String getLog() {
        return log;
    }
    @JsonSetter("log")
    public void setLog(String log) {
        this.log = log;
    }
}
