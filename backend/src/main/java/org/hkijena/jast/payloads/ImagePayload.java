package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.utils.ImageUtils;

public record ImagePayload(
    @JsonProperty long id,
    @JsonProperty long projectId,
    @JsonProperty String fileName,
    @JsonProperty String owner,
    @JsonProperty String experiment,
    @JsonProperty String sample,
    @JsonProperty String timePoint,
    @JsonProperty int groupRow,
    @JsonProperty int groupColumn,
    @JsonProperty AssayType assayType
) {

    public static ImagePayload create(Image image) {
        return new ImagePayload(image.getId(),
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
}
