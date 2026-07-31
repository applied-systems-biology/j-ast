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
import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;

import java.util.ArrayList;
import java.util.List;

public class DownloadBundlePayload {
    @JsonProperty
    private String id;

    @JsonProperty
    private DownloadBundleStatus status;

    @JsonProperty
    private int progressPercent;

    @JsonProperty
    private String progressMessage;

    @JsonProperty
    private long totalSize;

    @JsonProperty
    private int partCount;

    @JsonProperty
    private List<DownloadBundlePartPayload> parts = new ArrayList<>();

    @JsonProperty
    private String errorMessage;

    public DownloadBundlePayload() {
    }

    public DownloadBundlePayload(DownloadBundle bundle) {
        this.id = bundle.getId();
        this.status = bundle.getStatus();
        this.progressPercent = bundle.getProgressPercent();
        this.progressMessage = bundle.getProgressMessage();
        this.totalSize = bundle.getTotalSize();
        this.partCount = bundle.getPartCount();
        this.errorMessage = bundle.getErrorMessage();
        for (DownloadBundle.DownloadBundlePart part : bundle.getParts()) {
            this.parts.add(new DownloadBundlePartPayload(part));
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public DownloadBundleStatus getStatus() { return status; }
    public void setStatus(DownloadBundleStatus status) { this.status = status; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public List<DownloadBundlePartPayload> getParts() { return parts; }
    public void setParts(List<DownloadBundlePartPayload> parts) { this.parts = parts; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
