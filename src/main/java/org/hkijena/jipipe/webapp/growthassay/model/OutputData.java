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

    @OneToMany(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "input_data_id")
    private List<InputData> inputData = new ArrayList<>();

    @Column(name = "fog")
    private double fog;

    @Column(name = "visualization_storage_path", nullable = false, columnDefinition = "TEXT")
    private String visualizationStoragePath;

    @Column(name = "visualization_thumbnail_storage_path", nullable = false, columnDefinition = "TEXT")
    private String visualizationThumbnailStoragePath = "";

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

    public double getFog() {
        return fog;
    }

    public void setFog(double fog) {
        this.fog = fog;
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
