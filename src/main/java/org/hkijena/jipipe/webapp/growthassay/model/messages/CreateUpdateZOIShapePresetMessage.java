package org.hkijena.jipipe.webapp.growthassay.model.messages;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.Column;

public class CreateUpdateZOIShapePresetMessage {
    private long id = -1;
    private String name = "";
    private String description = "";
    private double mcaIntercept = 6.120974538;
    private double mcaRate = -0.008732194;
    private double stripIntercept = 0;
    private double stripRate = -0.008732194;
    private boolean global = false;

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter("mcaIntercept")
    public double getMcaIntercept() {
        return mcaIntercept;
    }

    @JsonSetter("mcaIntercept")
    public void setMcaIntercept(double mcaIntercept) {
        this.mcaIntercept = mcaIntercept;
    }

    @JsonGetter("mcaRate")
    public double getMcaRate() {
        return mcaRate;
    }

    @JsonSetter("mcaRate")
    public void setMcaRate(double mcaRate) {
        this.mcaRate = mcaRate;
    }

    @JsonGetter("stripIntercept")
    public double getStripIntercept() {
        return stripIntercept;
    }

    @JsonSetter("stripIntercept")
    public void setStripIntercept(double stripIntercept) {
        this.stripIntercept = stripIntercept;
    }

    @JsonGetter("stripRate")
    public double getStripRate() {
        return stripRate;
    }

    @JsonSetter("stripRate")
    public void setStripRate(double stripRate) {
        this.stripRate = stripRate;
    }

    @JsonGetter("global")
    public boolean isGlobal() {
        return global;
    }

    @JsonSetter("global")
    public void setGlobal(boolean global) {
        this.global = global;
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
