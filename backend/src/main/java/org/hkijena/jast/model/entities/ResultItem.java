package org.hkijena.jast.model.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.ResultItemType;
import org.hkijena.jast.utils.StringUtils;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

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

    @Column(name = "metadata", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path = "";

    @ManyToOne(fetch = FetchType.LAZY)
    private Result result;

    
    @Column(name = "raw_data", columnDefinition = "BYTEA")
    private byte[] rawData;

    
    @Column(name = "visualization_data", columnDefinition = "BYTEA")
    private byte[] visualizationData;

    
    @Column(name = "thumbnail_data", columnDefinition = "BYTEA")
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
        return StringUtils.nullToEmpty(name);
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

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public ResultItemType getType() {
        return type;
    }

    public void setType(ResultItemType type) {
        this.type = type;
    }
}
