package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
import org.hkijena.jast.utils.JsonUtils;

import java.io.Serial;
import java.time.LocalDateTime;

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

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.Ready;

    @Column(name = "name")
    private String name = "Unnamed";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "tmp_path")
    private String tmpPath;

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

    public BackendTaskPayload toPayload() {
        BackendTaskPayload instance = JsonUtils.readFromString(payload, BackendTaskPayload.class);
        instance.setId(id);
        instance.setStatus(status);
        instance.setCreatedAt(getCreatedAt());
        instance.setName(name);
        return instance;
    }
}
