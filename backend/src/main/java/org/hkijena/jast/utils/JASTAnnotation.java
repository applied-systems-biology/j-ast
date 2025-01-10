package org.hkijena.jast.utils;

import org.hkijena.jast.tasks.BackendTaskWorkloadDataSlot;
import org.hkijena.jast.tasks.BackendTaskWorkloadDataSlotType;
import org.hkijena.jast.tasks.BackendTaskWorkloadDataSlotValidationMode;

public enum JASTAnnotation {
    Plate("plate", BackendTaskWorkloadDataSlotType.ImageMaskAnnotation),
    StripDisk("strip-disk", BackendTaskWorkloadDataSlotType.ImageMaskAnnotation),
    ZOIShape("zoi-shape", BackendTaskWorkloadDataSlotType.ImageMaskAnnotation);

    private final String key;
    private final BackendTaskWorkloadDataSlotType slotType;

    JASTAnnotation(String key, BackendTaskWorkloadDataSlotType slotType) {
        this.key = key;
        this.slotType = slotType;
    }

    public BackendTaskWorkloadDataSlot toSlot() {
        return new BackendTaskWorkloadDataSlot(key, slotType);
    }

    public BackendTaskWorkloadDataSlot toSlot(BackendTaskWorkloadDataSlotValidationMode mode) {
        return new BackendTaskWorkloadDataSlot(key, slotType, mode);
    }

    public String getKey() {
        return key;
    }

    public BackendTaskWorkloadDataSlotType getSlotType() {
        return slotType;
    }
}
