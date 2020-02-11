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
 * Copyright 2014 ForgeRock AS
 */

package org.forgerock.doc.maven.post;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.SyntaxHighlighterCopier;

import java.io.IOException;

/**
 * XHTML post-processor.
 */
public class Xhtml {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Xhtml(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Add SyntaxHighlighter files for each XHTML document.
     *
     * @throws MojoExecutionException Failed to post-process XHTML.
     */
    public void execute() throws MojoExecutionException {

        String[] outputDirectories = new String[m.getDocNames().size()];

        int i = 0;
        for (final String docName : m.getDocNames()) {

            // Example: ${project.build.directory}/docbkx/xhtml/my-book
            outputDirectories[i] = FileUtils.getFile(
                    m.getDocbkxOutputDirectory(), "xhtml", docName).getPath();
            ++i;
        }

        SyntaxHighlighterCopier copier =
                new SyntaxHighlighterCopier(outputDirectories);
        try {
            copier.copy();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to copy files: " + e.getMessage(), e);
        }
    }
}
