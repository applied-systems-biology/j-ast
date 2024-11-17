package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.MaskImageAnnotation;

public record MaskImageAnnotationPayload(@JsonProperty long id,
                                         @JsonProperty long imageId,
                                         @JsonProperty long projectId,
                                         @JsonProperty String annotationTypeId) {
    public static MaskImageAnnotationPayload create(MaskImageAnnotation annotation) {
        return new MaskImageAnnotationPayload(annotation.getId(),
                annotation.getImage().getId(),
                annotation.getImage().getProject().getId(),
                annotation.getType());
    }
}
