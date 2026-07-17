/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.AssayType;

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
