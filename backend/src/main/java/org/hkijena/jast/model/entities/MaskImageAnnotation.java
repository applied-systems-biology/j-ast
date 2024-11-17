package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import org.hkijena.jast.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * An annotation associated to an image
 * Contains another image
 */
@Entity
@Table(name = "mask_image_annotations")
public class MaskImageAnnotation {
    @Serial
    private static final long serialVersionUID = 1L;

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

    @Lob
    @Column(name = "raw_data", columnDefinition = "BLOB")
    private byte[] rawData;

    @Lob
    @Column(name = "thumbnail_data", columnDefinition = "BLOB")
    private byte[] thumbnailData;

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

    /**
     * Checks if a type ID is valid
     * For J-AST this only is true if the type is one of:
     * * strip-disk
     * * plate
     * * zoi-shape
     * @param type the type
     * @return if it's a valid type
     */
    public static boolean isValidType(String type) {
        return "strip-disk".equals(type) || "plate".equals(type) || "zoi-shape".equals(type);
    }

    /**
     * Sets the raw data and thumbnail to an empty mask
     * @param image the image used as size reference
     */
    public void resetToMask(Image image) {
        BufferedImage img = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY);
        this.rawData = ImageUtils.toPNGByteArray(img);
        this.thumbnailData = ImageUtils.toPNGByteArrayThumbnail(img);
    }

    public void incrementVersion() {
        this.setVersion(this.getVersion() + 1);
    }
}
