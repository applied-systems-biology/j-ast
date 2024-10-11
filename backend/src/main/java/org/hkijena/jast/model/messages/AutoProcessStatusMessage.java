package org.hkijena.jast.model.messages;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.AutoProcessStatus;

public class AutoProcessStatusMessage {
    private AutoProcessStatus status = AutoProcessStatus.Running;

    private String log;

    @JsonGetter("status")
    public AutoProcessStatus getStatus() {
        return status;
    }
    @JsonSetter("status")
    public void setStatus(AutoProcessStatus status) {
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
