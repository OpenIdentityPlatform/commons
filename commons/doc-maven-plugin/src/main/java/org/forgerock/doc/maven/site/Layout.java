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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.site;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.AbstractDocbkxMojo.Format;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lay out built documents,
 * by default under {@code ${project.build.directory}/site/doc/}.
 */
public class Layout {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run the resources plugin.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Layout(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Lay out built documents.
     *
     * @throws MojoExecutionException Failed to layout site.
     */
    public void execute() throws MojoExecutionException {
        executor.layout();
    }

    /**
     * Get element specifying built documents to copy to the site directory.
     *
     * <p>
     *
     * Man pages are not currently copied anywhere.
     *
     * <p>
     *
     * Webhelp is handled separately.
     *
     * @return Compound element specifying built documents to copy.
     * @throws MojoExecutionException Something went wrong getting document names.
     */
    private MojoExecutor.Element getResources() throws MojoExecutionException {

        ArrayList<MojoExecutor.Element> r = new ArrayList<MojoExecutor.Element>();

        final List<Format> formats = m.getFormats();
        final String outputDir = m.path(m.getDocbkxOutputDirectory());

        if (formats.contains(Format.epub)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/epub/"),
                    element(name("includes"),
                            element(name("include"), "**/*.epub"))));
        }

        if (formats.contains(Format.html)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/"),
                    element(name("includes"),
                            element(name("include"), "html/**/*.*"))));
        }

        // Man pages are not currently copied anywhere.

        if (formats.contains(Format.pdf)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/pdf/"),
                    element(name("includes"),
                            element(name("include"), "**/*.pdf"))));
        }

        if (formats.contains(Format.rtf)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/rtf/"),
                    element(name("includes"),
                            element(name("include"), "**/*.rtf"))));
        }

        // Webhelp is handled separately.

        if (formats.contains(Format.xhtml5)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/"),
                    element(name("includes"),
                            element(name("include"), "xhtml/**/*.*"))));
        }

        if (formats.contains(Format.bootstrap)) {
            r.add(element(name("resource"),
                    element(name("directory"), outputDir + "/"),
                    element(name("includes"),
                            element(name("include"), "bootstrap/**/*.*"))));
        }

        return element("resources", r.toArray(new MojoExecutor.Element[r.size()]));
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Lay out built documents.
         *
         * @throws MojoExecutionException Failed to lay out documents.
         */
        public void layout() throws MojoExecutionException {

            final String siteDocDirectory = m.path(m.getSiteDirectory()) + "/doc";

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-resources-plugin"),
                            version(m.getMavenResourcesVersion())),
                    goal("copy-resources"),
                    configuration(
                            element(name("encoding"), "UTF-8"),
                            element(name("outputDirectory"), siteDocDirectory),
                            getResources()),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

            // The webhelp directory needs to be copied in its entirety
            // to avoid overwriting other HTML.

            if (m.getFormats().contains(Format.webhelp)) {

                // 2.0.15 does not allow <webhelpBaseDir> to be set,
                // so the output location is hard-coded for now.
                final String webHelpDir = m.path(m.getDocbkxOutputDirectory()) + "/webhelp";

                executeMojo(
                        plugin(
                                groupId("org.apache.maven.plugins"),
                                artifactId("maven-resources-plugin"),
                                version(m.getMavenResourcesVersion())),
                        goal("copy-resources"),
                        configuration(
                                element(name("encoding"), "UTF-8"),
                                element(name("outputDirectory"), siteDocDirectory + "/webhelp"),
                                element(name("resources"),
                                        element(name("resource"),
                                                element(name("directory"), webHelpDir),
                                                element(name("excludes"),
                                                        element(name("exclude"), "**/*.target.db"))))),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
            }

            // Optionally copy the entire directory of arbitrary resources
            // to the directories containing HTML format docs.
            if (m.doCopyResourceFiles() && m.getResourcesDirectory().exists()) {

                List<String> directories = new ArrayList<String>();
                if (m.getFormats().contains(Format.bootstrap)) {
                    directories.add("bootstrap");
                }

                if (m.getFormats().contains(Format.html)) {
                    directories.add("html");
                }

                if (m.getFormats().contains(Format.webhelp)) {
                    directories.add("webhelp");
                }

                if (m.getFormats().contains(Format.xhtml5)) {
                    directories.add("xhtml");
                }

                try {
                    for (String directory : directories) {
                        File targetDirectory = FileUtils.getFile(m.getSiteDirectory(), "doc", directory);
                        FileUtils.copyDirectoryToDirectory(m.getResourcesDirectory(), targetDirectory);
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy resources", e);
                }
            }
        }
    }
}
