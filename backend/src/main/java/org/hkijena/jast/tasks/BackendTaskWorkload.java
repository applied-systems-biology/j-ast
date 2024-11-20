package org.hkijena.jast.tasks;

import org.hkijena.jast.payloads.task.BackendTaskPayload;

public interface BackendTaskWorkload {
    String getName();
    String getDescription();

    void execute(BackendTaskPayload payload) throws Throwable;
}
