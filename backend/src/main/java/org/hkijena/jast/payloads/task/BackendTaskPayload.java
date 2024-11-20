package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.model.entities.BackendTask;
import org.hkijena.jast.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class BackendTaskPayload {
    @JsonProperty
    private long id = -1;

    @JsonProperty
    private List<Long> imageIds = new ArrayList<>();

    @JsonProperty
    private String taskId;

    @JsonProperty
    private long projectId;

    @JsonProperty
    private TaskStatus status = TaskStatus.Ready;

    public BackendTaskPayload() {

    }

    public BackendTaskPayload(BackendTask backendTask) {
        // Read payload data
        try {
            JsonUtils.getObjectMapper().readerForUpdating(this).readValue(backendTask.getPayload());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Take ID and status from the DB
        this.id = backendTask.getId();
        this.status = backendTask.getStatus();
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Long> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<Long> imageIds) {
        this.imageIds = imageIds;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }
}
