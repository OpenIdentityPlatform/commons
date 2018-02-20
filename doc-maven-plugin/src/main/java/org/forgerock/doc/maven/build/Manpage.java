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

package org.forgerock.doc.maven.build;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;

/**
 * Build man page output.
 */
public class Manpage {

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
    public Manpage(final AbstractDocbkxMojo mojo) {
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
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Prepare olink target database from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build target database.
         */
        void prepareOlinkDB() throws MojoExecutionException {
            // No plans to implement this for man pages.
        }

        /**
         * Build documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void build() throws MojoExecutionException {
            ArrayList<Element> cfg = new ArrayList<MojoExecutor.Element>();
            cfg.addAll(m.getBaseConfiguration());
            cfg.add(element(name("includes"), "*/" + m.getDocumentSrcName()));
            cfg.add(element(name("manpagesCustomization"), m.path(m.getManpagesCustomization())));

            File manPageOutputDir = new File(m.getDocbkxOutputDirectory(), "manpages");
            cfg.add(element(name("targetDirectory"), m.path(manPageOutputDir)));

            executeMojo(
                    plugin(
                            groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-maven-plugin"),
                            version(m.getDocbkxVersion())),
                    goal("generate-manpages"),
                    configuration(cfg.toArray(new Element[cfg.size()])),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

            // Man page generation replaces spaces in path name with underscores.
            // If necessary, this is corrected during post-processing.
            m.getLog().info("Man page output directory: "
                    + manPageOutputDir.getAbsolutePath().replace(' ', '_'));
        }
    }
}
