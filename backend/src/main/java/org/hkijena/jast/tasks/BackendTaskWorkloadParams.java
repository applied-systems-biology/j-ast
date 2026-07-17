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
