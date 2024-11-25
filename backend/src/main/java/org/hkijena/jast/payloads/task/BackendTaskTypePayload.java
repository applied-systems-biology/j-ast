package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.tasks.BackendTaskType;
import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.tasks.BackendTaskWorkloadDataSlot;
import org.hkijena.jast.tasks.BackendTaskWorkloadMode;

import java.util.ArrayList;
import java.util.List;

public class BackendTaskTypePayload {
    @JsonProperty
    private String taskId;
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private BackendTaskWorkloadMode workloadMode = BackendTaskWorkloadMode.Single;
    @JsonProperty
    private AssayType assayTypeRestriction = AssayType.Unknown;
    @JsonProperty
    private List<BackendTaskWorkloadDataSlot> inputs = new ArrayList<>();
    @JsonProperty
    private List<BackendTaskWorkloadDataSlot> outputs = new ArrayList<>();

    public BackendTaskTypePayload() {
    }

    public BackendTaskTypePayload(BackendTaskWorkload workload, String taskId) {
        setTaskId(taskId);
        setName(workload.getName());
        setDescription(workload.getDescription());
        setWorkloadMode(workload.getMode());
        setInputs(workload.getInputs());
        setOutputs(workload.getOutputs());
        setAssayTypeRestriction(workload.getAssayTypeRestriction());
    }

    public AssayType getAssayTypeRestriction() {
        return assayTypeRestriction;
    }

    public void setAssayTypeRestriction(AssayType assayTypeRestriction) {
        this.assayTypeRestriction = assayTypeRestriction;
    }

    public BackendTaskWorkloadMode getWorkloadMode() {
        return workloadMode;
    }

    public void setWorkloadMode(BackendTaskWorkloadMode workloadMode) {
        this.workloadMode = workloadMode;
    }

    public List<BackendTaskWorkloadDataSlot> getInputs() {
        return inputs;
    }

    public void setInputs(List<BackendTaskWorkloadDataSlot> inputs) {
        this.inputs = inputs;
    }

    public List<BackendTaskWorkloadDataSlot> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<BackendTaskWorkloadDataSlot> outputs) {
        this.outputs = outputs;
    }

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
