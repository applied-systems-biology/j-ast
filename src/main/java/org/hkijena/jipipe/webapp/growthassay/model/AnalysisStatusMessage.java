package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class AnalysisStatusMessage {
    private Dataset.Status status = Dataset.Status.Preparing;

    @JsonGetter("status")
    public Dataset.Status getStatus() {
        return status;
    }
    @JsonSetter("status")
    public void setStatus(Dataset.Status status) {
        this.status = status;
    }
}
