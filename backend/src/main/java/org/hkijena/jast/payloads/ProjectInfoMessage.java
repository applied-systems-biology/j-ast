package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.Project;

public class ProjectInfoMessage {
    private long id;
    private String name;
    private String owner;

    public ProjectInfoMessage() {

    }

    public ProjectInfoMessage(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.owner = project.getOwner() != null ? project.getOwner().getEmail() : "";
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The username of the owner
     * @return the username
     */
    @JsonGetter("owner")
    public String getOwner() {
        return owner;
    }

    @JsonSetter("owner")
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonGetter("id")
    public long getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(long id) {
        this.id = id;
    }
}
