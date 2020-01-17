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
 * Copyright 2013-2014 ForgeRock AS
 */

package org.forgerock.doc.maven.pre;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.FilteredFileCopier;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;

/**
 * Use <a href="http://arrenbrecht.ch/jcite/">JCite</a> to quote Java code.
 *
 * <p>
 *
 * This class generates source including the citations.
 * For example, if your DocBook source file includes
 * the following &lt;programlisting&gt;:
 *
 * <pre>
 * &lt;programlisting language=&quot;java&quot;
 * &gt;[jcp:org.forgerock.doc.jcite.test.Test:--- mainMethod]&lt;/programlisting&gt;
 * </pre>
 *
 * <p>
 *
 * Then this class replaces the citation with the code
 * in between {@code // --- mainMethod} comments,
 * suitable for inclusion in XML,
 * and leaves the new file with the modifiable copy of the sources
 * for further processing.
 */
public class JCite {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run JCite.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public JCite(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
        sourceDir = m.path(m.getDocbkxModifiableSourcesDirectory());
        tempOutputDirectory = new File(m.getBuildDirectory(), "docbkx-jcite");
        outputDir = m.path(tempOutputDirectory);
    }

    // JCite the sources in the modifiable copy.
    private final String sourceDir;

    // JCite to a temporary directory.
    private final File tempOutputDirectory;
    private final String outputDir;

    /**
     * Run JCite on the XML source files.
     *
     * @throws MojoExecutionException Failed to run JCite.
     */
    public void execute() throws MojoExecutionException {

        // JCite to a temporary directory...
        executor.runJCite();

        // ...and then overwrite the copy of sources with the new files.
        try {
            FilteredFileCopier.copyOthers(
                    ".xml", m.getDocbkxModifiableSourcesDirectory(), tempOutputDirectory);
            FileUtils.copyDirectory(tempOutputDirectory, m.getDocbkxModifiableSourcesDirectory());
            FileUtils.deleteDirectory(tempOutputDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Run JCite on the DocBook XML source files.
         *
         * @throws MojoExecutionException Failed to run JCite.
         */
        void runJCite() throws MojoExecutionException {

            // mojo-executor lacks fluent support for element attributes.
            // You can hack around this by including attributes
            // in the name of elements without children.
            // But the hack does not work for elements with children:
            // SAX barfs on closing tags containing a bunch of attributes.
            Xpp3Dom mkdir = new Xpp3Dom("mkdir");
            mkdir.setAttribute("dir", outputDir);

            Xpp3Dom taskdef = new Xpp3Dom("taskdef");
            taskdef.setAttribute("name", "jcite");
            taskdef.setAttribute("classname", "ch.arrenbrecht.jcite.JCiteTask");
            taskdef.setAttribute("classpathref", "maven.plugin.classpath");

            Xpp3Dom jcite = new Xpp3Dom("jcite");
            jcite.setAttribute("srcdir", sourceDir);
            jcite.setAttribute("destdir", outputDir);

            // Might have multiple paths to sources.
            Xpp3Dom sourcepath = new Xpp3Dom("sourcepath");
            if (m.getJCiteSourcePaths() != null && !m.getJCiteSourcePaths().isEmpty()) {
                for (File sourcePath : m.getJCiteSourcePaths()) {
                    String location = FilenameUtils
                            .separatorsToSystem(sourcePath.getPath());
                    Xpp3Dom pathelement = new Xpp3Dom("pathelement");
                    pathelement.setAttribute("location", location);
                    sourcepath.addChild(pathelement);
                }
            } else { // No source path defined. Try src/main/java.
                Xpp3Dom pathelement = new Xpp3Dom("pathelement");
                pathelement.setAttribute("location", "src/main/java");
                sourcepath.addChild(pathelement);
            }
            jcite.addChild(sourcepath);

            Xpp3Dom include = new Xpp3Dom("include");
            include.setAttribute("name", "**/*.xml");
            jcite.addChild(include);

            Xpp3Dom target = new Xpp3Dom("target");
            target.addChild(mkdir);
            target.addChild(taskdef);
            target.addChild(jcite);

            Xpp3Dom configuration = new Xpp3Dom("configuration");
            configuration.addChild(target);

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-antrun-plugin"),
                            version("1.7"),
                            dependencies(
                                    dependency(
                                    // See https://code.google.com/r/markcraig-jcite/.
                                            groupId("org.mcraig"),
                                            artifactId("jcite"),
                                            version(m.getJCiteVersion())))),
                    goal("run"),
                    configuration,
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }
    }
}
