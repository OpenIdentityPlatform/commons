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
 * Copyright 2024 3A Systems LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractAsciidocMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The {@code MavenSession} object, which is read-only.
     */
    @Parameter(property = "session", required = true, readonly = true)
    protected MavenSession session;

    @Component
    protected BuildPluginManager pluginManager;


    /**
     * Short name of the project, such as OpenAM, OpenDJ, OpenIDM.
     */
    @Parameter(property = "projectName", required = true)
    protected String projectName;

    /**
     * Project version.
     */
    @Parameter(property = "projectVersion", required = true)
    protected String projectVersion;

    /**
     * Version for this release.
     */
    @Parameter(property = "releaseVersion", required = true)
    protected String releaseVersion;


    /**
     * The project build directory.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}}.
     */
    @Parameter(defaultValue = "${project.build.directory}/asciidoc")
    protected File buildDirectory;

    @Parameter(defaultValue = "${basedir}/src/main/asciidoc")
    private File asciidocSourceDirectory;

    protected File getAsciidocSourceDirectory() throws MojoExecutionException {
        if(asciidocSourceDirectory == null) {
            throw new MojoExecutionException("asciidoc sourcer directory should not be empty");
        }
        return asciidocSourceDirectory;
    }

    protected File getAsciidocBuildSourceDirectory() throws MojoExecutionException {
        return new File(buildDirectory, "/source");
    }


    @Parameter(property = "documents", required = true)
    private List<String> documents;

    public static Set<String> ignoreFolders = new HashSet<>(Arrays.asList("partials", "images"));
    protected List<String> getDocuments() throws MojoExecutionException {
        if(documents == null || documents.size() == 0) {
            documents = Arrays.stream(getAsciidocSourceDirectory().listFiles())
                    .filter(f -> !ignoreFolders.contains(f.getName()))
                    .map(File::getName).collect(Collectors.toList());
        }
        if(documents.size() == 0) {
            throw new MojoExecutionException("At least one document should be set");
        }
        return documents;
    }

    /**
     * Base directory for built documentation.
     *
     * <br>
     *
     * Value: {@code ${project.build.directory}/antora}
     *
     * @return The base directory for built documentation.
     */
    public File getAntoraOutputDirectory() {
        return new File(buildDirectory, "/antora");
    }

}
