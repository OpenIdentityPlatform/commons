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

package org.forgerock.doc.maven.pre;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Replace {@code CURRENT.DOCID#} with the current document ID + #.
 * The current document ID is used to resolve olinks.
 */
public class CurrentDocId {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public CurrentDocId(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    private static final String CURRENT_DOCID = "CURRENT.DOCID#";

    /**
     * Replace {@code CURRENT.DOCID#} with the current document ID + #.
     *
     * @throws MojoExecutionException Failed to handle an XML source file.
     */
    public void execute() throws MojoExecutionException {

        final Set<String> docNames = m.getDocNames();
        final String sourceEncoding = m.getProject().getProperties()
                .getProperty("project.build.sourceEncoding", "UTF-8");

        for (String docName : docNames) {

            File documentDirectory = new File(m.getDocbkxModifiableSourcesDirectory(), docName);
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(documentDirectory);
            scanner.setIncludes(new String[] { "**/*.xml" });
            scanner.addDefaultExcludes();
            scanner.scan();

            for (String docFile : scanner.getIncludedFiles()) {
                try {
                    File documentFile = new File(documentDirectory, docFile);
                    String content = FileUtils.fileRead(documentFile, sourceEncoding);
                    String newContent = StringUtils.replace(content, CURRENT_DOCID, docName + "#");
                    FileUtils.fileWrite(documentFile, sourceEncoding, newContent);
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }
}
