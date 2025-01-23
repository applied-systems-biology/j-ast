package org.hkijena.jast.model.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.utils.ColorUtils;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.StringUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An image dataset
 */
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
    private String experiment = "";

    @Column(name = "sample", columnDefinition = "TEXT")
    private String sample = "";

    @Column(name = "timepoint", columnDefinition = "TEXT")
    private String timePoint = "";

    @Column(name = "original_file_name", columnDefinition = "TEXT")
    private String originalFileName;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "assay_type")
    @Enumerated(EnumType.STRING)
    private AssayType assayType = AssayType.Unknown;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "group_row")
    private Integer groupRow = -1;

    @Column(name = "group_column")
    private Integer groupColumn = -1;


    @Column(name = "raw_data", columnDefinition = "BLOB")
    @Lob
    private byte[] rawData;


    @Column(name = "thumbnail_data", columnDefinition = "BLOB")
    @Lob
    private byte[] thumbnailData;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "pixel_size_mm")
    private Double pixelSizeMillimeter = 0.144;

    @Column(name = "metadata", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "image")
    private List<MaskImageAnnotation> maskImageAnnotations = new ArrayList<>();

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public int getVersion() {
        return version != null ? version : 1;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void incrementVersion() {
        setVersion(getVersion() + 1);
    }

    public List<MaskImageAnnotation> getMaskImageAnnotations() {
        return maskImageAnnotations;
    }

    public List<MaskImageAnnotation> getFilteredMaskImageAnnotations() {
        Map<String, MaskImageAnnotation> result = new HashMap<>();
        for (MaskImageAnnotation annotation : maskImageAnnotations) {
            if (!StringUtils.isNullOrEmpty(annotation.getType()) && !result.containsKey(annotation.getType())) {
                result.put(annotation.getType(), annotation);
            }
        }
        return new ArrayList<>(result.values());
    }

    public void addMaskImageAnnotation(MaskImageAnnotation maskImageAnnotation) {
        maskImageAnnotations.add(maskImageAnnotation);
        maskImageAnnotation.setImage(this);
    }

    public void removeMaskImageAnnotation(MaskImageAnnotation maskImageAnnotation) {
        maskImageAnnotations.remove(maskImageAnnotation);
        maskImageAnnotation.setImage(null);
    }

    public double getPixelSizeMillimeter() {
        return pixelSizeMillimeter != null ? pixelSizeMillimeter : 0.144;
    }

    public void setPixelSizeMillimeter(double pixelSizeMillimeter) {
        this.pixelSizeMillimeter = pixelSizeMillimeter;
    }

    @NotNull
    public int getGroupColumn() {
        return groupColumn != null ? groupColumn : -1;
    }

    public void setGroupColumn(@NotNull int groupColumn) {
        this.groupColumn = groupColumn;
    }

    @NotNull
    public int getGroupRow() {
        return groupRow != null ? groupRow : -1;
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
        return assayType != null ? assayType : AssayType.Unknown;
    }

    public void setAssayType(@NotNull AssayType assayType) {
        this.assayType = assayType;
    }

    public int getImageWidth() {
        return imageWidth != null ? imageWidth : 0;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight != null ? imageHeight : 0;
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
        return StringUtils.nullToEmpty(experiment);
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getSample() {
        return StringUtils.nullToEmpty(sample);
    }

    public void setSample(String name) {
        this.sample = name;
    }

    public String getTimePoint() {
        return StringUtils.nullToEmpty(timePoint);
    }

    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }

    public String getOriginalFileName() {
        return StringUtils.nullToEmpty(originalFileName);
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void updateFromPayload(ImagePayload payload) {
        setOriginalFileName(payload.getFileName());
        setExperiment(payload.getExperiment());
        setAssayType(payload.getAssayType());
        setSample(payload.getSample());
        setTimePoint(payload.getTimePoint());
        setGroupColumn(payload.getGroupColumn());
        setGroupRow(payload.getGroupRow());
    }

    public void rebuildThumbnail() {
        BufferedImage raw = ImageUtils.fromPNGBytes(this.rawData);
        if (raw == null) {
            return;
        }
        rebuildThumbnail(raw);
    }

    public void rebuildThumbnail(BufferedImage originalImage) {
        BufferedImage thumbnail = ImageUtils.createThumbnail(originalImage);
        for (MaskImageAnnotation annotation : getFilteredMaskImageAnnotations()) {
            BufferedImage annotationThumbnail = ImageUtils.fromPNGBytes(annotation.getThumbnailData());
            if (annotationThumbnail != null) {
                BufferedImage gradient = ImageUtils.calculateGradient(annotationThumbnail);
                ImageUtils.overlayMask(thumbnail, gradient, ColorUtils.paletteColorFromString(annotation.getType()), 0.8);
            }
        }
        setThumbnailData(ImageUtils.toPNGByteArray(thumbnail));
    }

    public MaskImageAnnotation getOrCreateMaskAnnotation(String annotationTypeId, AtomicBoolean responseShouldSave) {
        for (MaskImageAnnotation annotation : getFilteredMaskImageAnnotations()) {
            if (annotation.getType().equals(annotationTypeId)) {
                return annotation;
            }
        }

        // Create new one
        MaskImageAnnotation annotation = new MaskImageAnnotation();
        annotation.setType(annotationTypeId);
        addMaskImageAnnotation(annotation);
        if (responseShouldSave != null) {
            responseShouldSave.set(true);
        }

        return annotation;
    }

    public MaskImageAnnotation getMaskImageAnnotation(String annotationTypeId) {
        for (MaskImageAnnotation annotation : getFilteredMaskImageAnnotations()) {
            if (annotation.getType().equals(annotationTypeId)) {
                return annotation;
            }
        }

        return null;
    }
}
