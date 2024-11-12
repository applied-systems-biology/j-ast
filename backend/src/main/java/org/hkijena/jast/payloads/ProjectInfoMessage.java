package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.Project;

public record ProjectInfoMessage(
        @JsonProperty long id,
        @JsonProperty String name,
        @JsonProperty String owner) {
    public static ProjectInfoMessage create(Project project) {
        return new ProjectInfoMessage(project.getId(), project.getName(), project.getOwner() != null ? project.getOwner().getEmail() : "");
    }
}
