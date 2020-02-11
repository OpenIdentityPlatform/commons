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

package org.forgerock.doc.maven.pre;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Use <a href="http://plantuml.sourceforge.net/">PlantUML</a> to generate images.
 *
 * <p>
 *
 * This class expects .txt files in the DocBook XML sources
 * that contain PlantUML diagrams.
 *
 * <p>
 *
 * It transforms the files to images in the same directories as the files.
 */
public class PlantUml {

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
    public PlantUml(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Run PlantUML on .txt files in the DocBook source files.
     *
     * @throws MojoExecutionException Failed to run PlantUML.
     */
    public void execute() throws MojoExecutionException {

        // JCite to a temporary directory...
        executor.runPlantUml();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Run PlantUML on .txt files in the DocBook source files.
         *
         * @throws MojoExecutionException Failed to run PlantUml.
         */
        void runPlantUml() throws MojoExecutionException {

            final String directory = m.path(m.getDocbkxModifiableSourcesDirectory());

            executeMojo(
                    plugin(
                            groupId("com.github.jeluard"),
                            artifactId("plantuml-maven-plugin"),
                            version("1.0"),
                            dependencies(
                                    dependency(
                                            groupId("net.sourceforge.plantuml"),
                                            artifactId("plantuml"),
                                            version(m.getPlantUmlVersion())))),
                    goal("generate"),
                    configuration(
                            element(name("sourceFiles"),
                                    element(name("directory"), directory),
                                    element(name("includes"),
                                            element(name("include"), "**/*.txt"))),
                            element(name("outputInSourceDirectory"), "true"),
                            element(name("verbose"), "false")),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
