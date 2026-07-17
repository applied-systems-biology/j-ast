/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.tasks;

public class BackendTaskWorkloadParameterSlot {
    private BackendTaskWorkloadParameterSlotDataType dataType;
    private String id;
    private String label;
    private String description;
    private Object defaultValue;
    private BackendTaskWorkloadParameterSlotType type;

    public BackendTaskWorkloadParameterSlot() {
    }

    public BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType dataType, BackendTaskWorkloadParameterSlotType type, String id, String label, String description, Object defaultValue) {
        this.dataType = dataType;
        this.type = type;
        this.id = id;
        this.label = label;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public BackendTaskWorkloadParameterSlotDataType getDataType() {
        return dataType;
    }

    public void setDataType(BackendTaskWorkloadParameterSlotDataType dataType) {
        this.dataType = dataType;
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
                "dataType=" + dataType +
                ", id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }

    public BackendTaskWorkloadParameterSlotType getType() {
        return type;
    }

    public void setType(BackendTaskWorkloadParameterSlotType type) {
        this.type = type;
    }
}
