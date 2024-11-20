package org.hkijena.jast.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

public class ImageUtils {

    public static final int DEFAULT_THUMBNAIL_SIZE = 128;

    public static BufferedImage createThumbnail(BufferedImage image) {
        return createThumbnail(image, DEFAULT_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE);
    }

    public static BufferedImage createThumbnail(BufferedImage image, int thumbnailWidth, int thumbnailHeight) {
        double thumbnailScale = Math.max(1.0 * thumbnailWidth / image.getWidth(), 1.0 * thumbnailHeight / image.getHeight());
        Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth,thumbnailHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = thumbnail.createGraphics();
        graphics2D.drawImage(scaledImage, (thumbnailWidth / 2) - scaledImage.getWidth(null) / 2, (thumbnailHeight / 2) - scaledImage.getHeight(null) / 2, null);
        graphics2D.dispose();
        return thumbnail;
    }

    public static BufferedImage fromPNGBytes(byte[] bytes) {
        if(bytes == null) return null;
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(inputStream);
        }
        catch(IOException e) {
            return null;
        }
    }

    public static byte[] toPNGByteArrayThumbnail(BufferedImage image) {
        return toPNGByteArray(createThumbnail(image));
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

    public static void morphologicalOperation(BufferedImage input, BufferedImage output, boolean dilate) {
        int width = input.getWidth();
        int height = input.getHeight();
        int radius = 1; // Radius for dilation/erosion

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean result = !dilate;

                // Check neighborhood
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                            int value = (input.getRGB(nx, ny) & 0xFF) > 0 ? 1 : 0;
                            if (dilate) {
                                result |= (value == 1);
                            } else {
                                result &= (value == 1);
                            }
                        }
                    }
                }

                output.setRGB(x, y, result ? 0xFFFFFFFF : 0xFF000000);
            }
        }
    }

    public static BufferedImage calculateGradient(BufferedImage mask) {

        if(mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            mask = convertImageType(mask, BufferedImage.TYPE_BYTE_GRAY);
        }

        int width = mask.getWidth();
        int height = mask.getHeight();

        BufferedImage dilated = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage eroded = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Perform dilation
        morphologicalOperation(mask, dilated, true);

        // Perform erosion
        morphologicalOperation(mask, eroded, false);

        // Compute gradient: dilated - eroded
        BufferedImage gradient = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int dilatedValue = (dilated.getRGB(x, y) & 0xFF);
                int erodedValue = (eroded.getRGB(x, y) & 0xFF);
                int gradientValue = Math.max(0, dilatedValue - erodedValue);
                gradient.setRGB(x, y, gradientValue > 0 ? 0xFFFFFFFF : 0xFF000000); // Binary result
            }
        }

        return gradient;
    }

    /**
     * Converts a BufferedImage to a new type.
     *
     * @param sourceImage The original image to convert.
     * @param targetType  The target image type (e.g., BufferedImage.TYPE_INT_ARGB).
     * @return A new BufferedImage with the specified type.
     */
    public static BufferedImage convertImageType(BufferedImage sourceImage, int targetType) {
        // Create a new BufferedImage of the desired type
        BufferedImage convertedImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);

        // Draw the original image onto the new image
        Graphics2D graphics = convertedImage.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return convertedImage;
    }

    public static void overlayMask(BufferedImage outputImage, BufferedImage mask, Color color, double opacity) {
        // Ensure both images have the same dimensions
        if (outputImage.getWidth() != mask.getWidth() || outputImage.getHeight() != mask.getHeight()) {
            throw new IllegalArgumentException("Both images must have the same dimensions");
        }
        if(outputImage.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException("Image type is not int-ARGB");
        }
        if(mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            mask = convertImageType(mask, BufferedImage.TYPE_BYTE_GRAY);
        }

        int width = outputImage.getWidth();
        int height = outputImage.getHeight();

        // Access raw image pixel data
        int[] outputPixels = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();
        byte[] maskPixels = ((DataBufferByte) mask.getRaster().getDataBuffer()).getData();

        final int maskColorRed = color.getRed();
        final int maskColorGreen = color.getGreen();
        final int maskColorBlue = color.getBlue();

        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;

                // Extract raw pixel color (ARGB)
                int rawPixel = outputPixels[idx];
                int red = (rawPixel >> 16) & 0xFF;
                int green = (rawPixel >> 8) & 0xFF;
                int blue = rawPixel & 0xFF;

                // Extract mask greyscale value (0-255)
                int maskValue = (int) ((maskPixels[idx] & 0xFF) * opacity); // Byte to unsigned

                // Interpolate red color
                int interpolatedRed = (int) ((1 - maskValue / 255.0) * red + (maskValue / 255.0) * maskColorRed);
                int interpolatedGreen = (int) ((1 - maskValue / 255.0) * green + (maskValue / 255.0) * maskColorGreen);
                int interpolatedBlue = (int) ((1 - maskValue / 255.0) * blue + (maskValue / 255.0) * maskColorBlue);

                // Compose ARGB for the output
                outputPixels[idx] = (0xFF << 24) // Alpha
                        | (interpolatedRed << 16)
                        | (interpolatedGreen << 8)
                        | interpolatedBlue;
            }
        }
    }

}
