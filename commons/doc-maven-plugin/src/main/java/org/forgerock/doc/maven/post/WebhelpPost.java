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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.post;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Webhelp post-processor for both single-page and chunked HTML formats.
 */
public class WebhelpPost {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public WebhelpPost(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Post-processes HTML formats.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException Failed to post-process HTML.
     */
    public void execute() throws MojoExecutionException {

        final File webhelpDir = new File(m.getDocbkxOutputDirectory(), "webhelp");

        HashMap<String, String> replacements = new HashMap<String, String>();

        try {
            // See https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag
            String robots = "<head>" + System.getProperty("line.separator")
                    + IOUtils.toString(getClass().getResourceAsStream("/robots.txt"), "UTF-8");
            replacements.put("<head>", robots);
            HtmlUtils.updateHtml(webhelpDir.getPath(), replacements);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to update webhelp with nofollow,noindex tag", e);
        }
    }
}
