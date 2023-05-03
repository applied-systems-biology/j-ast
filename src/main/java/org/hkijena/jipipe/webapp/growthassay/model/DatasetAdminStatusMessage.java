package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class DatasetAdminStatusMessage {

    private long id;
    private String owner;
    private String name;
    private String status;

    private boolean canCancel;

    @JsonGetter("can-cancel")
    public boolean isCanCancel() {
        return canCancel;
    }

    @JsonSetter("can-cancel")
    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }

    @JsonGetter("id")
    public long getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(long id) {
        this.id = id;
    }

    @JsonGetter("owner")
    public String getOwner() {
        return owner;
    }

    @JsonSetter("owner")
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("status")
    public String getStatus() {
        return status;
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = status;
    }
}
