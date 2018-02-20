/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.FileUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Set dots per inch in the metadata of a Portable Network Graphics image.
 */
public final class PngUtils {

    /**
     * Return image height in pixels.
     *
     * @param image image file.
     * @throws IOException Failed to read the image.
     * @return Image height in pixels.
     */
    private static int getHeight(final File image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image);
        return bufferedImage.getHeight();
    }
    /**
     * Return image width in pixels.
     *
     * @param image image file.
     * @throws IOException Failed to read the image.
     * @return Image width in pixels.
     */
    private static int getWidth(final File image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image);
        return bufferedImage.getWidth();
    }
    /**
     * Creates a thumbnail copy of provided image, prefixed with "thumb_".
     *
     * @param image image file.
     * @throws IOException Failed to read the image or to write the thumbnail.
     */
    public static void resizePng(final File image)
            throws IOException {
        BufferedImage originalImage = ImageIO.read(image);

        final int imageWidth = getWidth(image);
        final int imageHeight = getHeight(image);
        final int newWidth = 700;

        String absolutePath = image.getAbsolutePath();

        /** File thumbFile = new File(absolutePath.substring(0, absolutePath
                .lastIndexOf(File.separator)) + File.separator + "thumb_"
                + image.getName()); */

        File thumbFile = new File(image.getParent(), "thumb_" + image.getName());

        if (imageWidth > newWidth) {

            final int newHeight = Math.round(imageHeight * newWidth / imageWidth);

            /* System.out.println("Creating thumbnail of: " + image.getName()
                    + " (" + newWidth + " x " + newHeight + ")"); */

            BufferedImage scaledBI = getScaledInstance(
                    originalImage,
                    newWidth,
                    newHeight,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    true);

            saveBufferedImage(scaledBI, thumbFile, 160);
        } else {
            saveBufferedImage(originalImage, thumbFile, 160);
        }
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(
        BufferedImage img,
        int targetWidth,
        int targetHeight,
        Object hint,
        boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * Set the DPI on {@code image} so that it fits in {@code maxHeightInInches},
     * or to 160 if short enough.
     *
     * @param image PNG image file.
     * @param maxHeightInInches maximum available image height in inches.
     * @throws IOException Failed to save the image.
     */
    public static void setSafeDpi(final File image, final int maxHeightInInches)
            throws IOException {
        final int imageHeight = getHeight(image);
        final int defaultDpi = 160;
        final int defaultMaxHeight = maxHeightInInches * defaultDpi;

        // Images that do not fit by default must be
        if (imageHeight > defaultMaxHeight) {
            final double dpi = imageHeight * 1.0 / maxHeightInInches;
            setDpi(image, (int) Math.round(dpi));
        } else {
            setDpi(image);
        }
    }

    /**
     * Set the DPI on {@code image} to 160.
     *
     * @param image PNG image file.
     * @throws IOException Failed to save the image.
     */
    public static void setDpi(final File image) throws IOException {
        setDpi(image, 160);
    }

    /**
     * Set the DPI on {@code image} to {@code dotsPerInch}.
     *
     * @param image PNG image file.
     * @param dotsPerInch DPI to set in metadata.
     * @throws IOException Failed to save the image.
     */
    public static void setDpi(final File image, final int dotsPerInch) throws IOException {
        BufferedImage in = ImageIO.read(image);
        File updatedImage = File.createTempFile(image.getName(), ".tmp");
        saveBufferedImage(in, updatedImage, dotsPerInch);

        FileUtils.deleteQuietly(image);
        FileUtils.moveFile(updatedImage, image);
    }

    /*
     * Save an image, setting the DPI.
     *
     * @param bufferedImage The image to save.
     * @param outputFile The file to save the image to.
     * @param dotsPerInch The DPI setting to use.
     * @throws IOException Failed to write the image.
     */
    private static void saveBufferedImage(final BufferedImage bufferedImage,
                                          final File outputFile,
                                          final int dotsPerInch)
            throws IOException {
        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext();) {
            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier =
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                continue;
            }

            setDpi(metadata, dotsPerInch);

            final ImageOutputStream stream = ImageIO.createImageOutputStream(outputFile);
            try {
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(bufferedImage, null, metadata), writeParam);
            } finally {
                stream.close();
            }
            break;
        }
    }

    /*
     * Set the DPI in image metadata.
     *
     * @param metadata Image metadata.
     * @param dotsPerInch DPI setting to set.
     * @throws IIOInvalidTreeException Failed to write metadata.
     */
    private static void setDpi(IIOMetadata metadata, final int dotsPerInch)
            throws IIOInvalidTreeException {

        final double inchesPerMillimeter = 1.0 / 25.4;
        final double dotsPerMillimeter = dotsPerInch * inchesPerMillimeter;

        IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
        horizontalPixelSize.setAttribute("value", Double.toString(dotsPerMillimeter));

        IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
        verticalPixelSize.setAttribute("value", Double.toString(dotsPerMillimeter));

        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        dimension.appendChild(horizontalPixelSize);
        dimension.appendChild(verticalPixelSize);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dimension);

        metadata.mergeTree("javax_imageio_1.0", root);
    }

    private PngUtils() {
        // Not used.
    }
}
