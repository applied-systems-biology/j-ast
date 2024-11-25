package org.hkijena.jast.payloads.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.ResultItemType;
import org.hkijena.jast.model.entities.ResultItem;

public class ResultItemPayload {
    @JsonProperty
    private long id;

    @JsonProperty
    private long resultId;

    @JsonProperty
    private long projectId;

    @JsonProperty
    private String name;

    @JsonProperty
    private ResultItemType type;

    @JsonProperty
    private ResultItemType visualizationType;

    public ResultItemPayload() {
    }

    public ResultItemPayload(ResultItem resultItem) {
        this.id = resultItem.getId();
        this.resultId = resultItem.getResult().getId();
        this.projectId = resultItem.getResult().getProject().getId();
        this.name = resultItem.getName();
        this.type = resultItem.getType();
        this.visualizationType = resultItem.getVisualizationType();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    public ResultItemType getType() {
        return type;
    }

    public void setType(ResultItemType type) {
        this.type = type;
    }

    public ResultItemType getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(ResultItemType visualizationType) {
        this.visualizationType = visualizationType;
    }
}
