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
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;

/**
 * Rename PDF files in the release layout.
 */
public class PdfNames {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public PdfNames(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Rename PDF files in the release layout.
     *
     * @throws MojoExecutionException Failed to rename files.
     */
    public void execute() throws MojoExecutionException {
        final File dir = new File(m.getReleaseVersionPath());
        final String[] ext = {"pdf"};

        for (File pdf : FileUtils.listFiles(dir, ext, false)) { // Not recursive
            String name = pdf.getName().replaceFirst("-", "-" + m.getReleaseVersion() + "-");
            if (!pdf.renameTo(new File(pdf.getParent() + File.separator + name))) {
                throw new MojoExecutionException("Failed to rename PDF: " + name);
            }
        }
    }
}
