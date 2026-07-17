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

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.CreationTimestamp;
import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.utils.JsonUtils;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(name = "backend_tasks")
public class BackendTask {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "task_type_id", columnDefinition = "TEXT")
    private String taskTypeId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "log_file_id", columnDefinition = "TEXT")
    private String logFileId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.Ready;

    @Column(name = "name")
    private String name = "Unnamed";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "tmp_path")
    private String tmpPath;

    @Column(name = "job_id")
    private String jobId = "";

    public String getTmpPath() {
        return tmpPath;
    }

    public void setTmpPath(String tmpPath) {
        this.tmpPath = tmpPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTaskTypeId() {
        return taskTypeId;
    }

    public void setTaskTypeId(String taskTypeId) {
        this.taskTypeId = taskTypeId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isRunning() {
        return status == TaskStatus.Running || status == TaskStatus.Ready;
    }

    public String getLogFileId() {
        return logFileId;
    }

    public void setLogFileId(String logFile) {
        this.logFileId = logFile;
    }

    public BackendTaskPayload toPayload() {
        BackendTaskPayload instance = JsonUtils.readFromString(payload, BackendTaskPayload.class);
        instance.setId(id);
        instance.setStatus(status);
        instance.setCreatedAt(getCreatedAt());
        instance.setName(name);
        instance.setParameters(new ArrayList<>(instance.getParameters()));
        return instance;
    }

    public void setLog(FileStorageService fileStorageService, String log) {
        logFileId = fileStorageService.store(log);
    }

    @Transactional
    public void removeFilesLater(FileStorageService fileStorageService) {
        fileStorageService.deleteLater(logFileId);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
