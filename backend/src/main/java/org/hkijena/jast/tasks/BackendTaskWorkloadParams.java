package org.hkijena.jast.tasks;

import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.payloads.task.BackendTaskPayload;

import java.nio.file.Path;

public class BackendTaskWorkloadParams {
    private BackendTaskPayload payload;
    private Path tmpPath;
    private Path lockFilePath;
    private RuntimeConfig runtimeConfig;

    public Path getLockFilePath() {
        return lockFilePath;
    }

    public void setLockFilePath(Path lockFilePath) {
        this.lockFilePath = lockFilePath;
    }

    public RuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(RuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public BackendTaskPayload getPayload() {
        return payload;
    }

    public void setPayload(BackendTaskPayload payload) {
        this.payload = payload;
    }

    public Path getTmpPath() {
        return tmpPath;
    }

    public void setTmpPath(Path tmpPath) {
        this.tmpPath = tmpPath;
    }
}
