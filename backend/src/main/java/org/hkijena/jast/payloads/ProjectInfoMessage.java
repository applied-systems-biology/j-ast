package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ProjectInfoMessage {
    private int id;
    private String name;
    private String owner;

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
    public int getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(int id) {
        this.id = id;
    }
}
