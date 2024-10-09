package org.hkijena.jast.model.algorithms;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class StandardPlateDetectionAlgorithmSettings {
    private boolean ensureCircularPlate = true;
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

    @JsonGetter("ensure-circular-plate")
    public boolean isEnsureCircularPlate() {
        return ensureCircularPlate;
    }

    @JsonSetter("ensure-circular-plate")
    public void setEnsureCircularPlate(boolean ensureCircularPlate) {
        this.ensureCircularPlate = ensureCircularPlate;
    }
}
