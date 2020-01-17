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

package org.forgerock.doc.maven.backstage;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;

import java.io.File;
import java.io.IOException;

/**
 * Unpack documentation artifacts and prepare them for Backstage.
 */
public class ArtifactDocs {
    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The base directory under which to unpack doc artifacts.
     */
    private File baseDirectory;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo          The Mojo that holds the configuration.
     */
    public ArtifactDocs(final AbstractDocbkxMojo mojo) {
        m = mojo;
        baseDirectory = new File(m.getBackstageDirectory(), "apidocs");
    }

    /**
     * Unpack documentation artifacts and prepare them for Backstage.
     *
     * @throws MojoExecutionException   Failed to handle an artifact.
     */
    public void execute() throws MojoExecutionException {
        for (ArtifactItem artifactItem : m.getArtifactItems()) {
            final File outputDirectory = new File(baseDirectory, artifactItem.getOutputDirectory());

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-dependency-plugin"),
                            version(m.getMavenDependencyVersion())),
                    goal("unpack"),
                    configuration(
                            element("artifactItems",
                                    element("artifactItem",
                                            element("groupId", artifactItem.getGroupId()),
                                            element("artifactId", artifactItem.getArtifactId()),
                                            element("version", artifactItem.getVersion()),
                                            element("type", artifactItem.getType()),
                                            element("classifier", artifactItem.getClassifier()),
                                            element("overWrite", "true"),
                                            element("outputDirectory", outputDirectory.getPath()),
                                            element("includes", "**/*.*")))),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

            writeDocsetJson(artifactItem.getTitle(), outputDirectory);
        }
    }

    /**
     * Writes a {@code meta.json} file to the specified directory.
     * @param title     The title for the document.
     * @param directory The directory in which to write the file.
     * @throws MojoExecutionException   Failed to write {@code meta.json} file.
     */
    private void writeDocsetJson(final String title, final File directory) throws MojoExecutionException {
        if (title == null || title.isEmpty()) {
            throw new MojoExecutionException("Document title must be set for the artifact.");
        }

        final String json = "{\"title\":\"" + title + "\"}";
        final File file = new File(directory, "meta.json");
        try {
            FileUtils.writeStringToFile(file, json);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write :" + file.getPath());
        }
    }
}
