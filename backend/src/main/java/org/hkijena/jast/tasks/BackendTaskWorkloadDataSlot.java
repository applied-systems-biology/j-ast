package org.hkijena.jast.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendTaskWorkloadDataSlot {
    @JsonProperty
    private BackendTaskWorkloadDataSlotType type;

    @JsonProperty
    private BackendTaskWorkloadDataSlotValidationMode validationMode;

    @JsonProperty
    private String name;

    public BackendTaskWorkloadDataSlot() {
    }

    public BackendTaskWorkloadDataSlot(String name, BackendTaskWorkloadDataSlotType type) {
        this.name = name;
        this.type = type;
        this.validationMode = BackendTaskWorkloadDataSlotValidationMode.Always;
    }

    public BackendTaskWorkloadDataSlot(String name, BackendTaskWorkloadDataSlotType type, BackendTaskWorkloadDataSlotValidationMode validationMode) {
        this.name = name;
        this.type = type;
        this.validationMode = validationMode;
    }

    public BackendTaskWorkloadDataSlotValidationMode getValidationMode() {
        return validationMode;
    }

    public void setValidationMode(BackendTaskWorkloadDataSlotValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(BackendTaskWorkloadDataSlotType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public BackendTaskWorkloadDataSlotType getType() {
        return type;
    }
}
