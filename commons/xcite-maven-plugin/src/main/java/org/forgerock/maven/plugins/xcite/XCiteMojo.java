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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.xcite;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.forgerock.maven.plugins.xcite.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Copy quotes from source text files into target text files.
 * @goal cite
 */
@Mojo(name = "cite", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class XCiteMojo extends AbstractMojo {

    /**
     * Whether to escape XML characters in quotes.
     */
    @Parameter (defaultValue = "false")
    private boolean escapeXml;

    /**
     * Filter strings specifying files with citations to include.
     */
    @Parameter
    private String[] includes;

    /**
     * Filter strings specifying files to exclude.
     */
    @Parameter
    private String[] excludes;

    /**
     * Indent quotes this number of spaces from the left margin.
     */
    @Parameter (defaultValue = "0")
    private int reindent;

    /**
     * Output directory for files with quotes.
     */
    @Parameter (defaultValue = "${project.build.directory}/xcite")
    private File outputDirectory;

    /**
     * Source directory for files with citations.
     */
    @Parameter (defaultValue = "${basedir}/src/main")
    private File sourceDirectory;

    /**
     * Replace citations with quotes in included files,
     * writing the resulting files in the output directory.
     *
     * @throws MojoExecutionException   Could not create output directory.
     * @throws MojoFailureException     Failed to perform replacements.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException(
                        "Failed to create output directory: "
                                + outputDirectory.getPath());
            }
        }

        String[] files = FileUtils.getIncludedFiles(sourceDirectory, includes, excludes);
        Resolver resolver =
                new Resolver(outputDirectory, escapeXml, reindent, true);
        try {
            resolver.resolve(sourceDirectory, files);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}
