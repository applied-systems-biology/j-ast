package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class StripPresetPayload {
    @JsonProperty("id")
    private long id = 0L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("interpolation")
    private StripPresetInterpolation interpolation = StripPresetInterpolation.Linear;

    @JsonProperty("ticks")
    private List<Double> ticks = new ArrayList<>();

    public StripPresetPayload() {
    }

    public StripPresetPayload(StripPresetPayload other) {
        this.id = other.id;
        this.name = other.name;
        this.interpolation = other.interpolation;
        this.ticks = other.ticks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Double> getTicks() {
        return ticks;
    }

    public void setTicks(List<Double> ticks) {
        this.ticks = ticks;
    }

    public StripPresetInterpolation getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(StripPresetInterpolation interpolation) {
        this.interpolation = interpolation;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
