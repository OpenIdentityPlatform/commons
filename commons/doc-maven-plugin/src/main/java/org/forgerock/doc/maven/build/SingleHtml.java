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

package org.forgerock.doc.maven.build;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.ImageCopier;
import org.forgerock.doc.maven.utils.OLinkUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Build single-page HTML output.
 */
public class SingleHtml {

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
    public SingleHtml(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Build documents from DocBook XML sources.
     *
     * @throws MojoExecutionException Failed to build output.
     */
    public void execute() throws MojoExecutionException {
        executor.prepareOlinkDB();
        executor.build();
    }

    /**
     * Get absolute path to an Olink target database XML document
     * that points to the individual generated Olink DB files,
     * for single-page HTML.
     *
     * @return Absolute path to the file.
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String getTargetDB() throws MojoExecutionException {
        File targetDB = new File(m.getBuildDirectory(), "olinkdb-single-page-html.xml");

        try {
            OLinkUtils.createTargetDatabase(targetDB, "html", m);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + targetDB.getPath(), e);
        }

        return targetDB.getPath();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Prepare olink target database from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build target database.
         */
        void prepareOlinkDB() throws MojoExecutionException {

            for (String docName : m.getDocNames()) {
                ArrayList<Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.add(element(name("xincludeSupported"), m.isXincludeSupported()));
                cfg.add(element(name("sourceDirectory"), m.path(m.getDocbkxModifiableSourcesDirectory())));
                cfg.add(element(name("chunkedOutput"), "false"));
                cfg.add(element(name("htmlCustomization"), m.path(m.getSingleHTMLCustomization())));
                cfg.add(element(name("collectXrefTargets"), "only"));

                cfg.add(element(name("includes"), docName + "/" + m.getDocumentSrcName()));
                cfg.add(element(name("targetDirectory"), m.path(m.getDocbkxOutputDirectory()) + "/html"));
                cfg.add(element(name("targetsFilename"), m.getDocumentSrcName() + ".html.target.db"));

                executeMojo(
                        plugin(
                                groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(m.getDocbkxVersion())),
                        goal("generate-html"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
            }
        }

        /**
         * Build documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void build() throws MojoExecutionException {

            try {
                ImageCopier.copyImages("html", "", m);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy images", e);
            }

            ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
            cfg.addAll(m.getBaseConfiguration());
            cfg.add(element(name("includes"), "*/" + m.getDocumentSrcName()));
            cfg.add(element(name("chunkedOutput"), "false"));
            cfg.add(element(name("htmlCustomization"), m.path(m.getSingleHTMLCustomization())));
            cfg.add(element(name("targetDatabaseDocument"), getTargetDB()));
            cfg.add(element(name("targetDirectory"), m.path(m.getDocbkxOutputDirectory()) + "/html"));

            executeMojo(
                    plugin(
                            groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-maven-plugin"),
                            version(m.getDocbkxVersion())),
                    goal("generate-html"),
                    configuration(cfg.toArray(new Element[cfg.size()])),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
