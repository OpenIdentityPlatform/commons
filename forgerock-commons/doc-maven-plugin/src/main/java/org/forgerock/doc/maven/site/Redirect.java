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

package org.forgerock.doc.maven.site;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.IOException;

/**
 * Add file to redirect {@code /doc/index.html} to {@code /docs.html}.
 */
public class Redirect {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Redirect(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Add file to redirect {@code /doc/index.html} to {@code /docs.html}.
     *
     * @throws MojoExecutionException Failed to write file.
     */
    public void execute() throws MojoExecutionException {
        try {
            File file = FileUtils.getFile(m.getSiteDirectory(), "doc", "index.html");
            if (!file.exists()) {
                String redirect = IOUtils.toString(getClass().getResourceAsStream("/index.html"), "UTF-8");
                redirect = redirect.replaceAll("PROJECT", m.getProjectName())
                        .replaceAll("LOWERCASE", m.getProjectName().toLowerCase());
                FileUtils.write(file, redirect, "UTF-8");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy redirect file: " + e.getMessage(), e);
        }
    }
}
