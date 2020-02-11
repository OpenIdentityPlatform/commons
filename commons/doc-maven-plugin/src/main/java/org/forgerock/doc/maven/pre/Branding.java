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

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Unpack branding elements.
 * The branding is unpacked from a separate artifact.
 */
public class Branding {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run the dependency plugin.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Branding(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Augment the modifiable copy of DocBook XML sources with branding elements.
     *
     * @throws MojoExecutionException Failed to unpack branding.
     */
    public void execute() throws MojoExecutionException {
        executor.unpack();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Unpack branding elements from the branding artifact.
         *
         * @throws MojoExecutionException Failed to unpack branding.
         */
        public void unpack() throws MojoExecutionException {
            final String outputDir = m.path(m.getBuildDirectory());

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-dependency-plugin"),
                            version(m.getMavenDependencyVersion())),
                    goal("unpack"),
                    configuration(
                            element("artifactItems",
                                    element("artifactItem",
                                            element("groupId", m.getBrandingGroupId()),
                                            element("artifactId", m.getBrandingArtifactId()),
                                            element("version", m.getBrandingVersion()),
                                            element("type", "jar"),
                                            element("overWrite", "true"),
                                            element("outputDirectory", outputDir),
                                            element("includes", "**/*.*")))),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
