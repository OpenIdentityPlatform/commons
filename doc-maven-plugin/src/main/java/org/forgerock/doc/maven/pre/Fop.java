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

package org.forgerock.doc.maven.pre;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Prepare fonts for use with Apache FOP.
 */
public final class Fop {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run docbkx-tools.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Fop(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Prepare fonts for use with Apache FOP.
     *
     * @throws MojoExecutionException Failed to prepare to use FOP.
     */
    public void execute() throws MojoExecutionException {
        executor.copyFonts();
        executor.generateFontMetrics();
    }

    /**
     * Return a fonts element that includes all the custom fonts.
     *
     * @param fontDir Directory containing the custom fonts.
     * @return Fonts element.
     */
    public static MojoExecutor.Element getFontsElement(final String fontDir) {
        return element(
                name("fonts"),
                element(name("font"),
                        element(name("name"), "DejaVuSans"),
                        element(name("style"), "normal"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSans.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSans-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSans"),
                        element(name("style"), "normal"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSansCondensed-Bold.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansCondensed-Bold-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSans"),
                        element(name("style"), "italic"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSans-Oblique.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSans-Oblique-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSans"),
                        element(name("style"), "italic"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSansCondensed-BoldOblique.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansCondensed-BoldOblique-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSansMono"),
                        element(name("style"), "normal"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSansMono.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansMono-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSansMono"),
                        element(name("style"), "normal"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSansMono-Bold.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansMono-Bold-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSansMono"),
                        element(name("style"), "italic"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSansMono-Oblique.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansMono-Oblique-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSansMono"),
                        element(name("style"), "italic"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSansMono-BoldOblique.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSansMono-BoldOblique-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSerif"),
                        element(name("style"), "normal"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSerif.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSerif-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSerif"),
                        element(name("style"), "normal"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSerifCondensed-Bold.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSerifCondensed-Bold-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSerif"),
                        element(name("style"), "italic"),
                        element(name("weight"), "normal"),
                        element(name("embedFile"), fontDir + "/DejaVuSerif-Italic.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSerif-Italic-metrics.xml")),
                element(name("font"),
                        element(name("name"), "DejaVuSerif"),
                        element(name("style"), "italic"),
                        element(name("weight"), "bold"),
                        element(name("embedFile"), fontDir + "/DejaVuSerifCondensed-BoldItalic.ttf"),
                        element(name("metricsFile"), fontDir + "/DejaVuSerifCondensed-BoldItalic-metrics.xml")));
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Copy fonts for use when generating FO output.
         *
         * @throws MojoExecutionException Failed to copy fonts.
         */
        public void copyFonts() throws MojoExecutionException {
            String[] fonts = {"/fonts/DejaVuSans-Oblique.ttf",
                "/fonts/DejaVuSans.ttf",
                "/fonts/DejaVuSansCondensed-Bold.ttf",
                "/fonts/DejaVuSansCondensed-BoldOblique.ttf",
                "/fonts/DejaVuSansMono-Bold.ttf",
                "/fonts/DejaVuSansMono-BoldOblique.ttf",
                "/fonts/DejaVuSansMono-Oblique.ttf",
                "/fonts/DejaVuSansMono.ttf",
                "/fonts/DejaVuSerif-Italic.ttf",
                "/fonts/DejaVuSerif.ttf",
                "/fonts/DejaVuSerifCondensed-Bold.ttf",
                "/fonts/DejaVuSerifCondensed-BoldItalic.ttf"};

            for (String font : fonts) {
                final URL source = getClass().getResource(font);
                final File destination = new File(
                        m.getBuildDirectory(), font.replaceAll("/", File.separator));
                try {
                    FileUtils.copyURLToFile(source, destination);
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Failed to copy file: " + font + ". " + e.getMessage());
                }
            }
        }

        /**
         * Generate font metrics files.
         *
         * @throws MojoExecutionException Failed to generate font metrics.
         */
        public void generateFontMetrics() throws MojoExecutionException {

            final String fontsDir = m.path(m.getFontsDirectory());

            executeMojo(
                    plugin(
                            groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-fop-support"),
                            version(m.getDocbkxVersion())),
                    goal("generate"),
                    configuration(
                            element(name("ansi"), m.useAnsi()),
                            element(name("sourceDirectory"), fontsDir),
                            element(name("targetDirectory"), fontsDir)),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
