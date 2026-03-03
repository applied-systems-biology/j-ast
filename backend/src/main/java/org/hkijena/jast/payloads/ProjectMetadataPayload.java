package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Project;

import java.time.LocalDateTime;
import java.util.Objects;

public final class ProjectMetadataPayload {
    @JsonProperty
    private long id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String owner;

    @JsonProperty
    private ViewMode viewMode;

    @JsonProperty
    private LocalDateTime updatedAt;

    @JsonProperty
    private LocalDateTime createdAt;

    public ProjectMetadataPayload() {
    }

    public ProjectMetadataPayload(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.owner =  project.getOwner() != null ? project.getOwner().getEmail() : "";
        this.viewMode = project.getViewMode();
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ProjectMetadataPayload[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "owner=" + owner + ", " +
                "viewMode=" + viewMode + ", " +
                "createdAt=" + createdAt + ", " +
                "updatedAt=" + updatedAt + ']';
    }

}
