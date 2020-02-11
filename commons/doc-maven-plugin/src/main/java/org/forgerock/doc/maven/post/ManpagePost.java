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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.post;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.IOException;

/**
 * Fix man page file locations.
 */
public class ManpagePost extends AbstractDocbkxMojo {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public ManpagePost(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Fix man page file locations.
     * <br>
     * Man page generation replaces spaces in path name with underscores.
     * As a result, if man pages are built in a project directory like
     * {@code /path/to/My Doc Project},
     * then the generated man pages end up by default in
     * {@code /path/to/My_Doc_Project/target/docbkx/manpages}.
     * <br>
     * This method copies the result to the expected location.
     * <br>
     * This method then attempts to remove the extra generated directory,
     * though failure to remove the extra directory only logs an informational message
     * and does no throw an exception.
     *
     * @throws MojoExecutionException   Failed to copy files.
     */
    public void execute() throws MojoExecutionException {
        File manPageOutputDir = new File(m.getDocbkxOutputDirectory(), "manpages");
        File generatedManPageDir = new File(manPageOutputDir.getAbsolutePath().replace(' ', '_'));
        if (generatedManPageDir.equals(manPageOutputDir)) {
            return; // Nothing to copy, and do not delete the man page directory.
        }
        if (!generatedManPageDir.exists() || !hasChildren(generatedManPageDir)) {
            m.getLog().info("No man pages found in " + generatedManPageDir.getAbsolutePath());
            return; // No man pages. Nothing to do.
        }

        // The generated man page dir is different from the expected man page output dir,
        // so copy the content to the expected location and try to delete the generated dir.
        try {
            FileUtils.copyDirectory(generatedManPageDir, manPageOutputDir, FileFilterUtils.trueFileFilter());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        File topGeneratedDirWithUnderscore = getTopAncestorWithUnderscore(generatedManPageDir);
        try {
            FileUtils.deleteDirectory(topGeneratedDirWithUnderscore);
        } catch (IOException e) {
            m.getLog().info("Failed to delete generated man page dir: " + generatedManPageDir + e.getMessage());
        }
    }

    /**
     * Returns true if the specified directory has children.
     * @param directory The directory to check.
     * @return True if the specified directory has children.
     */
    private boolean hasChildren(final File directory) {
        String[] children = directory.list();
        return children.length > 0;
    }

    /**
     * Returns the top ancestor directory that contains an underscore in the path.
     * @param directory Returns this directory or an ancestor.
     * @return The top ancestor directory that contains an underscore in the path.
     */
    private File getTopAncestorWithUnderscore(final File directory) {
        File topAncestorWithUnderscore = directory;
        while (topAncestorWithUnderscore.getParent().contains("_")) {
            topAncestorWithUnderscore = topAncestorWithUnderscore.getParentFile();
        }
        return topAncestorWithUnderscore;
    }
}
