package org.hkijena.jast.tasks;

import org.hkijena.jast.payloads.task.BackendTaskPayload;

@BackendTaskType(typeId = "image-segment-plate")
public class SegmentPlateBackendTaskWorkload implements BackendTaskWorkload {
    @Override
    public String getName() {
        return "Auto-detect plate";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the plate for the selected images";
    }

    @Override
    public void execute(BackendTaskPayload payload) throws Throwable {
        System.out.println("Start task");
        Thread.sleep(10000);
        System.out.println("End task");
    }
}
