package org.hkijena.jast.model.entities;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.utils.ColorUtils;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.JsonUtils;
import org.hkijena.jast.utils.StringUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * An image dataset
 */
@Entity
@Table(name = "images")
public class Image {
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

    @Column(name = "mic")
    private Double mic = 0d;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "group_row")
    private Integer groupRow = -1;

    @Column(name = "group_column")
    private Integer groupColumn = -1;

    @Column(name = "raw_data_file_id", columnDefinition = "TEXT")
    private String rawDataFileId;

    @Column(name = "raw_data_file_size")
    private Long rawDataFileSize = 0L;

    @Column(name = "thumbnail_data_file_id", columnDefinition = "TEXT")
    private String thumbnailDataFileId;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "pixel_size_mm")
    private Double pixelSizeMillimeter = 0d;

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

    public double getMic() {
        if (mic == null) {
            mic = 0d;
        }
        return mic;
    }

    public void setMic(double mic) {
        this.mic = mic;
    }

    public long getRawDataFileSize() {
        return rawDataFileSize != null ? rawDataFileSize : 0;
    }

    public void setRawDataFileSize(long rawDataFileSize) {
        this.rawDataFileSize = rawDataFileSize;
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

    public String getRawDataFileId() {
        return rawDataFileId;
    }

    public void setRawDataFileId(String rawDataFileId) {
        this.rawDataFileId = rawDataFileId;
    }

    public void setGroupColumn(Integer groupColumn) {
        this.groupColumn = groupColumn;
    }

    public void setGroupRow(Integer groupRow) {
        this.groupRow = groupRow;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public void setMaskImageAnnotations(List<MaskImageAnnotation> maskImageAnnotations) {
        this.maskImageAnnotations = maskImageAnnotations;
    }

    public void setPixelSizeMillimeter(Double pixelSizeMillimeter) {
        this.pixelSizeMillimeter = pixelSizeMillimeter;
    }

    public String getThumbnailDataFileId() {
        return thumbnailDataFileId;
    }

    public void setThumbnailDataFileId(String thumbnailDataFileId) {
        this.thumbnailDataFileId = thumbnailDataFileId;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
        setMic(payload.getMic());
        setSample(payload.getSample());
        setTimePoint(payload.getTimePoint());
        setGroupColumn(payload.getGroupColumn());
        setGroupRow(payload.getGroupRow());
        setPixelSizeMillimeter(payload.getPixelSizeMillimeter());
        setMetadata(payload.getMetadata());
    }

    @Transactional
    public void rebuildThumbnail(FileStorageService fileStorageService) {
        BufferedImage raw = fileStorageService.loadPngOrNull(rawDataFileId);
        if (raw == null) {
            return;
        }
        rebuildThumbnail(fileStorageService, raw);
    }

    public BufferedImage getVisualization(FileStorageService fileStorageService) {
        BufferedImage result = ImageUtils.convertImageType(ImageUtils.fromPNGBytes(getRawData(fileStorageService)), BufferedImage.TYPE_INT_ARGB);
        for (MaskImageAnnotation annotation : getFilteredMaskImageAnnotations()) {
            BufferedImage annotationImg = ImageUtils.fromPNGBytes(annotation.getRawData(fileStorageService));
            if (annotationImg != null) {
                BufferedImage gradient = ImageUtils.calculateSobel(annotationImg);
                ImageUtils.overlayMask(result, gradient, ColorUtils.paletteColorFromString(annotation.getType()), 0.8);
            }
        }
        return result;
    }

    @Transactional
    public void rebuildThumbnail(FileStorageService fileStorageService, BufferedImage originalImage) {
        BufferedImage thumbnail = ImageUtils.createThumbnail(originalImage);
        for (MaskImageAnnotation annotation : getFilteredMaskImageAnnotations()) {
            BufferedImage annotationThumbnail = ImageUtils.fromPNGBytes(annotation.getThumbnailData(fileStorageService));
            if (annotationThumbnail != null) {
                BufferedImage gradient = ImageUtils.calculateSobel(annotationThumbnail);
                ImageUtils.overlayMask(thumbnail, gradient, ColorUtils.paletteColorFromString(annotation.getType()), 0.8);
            }
        }
        fileStorageService.deleteLater(thumbnailDataFileId); // Delete old version
        setThumbnailDataFileId(fileStorageService.store(thumbnail));
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

    public byte[] getThumbnailData(FileStorageService fileStorageService) {
        byte[] data = fileStorageService.loadOrNull(thumbnailDataFileId);
        return data != null ? data : ImageUtils.DUMMY_THUMBNAIL_BYTES;
    }

    public byte[] getRawData(FileStorageService fileStorageService) {
        return fileStorageService.load(rawDataFileId);
    }

    @Transactional
    public void setRawData(FileStorageService fileStorageService, byte[] pngByteArray) {
        fileStorageService.deleteLater(rawDataFileId); // Delete old version
        this.rawDataFileId = fileStorageService.store(pngByteArray);
        this.rawDataFileSize = (long) pngByteArray.length;
    }

    @Transactional
    public void deleteFilesLater(FileStorageService fileStorageService) {
        fileStorageService.deleteLater(rawDataFileId);
        fileStorageService.deleteLater(thumbnailDataFileId);
        for (MaskImageAnnotation annotation : getMaskImageAnnotations()) {
            annotation.deleteFilesLater(fileStorageService);
        }

    }

    public String getStripSequenceString() {
        Object stripPreset = getMetadata().get("stripPreset");
        if (stripPreset == null) {
            return "";
        }
        if (stripPreset instanceof String) {
            // parse as JSON object
            stripPreset = JsonUtils.readFromString((String) stripPreset, JsonNode.class);
        }
        if (stripPreset instanceof Map map) {
            return ((List<?>) map.get("ticks")).stream().map(Object::toString).collect(Collectors.joining(","));
        }
        if (stripPreset instanceof JsonNode node) {
            List<Double> items = new ArrayList<>();
            node.get("ticks").elements().forEachRemaining(nd -> items.add(nd.asDouble()));
            return items.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return "";
    }
}
