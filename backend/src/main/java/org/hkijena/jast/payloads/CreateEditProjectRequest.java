package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.ViewMode;

public class CreateEditProjectRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("viewMode")
    private ViewMode viewMode = ViewMode.Timeline;

    public CreateEditProjectRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
    }
}
