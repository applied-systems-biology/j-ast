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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageUtils {

    public static final int DEFAULT_THUMBNAIL_SIZE = 128;
    public static byte[] DUMMY_THUMBNAIL_BYTES;

    static {
        BufferedImage img = new BufferedImage(DEFAULT_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);
        DUMMY_THUMBNAIL_BYTES = toPNGByteArray(img);
    }

    public static boolean isImageNotEmpty(BufferedImage image) {
        if (image == null) {
            return false;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

        for (int pixel : pixels) {
            if ((pixel & 0x00FFFFFF) != 0) {  // if any pixel is not black
                return true;
            }
        }
        return false;
    }

    public static BufferedImage createThumbnail(BufferedImage image) {
        return createThumbnail(image, DEFAULT_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE);
    }

    public static BufferedImage invertImage(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage invertedImage = new BufferedImage(width, height, original.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = original.getRGB(x, y);
                int a = (rgba >> 24) & 0xFF; // Extract alpha
                int r = (rgba >> 16) & 0xFF; // Extract red
                int g = (rgba >> 8) & 0xFF;  // Extract green
                int b = rgba & 0xFF;         // Extract blue

                // Invert colors
                r = 255 - r;
                g = 255 - g;
                b = 255 - b;

                // Combine back into ARGB format
                int invertedRGBA = (a << 24) | (r << 16) | (g << 8) | b;
                invertedImage.setRGB(x, y, invertedRGBA);
            }
        }
        return invertedImage;
    }

    public static BufferedImage createThumbnail(BufferedImage image, int thumbnailWidth, int thumbnailHeight) {
        double thumbnailScale = Math.max(1.0 * thumbnailWidth / image.getWidth(), 1.0 * thumbnailHeight / image.getHeight());
        Image scaledImage = image.getScaledInstance((int) (image.getWidth() * thumbnailScale), (int) (image.getHeight() * thumbnailScale), Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = thumbnail.createGraphics();
        graphics2D.drawImage(scaledImage, (thumbnailWidth / 2) - scaledImage.getWidth(null) / 2, (thumbnailHeight / 2) - scaledImage.getHeight(null) / 2, null);
        graphics2D.dispose();
        return thumbnail;
    }

    public static BufferedImage fromPNGBytes(byte[] bytes) {
        if (bytes == null) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] toPNGByteArrayThumbnail(BufferedImage image) {
        return toPNGByteArray(createThumbnail(image));
    }

    public static byte[] toPNGByteArray(BufferedImage bufferedImage) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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

        if (mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
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
     * Applies Sobel edge detection to the input image.
     * Uses 3x3 Sobel kernels for both X and Y directions to compute gradient magnitude.
     * This method is optimized for speed using direct pixel manipulation.
     *
     * @param inputImage The input BufferedImage (will be converted to grayscale if needed)
     * @return A new BufferedImage containing the edge detection result (grayscale)
     */
    public static BufferedImage calculateSobel(BufferedImage inputImage) {
        // Convert to grayscale if needed
        if (inputImage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            inputImage = convertImageType(inputImage, BufferedImage.TYPE_BYTE_GRAY);
        }

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        // Get direct access to pixel data for performance
        byte[] inputPixels = ((DataBufferByte) inputImage.getRaster().getDataBuffer()).getData();

        // Create output image
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] outputPixels = ((DataBufferByte) outputImage.getRaster().getDataBuffer()).getData();

        // Sobel kernels
        // Gx: [[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]]
        // Gy: [[-1, -2, -1], [0, 0, 0], [1, 2, 1]]

        // Process each pixel (skip borders to avoid boundary checks)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Calculate pixel indices for 3x3 neighborhood
                int idx = y * width + x;
                int idxLeft = y * width + (x - 1);
                int idxRight = y * width + (x + 1);
                int idxTop = (y - 1) * width + x;
                int idxBottom = (y + 1) * width + x;
                int idxTopLeft = (y - 1) * width + (x - 1);
                int idxTopRight = (y - 1) * width + (x + 1);
                int idxBottomLeft = (y + 1) * width + (x - 1);
                int idxBottomRight = (y + 1) * width + (x + 1);

                // Convert to unsigned values
                int p00 = inputPixels[idxTopLeft] & 0xFF;
                int p01 = inputPixels[idxTop] & 0xFF;
                int p02 = inputPixels[idxTopRight] & 0xFF;
                int p10 = inputPixels[idxLeft] & 0xFF;
                int p11 = inputPixels[idx] & 0xFF;
                int p12 = inputPixels[idxRight] & 0xFF;
                int p20 = inputPixels[idxBottomLeft] & 0xFF;
                int p21 = inputPixels[idxBottom] & 0xFF;
                int p22 = inputPixels[idxBottomRight] & 0xFF;

                // Apply Sobel operator for X direction
                // Gx = -1*p00 + 0*p01 + 1*p02 + -2*p10 + 0*p11 + 2*p12 + -1*p20 + 0*p21 + 1*p22
                int gx = -p00 + p02 - 2 * p10 + 2 * p12 - p20 + p22;

                // Apply Sobel operator for Y direction
                // Gy = -1*p00 + -2*p01 + -1*p02 + 0*p10 + 0*p11 + 0*p12 + 1*p20 + 2*p21 + 1*p22
                int gy = -p00 - 2 * p01 - p02 + p20 + 2 * p21 + p22;

                // Calculate gradient magnitude: sqrt(Gx² + Gy²)
                // Use integer approximation for speed: |Gx| + |Gy| (sufficient for edge detection)
                int magnitude = Math.abs(gx) + Math.abs(gy);

                // Clamp to 0-255 range
                if (magnitude > 255) {
                    magnitude = 255;
                }

                outputPixels[idx] = (byte) magnitude;
            }
        }

        // Handle border pixels (set to 0)
        for (int x = 0; x < width; x++) {
            outputPixels[x] = 0; // Top row
            outputPixels[(height - 1) * width + x] = 0; // Bottom row
        }
        for (int y = 0; y < height; y++) {
            outputPixels[y * width] = 0; // Left column
            outputPixels[y * width + (width - 1)] = 0; // Right column
        }

        return outputImage;
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
        if (outputImage.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException("Image type is not int-ARGB");
        }
        if (mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
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

    public static byte[] getDummyThumbnailBytes() {
        return DUMMY_THUMBNAIL_BYTES;
    }
}
