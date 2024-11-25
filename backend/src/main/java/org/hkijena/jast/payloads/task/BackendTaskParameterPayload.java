package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.tasks.BackendTaskWorkloadParameterSlot;
import org.hkijena.jast.tasks.BackendTaskWorkloadParameterSlotType;

public class BackendTaskParameterPayload {
    @JsonProperty
    private BackendTaskWorkloadParameterSlotType type;

    @JsonProperty
    private String id;

    @JsonProperty
    private String label;

    @JsonProperty
    private String description;

    @JsonProperty
    private Object value;

    public BackendTaskParameterPayload() {
    }

    public BackendTaskParameterPayload(BackendTaskWorkloadParameterSlot slot) {
        this.type = slot.getType();
        this.id = slot.getId();
        this.label = slot.getLabel();
        this.description = slot.getDescription();
        this.value = slot.getDefaultValue();
    }

    public BackendTaskWorkloadParameterSlotType getType() {
        return type;
    }

    public void setType(BackendTaskWorkloadParameterSlotType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
