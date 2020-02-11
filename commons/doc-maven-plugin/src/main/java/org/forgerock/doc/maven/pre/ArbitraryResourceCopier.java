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

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies arbitrary resources from sources to pre-site output for HTML formats.
 */
public class ArbitraryResourceCopier {

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
    public ArbitraryResourceCopier(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Copies arbitrary resources from sources to pre-site output for HTML formats.
     *
     * @throws MojoExecutionException   Failed to copy files
     */
    public void execute() throws MojoExecutionException {
        executor.copy();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Copies arbitrary resources from sources to pre-site output for HTML formats.
         *
         * @throws MojoExecutionException   Failed to copy files
         */
        public void copy() throws MojoExecutionException {
            if (m.doCopyResourceFiles() && m.getResourcesDirectory().exists()) {

                List<String> directories = new ArrayList<>();
                if (m.getFormats().contains(AbstractDocbkxMojo.Format.bootstrap)) {
                    directories.add("bootstrap");
                }

                if (m.getFormats().contains(AbstractDocbkxMojo.Format.html)) {
                    directories.add("html");
                }

                if (m.getFormats().contains(AbstractDocbkxMojo.Format.webhelp)) {
                    directories.add("webhelp");
                }

                if (m.getFormats().contains(AbstractDocbkxMojo.Format.xhtml5)) {
                    directories.add("xhtml");
                }

                try {
                    for (String directory : directories) {
                        File targetDirectory = FileUtils.getFile(m.getDocbkxOutputDirectory(), directory);
                        FileUtils.copyDirectoryToDirectory(m.getResourcesDirectory(), targetDirectory);
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy resources", e);
                }
            }

        }
    }
}
