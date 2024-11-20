package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendTaskTypePayload {
    @JsonProperty
    private String taskId;
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
