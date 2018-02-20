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
import org.forgerock.doc.maven.utils.HtmlUtils;

import java.io.IOException;

/**
 * Add custom CSS for the normal, non-release build.
 */
public class CustomCss {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public CustomCss(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Add custom CSS to the modifiable copy of DocBook XML sources.
     *
     * @throws MojoExecutionException Failed to add CSS.
     */
    public void execute() throws MojoExecutionException {
        try {
            HtmlUtils.addCustomCss(
                    m.getPreSiteCss(),
                    m.getDocbkxModifiableSourcesDirectory(),
                    m.getDocumentSrcName());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
