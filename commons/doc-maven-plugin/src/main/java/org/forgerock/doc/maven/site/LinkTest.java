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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.site;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;

/**
 * Test links in pre-processed copy of the sources.
 *
 * <p>
 *
 * Errors are written by default to {@code ${project.build.directory}/docbkx/linktester.err}.
 * The test does not fail on error.
 */
public class LinkTest {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run the linktester plugin.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public LinkTest(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Test links in pre-processed copy of the sources.
     *
     * @throws MojoExecutionException Failed to complete link tests.
     */
    public void execute() throws MojoExecutionException {
        executor.test();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Run link tester.
         *
         * @throws MojoExecutionException Failed to run link tester.
         */
        public void test() throws MojoExecutionException {

            if (m.runLinkTester().equalsIgnoreCase("false")) {
                return;
            }

            final String log = m.path(new File(m.getDocbkxOutputDirectory(), "linktester.err"));

            // The list of URL patterns to skip can be extended by the configuration.
            MojoExecutor.Element skipUrlPatterns = getSkipUrlPatterns();

            executeMojo(
                    plugin(
                            groupId("org.forgerock.maven.plugins"),
                            artifactId("linktester-maven-plugin"),
                            version(m.getLinkTesterVersion())),
                    goal("check"),
                    configuration(
                            element(name("docSources"),
                                    element(name("docSource"),
                                            element(name("directory"),
                                                    m.getBuildDirectory().getPath()),
                                            element(name("includes"),
                                                    element(name("include"),
                                                            "**/" + m.getDocumentSrcName())))),
                            element(name("validating"), "true"),
                            element(name("skipUrls"), m.skipLinkCheck()),
                            element(name("xIncludeAware"), "true"),
                            element(name("failOnError"), "false"),
                            element(name("outputFile"), log),
                            skipUrlPatterns),
                    executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
        }

        /**
         * Return the URL patterns to skip, which can be extended by configuration.
         *
         * @return      The URL patterns to skip.
         */
        private MojoExecutor.Element getSkipUrlPatterns() {

            // The full list includes both default patterns and also configured patterns.
            final ArrayList<Element> patterns = new ArrayList<Element>();

            // Default patterns
            patterns.add(element(name("skipUrlPattern"), // ForgeRock JIRA
                    "^https://bugster.forgerock.org/jira/browse/.+$"));
            patterns.add(element(name("skipUrlPattern"), // RFCs
                    "^http://tools.ietf.org/html/rfc[0-9]+$"));
            patterns.add(element(name("skipUrlPattern"), // Internet-Drafts
                    "^http://tools.ietf.org/html/draft-.+$"));
            patterns.add(element(name("skipUrlPattern"), // example (see RFC 2606)
                    "^https?://[^/]*example.*$"));
            patterns.add(element(name("skipUrlPattern"), // localhost
                    "^https?://localhost.*$"));
            patterns.add(element(name("skipUrlPattern"), // relative URLs to arbitrary resources
                    "^\\.\\./\\Q" + m.getRelativeResourcesDirectoryPath() + "\\E.*$"));

            // Configured patterns
            if (m.getSkipUrlPatterns() != null) {
                for (String pattern : m.getSkipUrlPatterns()) {
                    patterns.add(element("skipUrlPattern", pattern));
                }
            }
            return element(name("skipUrlPatterns"), patterns.toArray(new Element[patterns.size()]));
        }
    }
}
