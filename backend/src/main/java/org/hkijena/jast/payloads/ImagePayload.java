package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private List<MaskImageAnnotationPayload> maskImageAnnotations = new ArrayList<>();

    public ImagePayload() {

    }

    public ImagePayload(
            long id,
            long projectId,
            String fileName,
            String owner,
            String experiment,
            String sample,
            String timePoint,
            int groupRow,
            int groupColumn,
            AssayType assayType,
            int version,
            List<MaskImageAnnotationPayload> maskImageAnnotations) {
        this.id = id;
        this.projectId = projectId;
        this.fileName = fileName;
        this.owner = owner;
        this.experiment = experiment;
        this.sample = sample;
        this.timePoint = timePoint;
        this.groupRow = groupRow;
        this.groupColumn = groupColumn;
        this.assayType = assayType;
        this.version = version;
        this.maskImageAnnotations = maskImageAnnotations;
    }

    public ImagePayload(Image image) {
       this(image.getId(),
                image.getProject().getId(),
                image.getOriginalFileName(),
                image.getProject().getOwner() != null ? image.getProject().getOwner().getEmail() : "",
                image.getExperiment(),
                image.getSample(),
                image.getTimePoint(),
                image.getGroupRow(),
                image.getGroupColumn(),
                image.getAssayType(),
               image.getVersion(),
               image.getFilteredMaskImageAnnotations().stream().map(MaskImageAnnotationPayload::create).toList());
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
