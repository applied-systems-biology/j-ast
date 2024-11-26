package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hkijena.jast.model.ResultItemType;

import java.io.Serial;

/**
 * A result item that has the metadata (as JSON object) and multiple data fields (raw data, optional data for browser visualization only, and a thumbnail)
 */
@Entity
@Table(name = "result_items")
public class ResultItem {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "metadata", columnDefinition = "TEXT")
    @NotNull
    private String metadata = "";

    @Column(name = "name", columnDefinition = "TEXT")
    @NotNull
    private String name;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path = "";

    @ManyToOne(fetch = FetchType.LAZY)
    private Result result;

    @Lob
    @Column(name = "raw_data", columnDefinition = "BLOB")
    private byte[] rawData;

    @Lob
    @Column(name = "visualization_data", columnDefinition = "BLOB")
    private byte[] visualizationData;

    @Lob
    @Column(name = "thumbnail_data", columnDefinition = "BLOB")
    private byte[] thumbnailData;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ResultItemType type = ResultItemType.Unknown;

    @Column(name = "visualization_type")
    @Enumerated(EnumType.STRING)
    private ResultItemType visualizationType = ResultItemType.Unknown;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getVisualizationData() {
        return visualizationData;
    }

    public void setVisualizationData(byte[] visualizationData) {
        this.visualizationData = visualizationData;
    }

    public ResultItemType getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(ResultItemType visualizationType) {
        this.visualizationType = visualizationType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public ResultItemType getType() {
        return type;
    }

    public void setType(ResultItemType type) {
        this.type = type;
    }
}
