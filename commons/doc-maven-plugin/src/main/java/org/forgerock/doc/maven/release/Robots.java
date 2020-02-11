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

package org.forgerock.doc.maven.release;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Remove robots meta tag in release HTML.
 */
public class Robots {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Robots(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Remove robots meta tag in release HTML.
     *
     * @throws MojoExecutionException Failed to remove tags.
     */
    public void execute() throws MojoExecutionException {

        HashMap<String, String> replacements = new HashMap<String, String>();

        try {
            final String robots = IOUtils.toString(
                    getClass().getResourceAsStream("/robots.txt"), "UTF-8");

            replacements.put(robots, ""); // Replace the tag with an empty string.
            HtmlUtils.updateHtml(m.getReleaseVersionPath(), replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
