package org.hkijena.jast.model.algorithms;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FOGAnalysisAlgorithmSettings {
    private String timePointEarly = "";
    private String timePointLate = "";
    private List<Double> growthReductionThresholds = new ArrayList<>(Arrays.asList(20.0, 50.0, 80.0));
    private double contrastMinValue = 50;
    private double contrastMaxValue = 250;

    @JsonGetter("contrast-min-value")
    public double getContrastMinValue() {
        return contrastMinValue;
    }

    @JsonSetter("contrast-min-value")
    public void setContrastMinValue(double contrastMinValue) {
        this.contrastMinValue = contrastMinValue;
    }

    @JsonGetter("contrast-max-value")
    public double getContrastMaxValue() {
        return contrastMaxValue;
    }

    @JsonSetter("contrast-max-value")
    public void setContrastMaxValue(double contrastMaxValue) {
        this.contrastMaxValue = contrastMaxValue;
    }

    @JsonGetter("growth-reduction-thresholds")
    public List<Double> getGrowthReductionThresholds() {
        return growthReductionThresholds;
    }

    @JsonSetter("growth-reduction-thresholds")
    public void setGrowthReductionThresholds(List<Double> growthReductionThresholds) {
        this.growthReductionThresholds = growthReductionThresholds;
    }

    @JsonGetter("time-point-early")
    public String getTimePointEarly() {
        return timePointEarly;
    }

    @JsonSetter("time-point-early")
    public void setTimePointEarly(String timePointEarly) {
        this.timePointEarly = timePointEarly;
    }

    @JsonGetter("time-point-late")
    public String getTimePointLate() {
        return timePointLate;
    }

    @JsonSetter("time-point-late")
    public void setTimePointLate(String timePointLate) {
        this.timePointLate = timePointLate;
    }
}
