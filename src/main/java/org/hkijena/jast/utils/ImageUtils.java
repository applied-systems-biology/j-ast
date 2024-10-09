package org.hkijena.jast.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageUtils {
    public static void createThumbnail(BufferedImage image, int thumbnailWidth, int thumbnailHeight, Path thumbnailStoragePath) throws IOException {
        double thumbnailScale = Math.max(1.0 * thumbnailWidth / image.getWidth(), 1.0 * thumbnailHeight / image.getHeight());
        Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth,thumbnailHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = thumbnail.createGraphics();
        graphics2D.drawImage(scaledImage, (thumbnailWidth / 2) - scaledImage.getWidth(null) / 2, (thumbnailHeight / 2) - scaledImage.getHeight(null) / 2, null);
        graphics2D.dispose();
        ImageIO.write(thumbnail, "PNG", thumbnailStoragePath.toFile());
    }
}
