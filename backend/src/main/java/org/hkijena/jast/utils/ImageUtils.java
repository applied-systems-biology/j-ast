package org.hkijena.jast.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

public class ImageUtils {
    public static BufferedImage createThumbnail(BufferedImage image, int thumbnailWidth, int thumbnailHeight) {
        double thumbnailScale = Math.max(1.0 * thumbnailWidth / image.getWidth(), 1.0 * thumbnailHeight / image.getHeight());
        Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth,thumbnailHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = thumbnail.createGraphics();
        graphics2D.drawImage(scaledImage, (thumbnailWidth / 2) - scaledImage.getWidth(null) / 2, (thumbnailHeight / 2) - scaledImage.getHeight(null) / 2, null);
        graphics2D.dispose();
        return thumbnail;
    }

    public static byte[] toPNGByteArray(BufferedImage bufferedImage) {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toPNGBase64String(byte[] bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
