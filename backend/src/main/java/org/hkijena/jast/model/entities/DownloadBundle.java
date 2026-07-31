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

package org.hkijena.jast.model.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.DownloadBundleStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "download_bundles")
public class DownloadBundle {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Result result;

    @Enumerated(EnumType.STRING)
    private DownloadBundleStatus status = DownloadBundleStatus.Preparing;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path = "/";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "total_size")
    private long totalSize;

    @Column(name = "part_count")
    private int partCount;

    @Column(name = "parts", columnDefinition = "JSON")
    @Type(JsonType.class)
    private List<DownloadBundlePart> parts = new ArrayList<>();

    @Column(name = "progress_percent")
    private int progressPercent;

    @Column(name = "progress_message", columnDefinition = "TEXT")
    private String progressMessage = "";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static class DownloadBundlePart {
        private String fileId;
        private String fileName;
        private long size;

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DownloadBundlePart that = (DownloadBundlePart) o;
            return size == that.size
                    && java.util.Objects.equals(fileId, that.fileId)
                    && java.util.Objects.equals(fileName, that.fileName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(fileId, fileName, size);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public DownloadBundleStatus getStatus() { return status; }
    public void setStatus(DownloadBundleStatus status) { this.status = status; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public List<DownloadBundlePart> getParts() { return parts; }
    public void setParts(List<DownloadBundlePart> parts) { this.parts = parts; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
