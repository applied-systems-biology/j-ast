package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.tasks.BackendTaskType;
import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.tasks.BackendTaskWorkloadDataSlot;
import org.hkijena.jast.tasks.BackendTaskWorkloadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackendTaskTypePayload {
    @JsonProperty
    private String taskId;
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private String category;
    @JsonProperty
    private BackendTaskWorkloadMode workloadMode = BackendTaskWorkloadMode.Single;
    @JsonProperty
    private AssayType assayTypeRestriction = AssayType.Unknown;
    @JsonProperty
    private List<BackendTaskWorkloadDataSlot> inputs = new ArrayList<>();
    @JsonProperty
    private List<BackendTaskWorkloadDataSlot> outputs = new ArrayList<>();
    @JsonProperty
    private List<BackendTaskParameterPayload> parameters = new ArrayList<>();
    @JsonProperty
    private boolean outputsResult = false;

    public BackendTaskTypePayload() {
    }

    public BackendTaskTypePayload(BackendTaskWorkload workload, String taskId) {
        setTaskId(taskId);
        setName(workload.getName());
        setDescription(workload.getDescription());
        setWorkloadMode(workload.getMode());
        setInputs(workload.getInputs());
        setOutputs(workload.getOutputs());
        setCategory(workload.getCategory());
        setAssayTypeRestriction(workload.getAssayTypeRestriction());
        setParameters(workload.getParameters().stream().map(BackendTaskParameterPayload::new).collect(Collectors.toList()));
        setOutputsResult(workload.isOutputsResult());
    }

    public boolean isOutputsResult() {
        return outputsResult;
    }

    public void setOutputsResult(boolean outputsResult) {
        this.outputsResult = outputsResult;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public List<BackendTaskParameterPayload> getParameters() {
        return parameters;
    }

    public void setParameters(List<BackendTaskParameterPayload> parameters) {
        this.parameters = parameters;
    }
}
