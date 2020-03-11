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

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Fix favicon links in release HTML.
 */
public class Favicon {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Favicon(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Fix favicon links in release HTML.
     *
     * @throws MojoExecutionException Failed to fix links.
     */
    public void execute() throws MojoExecutionException {

        HashMap<String, String> replacements = new HashMap<String, String>();
        final String oldFaviconLink = m.getFaviconLink();
        final String newFaviconLink = m.getReleaseFaviconLink();

        if (!oldFaviconLink.equalsIgnoreCase(newFaviconLink)) {
            replacements.put(oldFaviconLink, newFaviconLink);
            try {
                HtmlUtils.updateHtml(m.getReleaseVersionPath(), replacements);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }
}
