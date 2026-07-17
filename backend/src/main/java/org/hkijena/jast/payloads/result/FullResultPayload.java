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

package org.hkijena.jast.payloads.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.Result;

import java.util.ArrayList;
import java.util.List;

public class FullResultPayload extends ResultPayload {
    @JsonProperty
    private List<ResultItemPayload> items = new ArrayList<>();

    public FullResultPayload() {

    }

    public FullResultPayload(Result result) {
        super(result);
        this.items = result.getResultItems().stream().map(ResultItemPayload::new).toList();
    }

    public List<ResultItemPayload> getItems() {
        return items;
    }

    public void setItems(List<ResultItemPayload> items) {
        this.items = items;
    }
}
