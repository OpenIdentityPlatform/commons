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
 * Copyright 2012-2014 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Set;

/**
 * Copy images from source to destination.
 */
public final class ImageCopier {

    /**
     * Support a subset of formats described in the documentation for the <a
     * href="http://www.docbook.org/tdg/en/html/imagedata.html">ImageData</a>
     * element.
     */
    private static final String[] IMAGE_FILE_SUFFIXES =
    {".bmp", ".eps", ".gif", ".jpeg", ".jpg", ".png", ".svg", ".tiff"};

    /**
     * Copy images from source to destination.
     *
     *
     * @param docType         Type of output document,
     *                        such as {@code epub} or {@code html}
     * @param baseName        Directory name to add, such as {@code index}.
     *                        Leave null or empty when not adding a directory name.
     * @param mojo            Mojo holding configuration information.
     *
     * @throws IOException    Something went wrong copying images.
     */
    public static void copyImages(final String docType,
                                  final String baseName,
                                  final AbstractDocbkxMojo mojo)
            throws IOException {
        copyImages(
                docType,
                baseName,
                mojo.getDocumentSrcName(),
                mojo.getDocbkxModifiableSourcesDirectory(),
                mojo.getDocbkxOutputDirectory());
    }

    /**
     * Copy images from source to destination.
     *
     * <p>
     *
     * DocBook XSL does not copy the images,
     * because XSL does not have a facility for copying files.
     * Unfortunately, neither does docbkx-tools.
     *
     * @param docType         Type of output document,
     *                        such as {@code epub} or {@code html}
     * @param baseName        Directory name to add, such as {@code index}.
     *                        Leave null or empty when not adding a directory name.
     * @param documentSrcName Top-level DocBook XML document source name,
     *                        such as {@code index.xml}.
     * @param sourceDirectory Base directory for DocBook XML sources.
     * @param outputDirectory Base directory where the output is found.
     *
     * @throws IOException    Something went wrong copying images.
     */
    public static void copyImages(final String docType,
                                  final String baseName,
                                  final String documentSrcName,
                                  final File sourceDirectory,
                                  final File outputDirectory)
            throws IOException {

        if (docType == null) {
            throw new IllegalArgumentException("Type of output document must not be null.");
        }


        if (documentSrcName == null) {
            throw new IllegalArgumentException(
                    "Top-level DocBook XML document source name must not be null.");
        }

        Set<String> docNames = NameUtils.getDocumentNames(
                sourceDirectory, documentSrcName);
        if (docNames.isEmpty()) {
            throw new IOException("No document names found.");
        }

        String extra = "";
        if (!(baseName == null) && !baseName.equalsIgnoreCase("")) {
            extra = File.separator + baseName;
        }

        FileFilter onlyImages = new SuffixFileFilter(IMAGE_FILE_SUFFIXES);

        for (String docName : docNames) {

            // Copy images specific to the document.
            File srcDir = FileUtils.getFile(sourceDirectory, docName, "images");
            File destDir = FileUtils.getFile(outputDirectory, docType, docName + extra, "images");
            if (srcDir.exists()) {
                FileUtils.copyDirectory(srcDir, destDir, onlyImages);
            }


            // Copy any shared images.
            String shared = "shared" + File.separator + "images";
            srcDir = new File(sourceDirectory, shared);
            destDir = FileUtils.getFile(outputDirectory, docType, docName + extra, shared);
            if (srcDir.exists()) {
                FileUtils.copyDirectory(srcDir, destDir, onlyImages);
            }
        }
    }

    private ImageCopier() {
        // Not used.
    }
}
