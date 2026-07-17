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

package org.hkijena.jast.services;

import org.hkijena.jast.model.entities.Image;

import java.util.function.Function;

/**
 * Used by the writing process of image metadata
 */
public class ImageMetadata {
    private final String columnName;
    private final Function<Image, Object> valueGenerator;

    public ImageMetadata(String columnName, Function<Image, Object> valueGenerator) {
        this.columnName = columnName;
        this.valueGenerator = valueGenerator;
    }

    public String getColumnName() {
        return columnName;
    }

    public Function<Image, Object> getValueGenerator() {
        return valueGenerator;
    }
}
