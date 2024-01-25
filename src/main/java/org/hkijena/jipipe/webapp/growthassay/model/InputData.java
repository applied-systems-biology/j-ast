package org.hkijena.jipipe.webapp.growthassay.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hkijena.jipipe.webapp.growthassay.utils.PathUtils;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;

import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;

@Entity
@Table(name = "input_data")
public class InputData {
    @Serial
    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    @NotNull
    private String storagePath;

    @Column(name = "thumbnail_storage_path", columnDefinition = "TEXT")
    @NotNull
    private String thumbnailStoragePath = "";

    @Column(name = "experiment", columnDefinition = "TEXT")
    @NotNull
    private String experiment;

    @Column(name = "sample", columnDefinition = "TEXT")
    @NotNull
    private String sample;

    @Column(name = "timepoint", columnDefinition = "TEXT")
    @NotNull
    private String timePoint;

    @Column(name = "original_file_name", columnDefinition = "TEXT")
    @NotNull
    private String originalFileName;

    @Column(name = "image_width")
    @NotNull
    private int imageWidth;

    @Column(name = "image_height")
    @NotNull
    private int imageHeight;

    @Column(name = "plate_diameter_mm")
    @NotNull
    private double plateDiameter = 90;

    @Column(name = "assay_type")
    @NotNull
    @Enumerated(EnumType.STRING)
    private AssayType assayType = AssayType.DDA;

    @Column(name = "etest_zoi_shape_preset_name", columnDefinition = "TEXT")
    @ColumnDefault("Default")
    private String eTestZOIShapePresetName = "Default";

    @Column(name = "etest_zoi_shape_mca_intercept", columnDefinition = "DOUBLE")
    @NotNull
    @ColumnDefault("6.120974538")
    private double eTestZOIShapePresetMcaIntercept = 6.120974538;

    @Column(name = "etest_zoi_shape_mca_rate", columnDefinition = "DOUBLE")
    @NotNull
    @ColumnDefault("-0.008732194")
    private double eTestZOIShapePresetMcaRate = -0.008732194;

    @Column(name = "etest_zoi_shape_strip_intercept", columnDefinition = "DOUBLE")
    @NotNull
    @ColumnDefault("0")
    private double eTestZOIShapePresetStripIntercept = 0;

    @Column(name = "etest_zoi_shape_strip_rate", columnDefinition = "DOUBLE")
    @NotNull
    @ColumnDefault("-0.008732194")
    private double eTestZOIShapePresetStripRate = -0.008732194;

    @ManyToOne(fetch = FetchType.LAZY)
    private Dataset dataset;

    public String geteTestZOIShapePresetName() {
        return eTestZOIShapePresetName;
    }

    public void seteTestZOIShapePresetName(String eTestZOIShapePresetName) {
        this.eTestZOIShapePresetName = eTestZOIShapePresetName;
    }

    public double geteTestZOIShapePresetMcaIntercept() {
        return eTestZOIShapePresetMcaIntercept;
    }

    public void seteTestZOIShapePresetMcaIntercept(double eTestZOIShapePresetMcaIntercept) {
        this.eTestZOIShapePresetMcaIntercept = eTestZOIShapePresetMcaIntercept;
    }

    public double geteTestZOIShapePresetMcaRate() {
        return eTestZOIShapePresetMcaRate;
    }

    public void seteTestZOIShapePresetMcaRate(double eTestZOIShapePresetMcaRate) {
        this.eTestZOIShapePresetMcaRate = eTestZOIShapePresetMcaRate;
    }

    public double geteTestZOIShapePresetStripIntercept() {
        return eTestZOIShapePresetStripIntercept;
    }

    public void seteTestZOIShapePresetStripIntercept(double eTestZOIShapePresetStripIntercept) {
        this.eTestZOIShapePresetStripIntercept = eTestZOIShapePresetStripIntercept;
    }

    public double geteTestZOIShapePresetStripRate() {
        return eTestZOIShapePresetStripRate;
    }

    public void seteTestZOIShapePresetStripRate(double eTestZOIShapePresetStripRate) {
        this.eTestZOIShapePresetStripRate = eTestZOIShapePresetStripRate;
    }

    public AssayType getAssayType() {
        return assayType;
    }

    public void setAssayType(AssayType assayType) {
        this.assayType = assayType;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getThumbnailStoragePath() {
        return thumbnailStoragePath;
    }

    public void setThumbnailStoragePath(String thumbnailStoragePath) {
        this.thumbnailStoragePath = thumbnailStoragePath;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
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

    public String getSample() {
        return sample;
    }

    public void setSample(String name) {
        this.sample = name;
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

    public double getPlateDiameter() {
        return plateDiameter;
    }

    public void setPlateDiameter(double plateDiameter) {
        this.plateDiameter = plateDiameter;
    }

    public String getFinalFileName() {
        return getAssayType() + "_" + getExperiment() + "_" + getSample() + "_" + getTimePoint() + "_" + getPlateDiameter();
    }

    public void tryAutoFill(String originalFileName) {
        originalFileName = originalFileName.trim();
        if(originalFileName.toLowerCase().endsWith(".png")) {
            originalFileName = originalFileName.substring(0, originalFileName.length() - 4);
        }
        while(originalFileName.contains("__")) {
            originalFileName = originalFileName.replace("__", "_");
        }
        String[] components = originalFileName.split("_");

        if(components.length >= 4) {
            if(components[0].toLowerCase().startsWith("d")) {
                setAssayType(AssayType.DDA);
            }
            else {
                setAssayType(AssayType.ETest);
            }
            setExperiment(components[1]);
            setSample(components[2]);
            setTimePoint(components[3]);
            if(components.length > 4) {
                if(NumberUtils.isCreatable(components[4])) {
                    setPlateDiameter(NumberUtils.createDouble(components[4]));
                }
            }
        }
        else if(components.length >= 3) {
            setExperiment(components[0]);
            setSample(components[1]);
            setTimePoint(components[2]);
            if(components.length > 3) {
                if(NumberUtils.isCreatable(components[3])) {
                    setPlateDiameter(NumberUtils.createDouble(components[3]));
                }
            }
        }
        else if(components.length == 2) {
            setExperiment("Experiment");
            setSample(components[0]);
            setTimePoint(components[1]);
        }
        else {
            setExperiment("");
            setSample("");
            setTimePoint("");
        }
    }

    public void deleteStorage(Path parentStoragePath) {
        if(!StringUtils.isNullOrEmpty(storagePath)) {
            PathUtils.trySafeDeleteFile(Paths.get(storagePath), parentStoragePath);
        }
        if(!StringUtils.isNullOrEmpty(thumbnailStoragePath)) {
            PathUtils.trySafeDeleteFile(Paths.get(thumbnailStoragePath), parentStoragePath);
        }
    }
}
