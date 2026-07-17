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
import org.hkijena.jast.model.ResultItemType;
import org.hkijena.jast.model.entities.ResultItem;

public class ResultItemPayload {
    @JsonProperty
    private long id;

    @JsonProperty
    private long resultId;

    @JsonProperty
    private long projectId;

    @JsonProperty
    private String name;

    @JsonProperty
    private String path;

    @JsonProperty
    private ResultItemType type;

    @JsonProperty
    private ResultItemType visualizationType;

    @JsonProperty
    private long size = 0;

    public ResultItemPayload() {
    }

    public ResultItemPayload(ResultItem resultItem) {
        this.id = resultItem.getId();
        this.resultId = resultItem.getResult().getId();
        this.projectId = resultItem.getResult().getProject().getId();
        this.name = resultItem.getName();
        this.path = resultItem.getPath();
        this.type = resultItem.getType();
        this.visualizationType = resultItem.getVisualizationType();
        this.size = resultItem.getRawDataFileSize();
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    public ResultItemType getType() {
        return type;
    }

    public void setType(ResultItemType type) {
        this.type = type;
    }

    public ResultItemType getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(ResultItemType visualizationType) {
        this.visualizationType = visualizationType;
    }
}
