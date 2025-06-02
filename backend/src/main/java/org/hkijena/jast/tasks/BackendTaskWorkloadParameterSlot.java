package org.hkijena.jast.tasks;

public class BackendTaskWorkloadParameterSlot {
    private BackendTaskWorkloadParameterSlotType type;
    private String id;
    private String label;
    private String description;
    private Object defaultValue;

    public BackendTaskWorkloadParameterSlot() {
    }

    public BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType type, String id, String label, String description, Object defaultValue) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.description = description;
        this.defaultValue = defaultValue;
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

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return "BackendTaskWorkloadParameterSlot{" +
                "type=" + type +
                ", id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }
}
