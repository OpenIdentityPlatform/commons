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
 * Copyright 2014-2015 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Copy Bootstrap files under the specified directories.
 */
public final class BootstrapCopier {

    /**
     * Bootstrap logo files.
     */
    private final String[] bootstrapLogoResources = {
        "/logos/hero-bg-01.png",
        "/logos/left-shape-4.png",
        "/logos/Icon_External_Link.png",
        "/logos/forgerock-header-logo.png",
        "/logos/FR_logo_horiz_FC_rev.png"
    };

    /**
     * Bootstrap SWF files.
     */
    private final String[] bootstrapSwfResources = {
        "/swf/ZeroClipboard.swf"
    };

    /**
     * Directories where HtmlForBootstrap scripts, SWF and CSS are to be added.
     */
    private String[] outputDirectories;

    /**
     * Construct a BootstrapCopier, specifying output directories.
     *
     * @param outputDirectories Full path to directories under which to copy files.
     */
    public BootstrapCopier(final String[] outputDirectories) {
        this.outputDirectories = outputDirectories;
    }

    /**
     * For each outputDirectory, copy HtmlForBootstrap files under outputDirectory/sh.
     *
     * @throws java.io.IOException Failed to copy files.
     */
    public void copy() throws IOException {
        addBootstrapLogos();
        addBootstrapSwf();
    }

    /**
     * Add HtmlForBootstrap logo files in each output directory.
     *
     * @throws java.io.IOException Failed to add scripts.
     */
    private void addBootstrapLogos() throws IOException {
        addBootstrapResources(bootstrapLogoResources);
    }

    private void addBootstrapSwf() throws IOException {
        addBootstrapResources(bootstrapSwfResources);
    }

    /**
     * Add Bootstrap resource files in each output directory.
     *
     * @param resources List of resource files to copy.
     * @throws java.io.IOException Failed to files.
     */
    private void addBootstrapResources(final String[] resources) throws IOException {

        for (String resource : resources) {
            URL resourceUrl = getClass().getResource(resource);

            // The html.stylesheet parameter should probably take URLs.
            // When local files are referenced,
            // the DocBook XSL stylesheets do not copy the files.
            // Instead the files must be copied to the output directories.

            if (resourceUrl != null) {
                for (final String outputDirectory : outputDirectories) {
                    final File styleSheetFile = FileUtils.getFile(outputDirectory, "includes", resource);
                    FileUtils.copyURLToFile(resourceUrl, styleSheetFile);

                }
            } else {
                System.err.println("WARNING: Resource " + resource + " "
                    + "cannot be " + "found!");
            }
        }
    }
}
