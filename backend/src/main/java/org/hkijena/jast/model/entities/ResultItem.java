package org.hkijena.jast.model.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.Type;
import org.hkijena.jast.model.ResultItemType;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.utils.ImageUtils;
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

    @Column(name = "raw_data_file_id", columnDefinition = "TEXT")
    private String rawDataFileId;

    @Column(name = "raw_data_file_size")
    private Long rawDataFileSize = 0L;

    @Column(name = "visualization_data_file_id", columnDefinition = "TEXT")
    private String visualizationDataFileId;

    @Column(name = "thumbnail_data_file_id", columnDefinition = "TEXT")
    private String thumbnailDataFileId;

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

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public String getRawDataFileId() {
        return rawDataFileId;
    }

    public void setRawDataFileId(String rawDataFileId) {
        this.rawDataFileId = rawDataFileId;
    }

    public long getRawDataFileSize() {
        return rawDataFileSize != null ? rawDataFileSize : 0L;
    }

    public void setRawDataFileSize(long rawDataFileSize) {
        this.rawDataFileSize = rawDataFileSize;
    }

    public String getThumbnailDataFileId() {
        return thumbnailDataFileId;
    }

    public void setThumbnailDataFileId(String thumbnailDataFileId) {
        this.thumbnailDataFileId = thumbnailDataFileId;
    }

    public String getVisualizationDataFileId() {
        return visualizationDataFileId;
    }

    public void setVisualizationDataFileId(String visualizationDataFileId) {
        this.visualizationDataFileId = visualizationDataFileId;
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

    public byte[] getThumbnailData(FileStorageService fileStorageService) {
        byte[] bytes = fileStorageService.loadOrNull(thumbnailDataFileId);
        return bytes != null ? bytes : ImageUtils.DUMMY_THUMBNAIL_BYTES;
    }

    public byte[] getRawData(FileStorageService fileStorageService) {
        return fileStorageService.load(rawDataFileId);
    }

    public void setRawData(FileStorageService fileStorageService, byte[] byteArray) {
        this.rawDataFileId = fileStorageService.store(byteArray);
        this.rawDataFileSize = (long) byteArray.length;
    }

    public void setThumbnailData(FileStorageService fileStorageService, byte[] byteArray) {
        this.thumbnailDataFileId = fileStorageService.store(byteArray);
    }

    @Transactional
    public void deleteFilesLater(FileStorageService fileStorageService) {
        fileStorageService.deleteLater(rawDataFileId);
        fileStorageService.deleteLater(visualizationDataFileId);
        fileStorageService.deleteLater(thumbnailDataFileId);
    }
}
