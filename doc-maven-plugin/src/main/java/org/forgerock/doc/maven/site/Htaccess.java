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
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;

import java.io.File;
import java.io.IOException;

/**
 * Add {@code .htaccess} file to the site layout.
 */
public class Htaccess {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Htaccess(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Add {@code .htaccess} file to the site layout.
     *
     * @throws MojoExecutionException Failed to copy file.
     */
    public void execute() throws MojoExecutionException {
        final String layoutDir = new File(m.getSiteDirectory(), "doc").getPath();
        final File htaccess = new File(m.getBuildDirectory(), ".htaccess");

        FileUtils.deleteQuietly(htaccess);
        try {
            FileUtils.copyURLToFile(getClass().getResource("/.htaccess"), htaccess);
            HtmlUtils.addHtaccess(layoutDir, htaccess);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy .htaccess: " + e.getMessage(), e);
        }
    }
}
