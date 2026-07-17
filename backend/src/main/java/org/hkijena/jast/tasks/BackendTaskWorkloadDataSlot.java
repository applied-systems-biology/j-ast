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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BackendTaskWorkloadDataSlotType getType() {
        return type;
    }

    public void setType(BackendTaskWorkloadDataSlotType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "BackendTaskWorkloadDataSlot{" +
                "type=" + type +
                ", validationMode=" + validationMode +
                ", name='" + name + '\'' +
                '}';
    }
}
