package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.model.entities.BackendTask;
import org.hkijena.jast.utils.JsonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BackendTaskPayload {
    @JsonProperty
    private long id = -1;

    @JsonProperty
    private List<Long> imageIds = new ArrayList<>();

    @JsonProperty
    private String taskId;

    @JsonProperty
    private String name;

    @JsonProperty
    private long projectId;

    @JsonProperty
    private TaskStatus status = TaskStatus.Ready;

    @JsonProperty
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty
    private List<BackendTaskParameterPayload> parameters = new ArrayList<>();


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
        this.createdAt = backendTask.getCreatedAt();
        this.name = backendTask.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public List<BackendTaskParameterPayload> getParameters() {
        return parameters;
    }

    public void setParameters(List<BackendTaskParameterPayload> parameters) {
        this.parameters = parameters;
    }

    public BackendTaskParameterPayload getParameter(String key) {
        for (BackendTaskParameterPayload parameter : parameters) {
            if(Objects.equals(parameter.getId(), key)) {
                return parameter;
            }
        }
        return null;
    }
}
