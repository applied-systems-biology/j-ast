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
