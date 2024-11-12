package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hkijena.jast.model.AssayType;

import java.io.Serial;

@Entity
@Table(name = "images")
public class Image {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "experiment", columnDefinition = "TEXT")
    @NotNull
    private String experiment = "";

    @Column(name = "sample", columnDefinition = "TEXT")
    @NotNull
    private String sample = "";

    @Column(name = "timepoint", columnDefinition = "TEXT")
    @NotNull
    private String timePoint = "";

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
    private AssayType assayType = AssayType.Unknown;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "group_row")
    @NotNull
    private int groupRow = -1;

    @Column(name = "group_column")
    @NotNull
    private int groupColumn = -1;

    @Lob
    @Column(name = "raw_data", columnDefinition = "BLOB")
    private byte[] rawData;

    @Lob
    @Column(name = "thumbnail_data", columnDefinition = "BLOB")
    private byte[] thumbnailData;

    @NotNull
    public int getGroupColumn() {
        return groupColumn;
    }

    public void setGroupColumn(@NotNull int groupColumn) {
        this.groupColumn = groupColumn;
    }

    @NotNull
    public int getGroupRow() {
        return groupRow;
    }

    public void setGroupRow(@NotNull int groupRow) {
        this.groupRow = groupRow;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    public @NotNull AssayType getAssayType() {
        return assayType;
    }

    public void setAssayType(@NotNull AssayType assayType) {
        this.assayType = assayType;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
