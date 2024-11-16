package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.ImageAnnotation;

public record ProjectImageAnnotationPayload(@JsonProperty long id,
                                            @JsonProperty long imageId,
                                            @JsonProperty long projectId,
                                            @JsonProperty String annotationTypeId) {
    public static ProjectImageAnnotationPayload create(ImageAnnotation annotation) {
        return new ProjectImageAnnotationPayload(annotation.getId(),
                annotation.getImage().getId(),
                annotation.getImage().getProject().getId(),
                annotation.getType());
    }
}
