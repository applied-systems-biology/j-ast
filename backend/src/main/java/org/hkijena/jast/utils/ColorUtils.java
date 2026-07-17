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

package org.hkijena.jast.utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorUtils {

    public static final Color[] PALETTE = new Color[]{
            new Color(255, 0, 0),        // Red
            new Color(0, 255, 0),        // Green
            new Color(255, 255, 0),      // Yellow
            new Color(0, 255, 255),      // Cyan
            new Color(255, 0, 255),      // Magenta
            new Color(255, 165, 0),      // Orange
            new Color(128, 0, 128),      // Purple
            new Color(255, 105, 180),    // HotPink
    };

    public static final Map<String, Color> STATIC_PALETTE = new HashMap<>();

    static {
        STATIC_PALETTE.put("plate", PALETTE[0]);
        STATIC_PALETTE.put("strip-disk", PALETTE[1]);
        STATIC_PALETTE.put("zoi-shape", PALETTE[3]);
    }

    /**
     * Converts a color to a Hex string
     *
     * @param color the color
     * @return A hex string #RRGGBB or #RRGGBBAA (only if alpha is not 255)
     */
    public static String colorToHexString(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        } else {
            return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
    }

    public static Color randomColorFromString(String str, float saturation, float brightness) {
        return Color.getHSBColor(stringToFloat(str), saturation, brightness);
    }

    public static float stringToFloat(String input) {
        int hash = input.hashCode();
        long positiveHash = hash & 0xFFFFFFFFL;
        return positiveHash / (float) 0xFFFFFFFFL;
    }

    public static Color paletteColorFromString(String str) {
        Color color = STATIC_PALETTE.getOrDefault(str, null);
        if (color != null) {
            return color;
        }
        int hash = str.hashCode();
        if (hash < 0) {
            hash = -hash;
        }
        return PALETTE[hash % PALETTE.length];
    }

}
