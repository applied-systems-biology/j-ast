package org.hkijena.jipipe.webapp.growthassay.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageUtils {
    public static void createThumbnail(BufferedImage image, Path thumbnailStoragePath) throws IOException {
        double thumbnailScale = Math.max(64.0 / image.getWidth(), 64.0 / image.getHeight());
        Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(64,64, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = thumbnail.createGraphics();
        graphics2D.drawImage(scaledImage, 32 - scaledImage.getWidth(null) / 2, 32 - scaledImage.getHeight(null) / 2, null);
        graphics2D.dispose();
        ImageIO.write(thumbnail, "PNG", thumbnailStoragePath.toFile());
    }
}
