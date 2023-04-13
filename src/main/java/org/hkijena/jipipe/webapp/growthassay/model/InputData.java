package org.hkijena.jipipe.webapp.growthassay.model;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "input_data")
public class InputData {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "thumbnail_storage_path", nullable = false, columnDefinition = "TEXT")
    private String thumbnailStoragePath = "";

    @Column(name = "experiment", nullable = false, columnDefinition = "TEXT")
    private String experiment;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "timepoint", nullable = false, columnDefinition = "TEXT")
    private String timePoint;

    @Column(name = "original_file_name", nullable = false, columnDefinition = "TEXT")
    private String originalFileName;

    public String getThumbnailStoragePath() {
        return thumbnailStoragePath;
    }

    public void setThumbnailStoragePath(String thumbnailStoragePath) {
        this.thumbnailStoragePath = thumbnailStoragePath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void tryAutoFill(String originalFileName) {
        originalFileName = originalFileName.trim();
        if(originalFileName.toLowerCase().endsWith(".png")) {
            originalFileName = originalFileName.substring(originalFileName.length() - 5);
        }
        while(originalFileName.contains("__")) {
            originalFileName = originalFileName.replace("__", "_");
        }
        String[] components = originalFileName.split("_");
        if(components.length == 3) {
            setExperiment(components[0]);
            setName(components[1]);
            setTimePoint(components[2]);
        }
        else {
            setExperiment("");
            setName("");
            setTimePoint("");
        }
    }
}
