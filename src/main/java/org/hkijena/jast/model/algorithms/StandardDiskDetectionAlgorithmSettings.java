package org.hkijena.jast.model.algorithms;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;

public class StandardDiskDetectionAlgorithmSettings {
    private double minDiameter = 3;
    private double maxDiameter = 13;
    private double minCircularity = 0.5;
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

    @JsonGetter("min-diameter")
    public double getMinDiameter() {
        return minDiameter;
    }

    @JsonSetter("min-diameter")
    public void setMinDiameter(double minDiameter) {
        this.minDiameter = minDiameter;
    }

    @JsonGetter("max-diameter")
    public double getMaxDiameter() {
        return maxDiameter;
    }

    @JsonSetter("max-diameter")
    public void setMaxDiameter(double maxDiameter) {
        this.maxDiameter = maxDiameter;
    }

    @JsonGetter("min-circularity")
    public double getMinCircularity() {
        return minCircularity;
    }

    @JsonSetter("min-circularity")
    public void setMinCircularity(double minCircularity) {
        this.minCircularity = minCircularity;
    }
}
