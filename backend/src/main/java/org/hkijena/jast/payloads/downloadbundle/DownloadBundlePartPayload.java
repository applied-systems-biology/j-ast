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

package org.hkijena.jast.payloads.downloadbundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.DownloadBundle.DownloadBundlePart;

public class DownloadBundlePartPayload {
    @JsonProperty
    private String fileName;

    @JsonProperty
    private long size;

    public DownloadBundlePartPayload() {
    }

    public DownloadBundlePartPayload(DownloadBundlePart part) {
        this.fileName = part.getFileName();
        this.size = part.getSize();
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
