package org.hkijena.jipipe.webapp.growthassay.utils;

import java.awt.*;

public class ColorUtils {
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

}
