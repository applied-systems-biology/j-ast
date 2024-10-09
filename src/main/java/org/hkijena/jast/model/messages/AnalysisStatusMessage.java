package org.hkijena.jast.model.messages;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.TimeSeries;

public class AnalysisStatusMessage {
    private TimeSeries.Status status = TimeSeries.Status.Preparing;

    private String log;

    @JsonGetter("status")
    public TimeSeries.Status getStatus() {
        return status;
    }
    @JsonSetter("status")
    public void setStatus(TimeSeries.Status status) {
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
