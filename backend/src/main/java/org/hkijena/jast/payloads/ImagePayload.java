package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.utils.ImageUtils;

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
            AssayType assayType
    ) {
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
                image.getAssayType());
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ImagePayload) obj;
        return this.id == that.id &&
                this.projectId == that.projectId &&
                Objects.equals(this.fileName, that.fileName) &&
                Objects.equals(this.owner, that.owner) &&
                Objects.equals(this.experiment, that.experiment) &&
                Objects.equals(this.sample, that.sample) &&
                Objects.equals(this.timePoint, that.timePoint) &&
                this.groupRow == that.groupRow &&
                this.groupColumn == that.groupColumn &&
                Objects.equals(this.assayType, that.assayType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId, fileName, owner, experiment, sample, timePoint, groupRow, groupColumn, assayType);
    }

    @Override
    public String toString() {
        return "ImagePayload[" +
                "id=" + id + ", " +
                "projectId=" + projectId + ", " +
                "fileName=" + fileName + ", " +
                "owner=" + owner + ", " +
                "experiment=" + experiment + ", " +
                "sample=" + sample + ", " +
                "timePoint=" + timePoint + ", " +
                "groupRow=" + groupRow + ", " +
                "groupColumn=" + groupColumn + ", " +
                "assayType=" + assayType + ']';
    }

}
