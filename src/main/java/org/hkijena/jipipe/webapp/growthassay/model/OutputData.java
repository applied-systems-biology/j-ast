package org.hkijena.jipipe.webapp.growthassay.model;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "output_data")
public class OutputData {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Dataset dataset;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<InputData> inputData = new ArrayList<>();

    @Column(name = "experiment", nullable = false, columnDefinition = "TEXT")
    private String experiment;

    @Column(name = "sample", nullable = false, columnDefinition = "TEXT")
    private String sample;

    @Column(name = "threshold")
    private double threshold;

    @Column(name = "fog")
    private double fog;

    @Column(name = "rad")
    private double rad;

    @Column(name = "visualization_storage_path", nullable = false, columnDefinition = "TEXT")
    private String visualizationStoragePath;

    @Column(name = "visualization_thumbnail_storage_path", nullable = false, columnDefinition = "TEXT")
    private String visualizationThumbnailStoragePath = "";

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getVisualizationStoragePath() {
        return visualizationStoragePath;
    }

    public void setVisualizationStoragePath(String visualizationStoragePath) {
        this.visualizationStoragePath = visualizationStoragePath;
    }

    public String getVisualizationThumbnailStoragePath() {
        return visualizationThumbnailStoragePath;
    }

    public void setVisualizationThumbnailStoragePath(String visualizationThumbnailStoragePath) {
        this.visualizationThumbnailStoragePath = visualizationThumbnailStoragePath;
    }

    public List<InputData> getInputData() {
        return inputData;
    }

    public void setInputData(List<InputData> inputData) {
        this.inputData = inputData;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public double getFog() {
        return fog;
    }

    public void setFog(double fog) {
        this.fog = fog;
    }

    public double getRad() {
        return rad;
    }

    public void setRad(double rad) {
        this.rad = rad;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
