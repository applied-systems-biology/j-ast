package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * An annotation associated to an image
 * Contains another image
 */
@Entity
@Table(name = "mask_image_annotations")
public class MaskImageAnnotation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "version")
    private Integer version = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private Image image;

    @Column(name = "type", columnDefinition = "TEXT")
    private String type;

    @Column(name = "raw_data_file_id", columnDefinition = "TEXT")
    private String rawDataFileId;

    @Column(name = "raw_data_file_size")
    private Long rawDataFileSize = 0L;

    @Column(name = "thumbnail_data_file_id", columnDefinition = "TEXT")
    private String thumbnailDataFileId;


    /**
     * Checks if a type ID is valid
     * For J-AST this only is true if the type is one of:
     * * strip-disk
     * * plate
     * * zoi-shape
     *
     * @param type the type
     * @return if it's a valid type
     */
    public static boolean isValidType(String type) {
        return "strip-disk".equals(type) || "plate".equals(type) || "zoi-shape".equals(type);
    }

    public int getVersion() {
        return version == null ? 0 : version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getRawDataFileId() {
        return rawDataFileId;
    }

    public void setRawDataFileId(String rawDataFileId) {
        this.rawDataFileId = rawDataFileId;
    }

    public String getThumbnailDataFileId() {
        return thumbnailDataFileId;
    }

    public void setThumbnailDataFileId(String thumbnailDataFileId) {
        this.thumbnailDataFileId = thumbnailDataFileId;
    }

    public byte[] getRawData(FileStorageService fileStorageService) {
        byte[] bytes = fileStorageService.loadOrNull(rawDataFileId);
        if (bytes == null) {
            return ImageUtils.toPNGByteArray(new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY));
        }
        return bytes;
    }

    @Transactional
    public void setRawData(FileStorageService fileStorageService, byte[] rawData) {
        fileStorageService.deleteLater(rawDataFileId); // Delete old version
        rawDataFileId = fileStorageService.store(rawData);
        rawDataFileSize = (long) rawData.length;
    }

    public byte[] getThumbnailData(FileStorageService fileStorageService) {
        byte[] bytes = fileStorageService.loadOrNull(thumbnailDataFileId);
        if (bytes == null) {
            return ImageUtils.DUMMY_THUMBNAIL_BYTES;
        }
        return bytes;
    }

    @Transactional
    public void setThumbnailData(FileStorageService fileStorageService, byte[] thumbnailData) {
        fileStorageService.deleteLater(thumbnailDataFileId); // Delete old version
        thumbnailDataFileId = fileStorageService.store(thumbnailData);
    }

    /**
     * Sets the raw data and thumbnail to an empty mask
     *
     * @param image the image used as size reference
     */
    @Transactional
    public void resetToEmptyMask(FileStorageService fileStorageService, Image image) {
        fileStorageService.deleteLater(rawDataFileId); // Delete old version
        fileStorageService.deleteLater(thumbnailDataFileId); // Delete old version

        BufferedImage img = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY);
        this.rawDataFileId = fileStorageService.store(ImageUtils.toPNGByteArray(img));
        this.thumbnailDataFileId = fileStorageService.store(ImageUtils.toPNGByteArrayThumbnail(img));
    }

    public void incrementVersion() {
        this.setVersion(this.getVersion() + 1);
    }

    public void deleteFilesLater(FileStorageService fileStorageService) {
        fileStorageService.deleteLater(rawDataFileId);
        fileStorageService.deleteLater(thumbnailDataFileId);
    }

    public long getRawDataFileSize() {
        return rawDataFileSize != null ? rawDataFileSize : 0;
    }

    public void setRawDataFileSize(long rawDataFileLength) {
        this.rawDataFileSize = rawDataFileLength;
    }

}
