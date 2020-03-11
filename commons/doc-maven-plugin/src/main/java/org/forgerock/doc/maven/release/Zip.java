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

package org.forgerock.doc.maven.release;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Zip release documents if configured to do so.
 *
 * <p>
 *
 * This zips the release layout only on one level,
 * and does not handle assembly of multiple zips
 * into a single documentation set .zip.
 */
public class Zip {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run the assembly plugin.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Zip(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Zip release documents.
     *
     * @throws MojoExecutionException Failed to zip documents.
     */
    public void execute() throws MojoExecutionException {
        executor.zip();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Zip release documents.
         *
         * @throws MojoExecutionException Failed to zip documents.
         */
        public void zip() throws MojoExecutionException {

            if (!m.doBuildReleaseZip()) {
                return;
            }

            final URL resource = getClass().getResource("/zip.xml");
            final File assembly = new File(m.getBuildDirectory(), "assembly.xml");

            try {
                FileUtils.copyURLToFile(resource, assembly);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }


            final String finalName = m.getProjectName() + "-" + m.getReleaseVersion();

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-assembly-plugin"),
                            version(m.getMavenAssemblyVersion())),
                    goal("single"),
                    configuration(
                            element(name("finalName"), finalName),
                            element(name("descriptors"),
                                    element(name("descriptor"), m.path(assembly)))),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
