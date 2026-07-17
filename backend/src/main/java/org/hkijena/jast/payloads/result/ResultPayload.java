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

package org.hkijena.jast.payloads.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.Result;

public class ResultPayload {
    @JsonProperty
    private long id = -1;

    @JsonProperty
    private long projectId = -1;

    @JsonProperty
    private String name = "";

    @JsonProperty
    private String description = "";

    @JsonProperty
    private String createdAt = "";

    @JsonProperty
    private boolean viewed = false;

    public ResultPayload() {
    }

    public ResultPayload(Result result) {
        this.id = result.getId();
        this.projectId = result.getProject().getId();
        this.name = result.getName();
        this.description = result.getDescription();
        this.createdAt = result.getCreatedAt().toString();
        this.viewed = result.isViewed();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }
}
