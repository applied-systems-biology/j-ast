package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;

import java.util.*;

public class ImagePayload {
    @JsonProperty
    private long id;
    @JsonProperty
    private long projectId;
    @JsonProperty
    private String fileName;
    @JsonProperty
    private String owner;
    @JsonProperty
    private String experiment;
    @JsonProperty
    private String sample;
    @JsonProperty
    private String timePoint;
    @JsonProperty
    private int groupRow;
    @JsonProperty
    private int groupColumn;
    @JsonProperty
    private AssayType assayType;
    @JsonProperty
    private int version;
    @JsonProperty
    private double pixelSizeMillimeter;
    @JsonProperty
    private List<MaskImageAnnotationPayload> maskImageAnnotations = new ArrayList<>();
    @JsonProperty
    private Map<String, Object> metadata = new HashMap<>();
    @JsonProperty
    private long size;

    public ImagePayload() {

    }

    public ImagePayload(Image image) {
        this.id = image.getId();
        this.projectId = image.getProject().getId();
        this.assayType = image.getAssayType();
        this.fileName = image.getOriginalFileName();
        this.experiment = image.getExperiment();
        this.sample = image.getSample();
        this.timePoint = image.getTimePoint();
        this.groupRow = image.getGroupRow();
        this.groupColumn = image.getGroupColumn();
        this.version = image.getVersion();
        this.pixelSizeMillimeter = image.getPixelSizeMillimeter();
        this.maskImageAnnotations = image.getFilteredMaskImageAnnotations().stream().map(MaskImageAnnotationPayload::create).toList();
        this.metadata = new HashMap<>(image.getMetadata());
        this.size = image.getRawData().length;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public double getPixelSizeMillimeter() {
        return pixelSizeMillimeter;
    }

    public void setPixelSizeMillimeter(double pixelSizeMillimeter) {
        this.pixelSizeMillimeter = pixelSizeMillimeter;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public String getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }

    public int getGroupRow() {
        return groupRow;
    }

    public void setGroupRow(int groupRow) {
        this.groupRow = groupRow;
    }

    public int getGroupColumn() {
        return groupColumn;
    }

    public void setGroupColumn(int groupColumn) {
        this.groupColumn = groupColumn;
    }

    public AssayType getAssayType() {
        return assayType;
    }

    public void setAssayType(AssayType assayType) {
        this.assayType = assayType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<MaskImageAnnotationPayload> getMaskImageAnnotations() {
        return maskImageAnnotations;
    }

    public void setMaskImageAnnotations(List<MaskImageAnnotationPayload> maskImageAnnotations) {
        this.maskImageAnnotations = maskImageAnnotations;
    }
}
