package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class ProjectInfoMessage {
    private String projectName;
    private String projectOwner;

    @JsonGetter("name")
    public String getProjectName() {
        return projectName;
    }

    @JsonSetter("name")
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @JsonGetter("owner")
    public String getProjectOwner() {
        return projectOwner;
    }

    @JsonSetter("owner")
    public void setProjectOwner(String projectOwner) {
        this.projectOwner = projectOwner;
    }
}
