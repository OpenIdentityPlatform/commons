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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.doc.maven.pre;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Converts Asciidoc source files to DocBook 5 XML.
 */
public class AsciidocToDocBook {

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
    public AsciidocToDocBook(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Copies arbitrary resources from sources to pre-site output for HTML formats.
     *
     * @throws MojoExecutionException Failed to copy files
     */
    public void execute() throws MojoExecutionException {
        executor.convert();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Converts Asciidoc source files to DocBook 5 XML.
         *
         * @throws MojoExecutionException   Failed to convert files
         */
        public void convert() throws MojoExecutionException {
            if (!m.getAsciidocSourceDirectory().exists()) {
                return;
            }

            executeMojo(
                    plugin(
                            groupId("org.asciidoctor"),
                            artifactId("asciidoctor-maven-plugin"),
                            version(m.getAsciidoctorPluginVersion())),
                    goal("process-asciidoc"),
                    configuration(
                            element("baseDir", m.path(m.getAsciidocSourceDirectory())),
                            element("outputDirectory", m.path(m.getDocbkxModifiableSourcesDirectory())),
                            element("preserveDirectories", "true"),
                            element("extensions",
                                    element("extension", ".ad"),
                                    element("extension", ".adoc"),
                                    element("extension", ".asciidoc"),
                                    element("extension", ".txt")),
                            element("backend", "docbook"),
                            element("doctype", "book")),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
