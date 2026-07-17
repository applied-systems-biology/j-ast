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

package org.hkijena.jast.payloads.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.tasks.BackendTaskWorkloadParameterSlot;
import org.hkijena.jast.tasks.BackendTaskWorkloadParameterSlotDataType;
import org.hkijena.jast.tasks.BackendTaskWorkloadParameterSlotType;

public class BackendTaskParameterPayload {
    @JsonProperty
    private BackendTaskWorkloadParameterSlotDataType dataType;

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
        this.dataType = slot.getDataType();
        this.type = slot.getType();
        this.id = slot.getId();
        this.label = slot.getLabel();
        this.description = slot.getDescription();
        this.value = slot.getDefaultValue();
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
