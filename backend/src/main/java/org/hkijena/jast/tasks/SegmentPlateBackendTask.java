package org.hkijena.jast.tasks;

@BackendTaskType(typeId = "image-segment-plate")
public class SegmentPlateBackendTask implements BackendTask {
    @Override
    public String getName() {
        return "Auto-detect plate";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the plate for the selected images";
    }
}
