package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.utils.ImageUtils;

public record EditImageRequest(
    @JsonProperty long id,
    @JsonProperty String fileName,
    @JsonProperty String experiment,
    @JsonProperty String sample,
    @JsonProperty String timePoint,
    @JsonProperty int groupRow,
    @JsonProperty int groupColumn,
    @JsonProperty AssayType assayType
) {
}
