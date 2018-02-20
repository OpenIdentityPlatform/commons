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
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.IOException;

/**
 * Add an index.html file to the release layout.
 */
public class IndexHtml {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public IndexHtml(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Add an index.html file to the release layout.
     *
     * @throws MojoExecutionException Failed to copy file.
     */
    public void execute() throws MojoExecutionException {
        if (!m.keepCustomIndexHtml()) {
            final File indexHtml = new File(m.getReleaseVersionPath(), "index.html");
            FileUtils.deleteQuietly(indexHtml);

            try {
                String content = IOUtils.toString(getClass().getResource("/dfo.index.html"), "UTF-8");
                content = content.replace("PRODUCT", m.getProjectName().toLowerCase());
                content = content.replace("VERSION", m.getReleaseVersion());

                FileUtils.writeStringToFile(indexHtml, content, "UTF-8");
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }
}
