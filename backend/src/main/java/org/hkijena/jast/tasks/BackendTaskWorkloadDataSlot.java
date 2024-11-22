package org.hkijena.jast.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendTaskWorkloadDataSlot {
    @JsonProperty
    private BackendTaskWorkloadDataSlotType type;

    @JsonProperty
    private String name;

    public BackendTaskWorkloadDataSlot() {
    }

    public BackendTaskWorkloadDataSlot(String name, BackendTaskWorkloadDataSlotType type) {
        this.name = name;
        this.type = type;
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
