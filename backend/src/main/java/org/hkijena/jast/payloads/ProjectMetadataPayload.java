package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.Project;

public record ProjectMetadataPayload(
        @JsonProperty long id,
        @JsonProperty String name,
        @JsonProperty String owner) {
    public static ProjectMetadataPayload create(Project project) {
        return new ProjectMetadataPayload(project.getId(), project.getName(), project.getOwner() != null ? project.getOwner().getEmail() : "");
    }
}
