package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.utils.ImageUtils;

public record ImageInfoMessage (
    @JsonProperty long id,
    @JsonProperty long projectId,
    @JsonProperty String fileName,
    @JsonProperty String owner,
    @JsonProperty String experiment,
    @JsonProperty String sample,
    @JsonProperty String timePoint,
    @JsonProperty String thumbnailData
) {

    public static ImageInfoMessage create(Image image) {
        return new ImageInfoMessage(image.getId(),
                image.getProject().getId(),
                image.getOriginalFileName(),
                image.getProject().getOwner() != null ? image.getProject().getOwner().getEmail() : "",
                image.getExperiment(),
                image.getSample(),
                image.getTimePoint(),
                ImageUtils.toPNGBase64String(image.getThumbnailData())
                );
    }
}
