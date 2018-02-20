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

package org.forgerock.doc.maven.post;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;
import org.forgerock.doc.maven.utils.SyntaxHighlighterCopier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * HTML post-processor for both single-page and chunked HTML formats.
 */
public class Html {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Html(final AbstractDocbkxMojo mojo) {
        m = mojo;

        outputDirectories = new String[2];
        outputDirectories[0] = "";
        outputDirectories[1] = File.separator + FilenameUtils.getBaseName(m.getDocumentSrcName());
    }

    /**
     * Post-processes HTML formats.
     *
     * @throws MojoExecutionException Failed to post-process HTML.
     */
    public void execute() throws MojoExecutionException {
        // Add JavaScript for manipulating HTML content.
        addScript();


        // Add SyntaxHighlighter files.
        final File htmlDir = new File(m.getDocbkxOutputDirectory(), "html");
        final String chunkDirName = FilenameUtils.getBaseName(m.getDocumentSrcName());

        String[] outputDirectories = new String[2 * m.getDocNames().size()];

        int i = 0;
        for (final String docName : m.getDocNames()) {

            final File docDir = new File(htmlDir, docName);

            // Examples:
            // ${project.build.directory}/docbkx/html/my-book
            outputDirectories[i] = docDir.getPath();
            ++i;

            // ${project.build.directory}/docbkx/html/my-book/index
            outputDirectories[i] = new File(docDir, chunkDirName).getPath();
            ++i;
        }

        SyntaxHighlighterCopier copier =
                new SyntaxHighlighterCopier(outputDirectories);
        try {
            copier.copy();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to copy files: " + e.getMessage(), e);
        }


        // Edit the HTML for publication.
        editBuiltHtml(htmlDir.getPath());


        // Optionally fix links to arbitrary resources in chunked HTML.
        if (m.doCopyResourceFiles() && m.getResourcesDirectory().exists()) {

            final String baseName = FilenameUtils.getBaseName(m.getResourcesDirectory().getPath());

            for (final String docName : m.getDocNames()) {

                final File docDir = new File(htmlDir, docName);

                try {
                    HtmlUtils.fixResourceLinks(new File(docDir, chunkDirName).getPath(), baseName);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to update resource links", e);
                }
            }
        }
    }

    /**
     * Directories where scripts and CSS are to be added.
     */
    private String[] outputDirectories;

    /**
     * Add JavaScript to include in HTML in each document source directory.
     * See <a href="http://docbook.sourceforge.net/release/xsl/current/doc/html/html.script.html"
     * >html.script</a> for details.
     *
     * @throws MojoExecutionException Failed to add script.
     */
    private void addScript() throws MojoExecutionException {

        final URL scriptUrl = getClass().getResource("/js/" + m.getJavaScriptFileName());
        String scriptString;
        try {
            scriptString = IOUtils.toString(scriptUrl);
        } catch (IOException ie) {
            throw new MojoExecutionException("Failed to read " + scriptUrl, ie);
        }

        if (scriptString != null) {
            scriptString = scriptString.replace("PROJECT_NAME", m.getProjectName().toLowerCase());
            scriptString = scriptString.replace("PROJECT_VERSION", m.getProjectVersion());
            scriptString = scriptString.replace("LATEST_JSON", m.getLatestJson());
            scriptString = scriptString.replace("DOCS_SITE", m.getDocsSite());
            scriptString = scriptString.replace("EOSL_JSON", m.getEoslJson());
        } else {
            throw new MojoExecutionException(scriptUrl + " was empty");
        }

        // The html.script parameter should probably take URLs.
        // When local files are referenced,
        // the DocBook XSL stylesheets do not copy the .js files.
        // Instead the files must be copied to the output directories.

        for (final String outputDirectory : outputDirectories) {

            for (final String docName : m.getDocNames()) {

                final File parent = FileUtils.getFile(
                        m.getDocbkxOutputDirectory(), "html", docName + outputDirectory);
                final File scriptFile = new File(parent, m.getJavaScriptFileName());

                try {
                    FileUtils.writeStringToFile(scriptFile, scriptString, "UTF-8");
                } catch (IOException ie) {
                    throw new MojoExecutionException(
                            "Failed to write to " + scriptFile.getPath(), ie);
                }
            }
        }
    }

    /**
     * Edit build single-page and chunked HTML.
     *
     * <p>
     *
     * The HTML built by docbkx-tools does not currently include the following,
     * which this method adds.
     *
     * <ul>
     * <li>A DOCTYPE declaration (needed by Internet Explorer to interpret CSS</li>
     * <li>A meta tag for controlling crawling and indexing</li>
     * <li>JavaScript to call the SyntaxHighlighter brushes</li>
     * <li>A favicon link</li>
     * <li>A paragraph about logging issues with a link to JIRA</li>
     * <li>JavaScript used by Google Analytics</li>
     * </ul>
     *
     * @param htmlDir Directory under which to find HTML output
     * @throws MojoExecutionException Something went wrong when updating HTML.
     */
    final void editBuiltHtml(final String htmlDir) throws MojoExecutionException {
        try {
            HashMap<String, String> replacements = new HashMap<String, String>();

            String doctype = IOUtils.toString(
                    getClass().getResourceAsStream("/starthtml-doctype.txt"), "UTF-8");
            replacements.put("<html>", doctype);

            // See https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag
            String robots = "<head>" + System.getProperty("line.separator")
                    + IOUtils.toString(getClass().getResourceAsStream("/robots.txt"), "UTF-8");
            replacements.put("<head>", robots);

            String favicon = IOUtils.toString(
                    getClass().getResourceAsStream("/endhead-favicon.txt"), "UTF-8");
            favicon = favicon.replace("FAVICON-LINK", m.getFaviconLink());
            replacements.put("</head>", favicon);

            String linkToJira = getLinkToJira();

            String gascript = IOUtils.toString(
                    getClass().getResourceAsStream("/endbody-ga.txt"), "UTF-8");
            gascript = gascript.replace("ANALYTICS-ID", m.getGoogleAnalyticsId());
            replacements.put("</body>", linkToJira + "\n" + gascript);

            HtmlUtils.updateHtml(htmlDir, replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to update output HTML correctly: " + e.getMessage());
        }
    }

    /**
     * Return a &lt;p&gt; containing a link to log a bug in JIRA, depending on the project.
     * The string is not localized.
     *
     * @return &lt;p&gt; containing a link to log a bug in JIRA.
     */
    private String getLinkToJira() {
        String link = "<p>&nbsp;</p><div id=\"footer\"><p>Something wrong on this page? "
                + "<a href=\"JIRA-URL\">Log a documentation bug.</a></p></div>";

        // https://confluence.atlassian.com/display/JIRA/Creating+Issues+via+direct+HTML+links
        String jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa";

        if (m.getProjectName().equalsIgnoreCase("OpenAM")) {
            jiraURL += "?pid=10000&components=10007&issuetype=1";
        }
        if (m.getProjectName().equalsIgnoreCase("OpenDJ")) {
            jiraURL += "?pid=10040&components=10132&issuetype=1";
        }
        if (m.getProjectName().equalsIgnoreCase("OpenICF")) {
            jiraURL += "?pid=10041&components=10170&issuetype=1";
        }
        if (m.getProjectName().equalsIgnoreCase("OpenIDM")) {
            jiraURL += "?pid=10020&components=10164&issuetype=1";
        }
        if (m.getProjectName().equalsIgnoreCase("OpenIG")) {
            jiraURL += "?pid=10060&components=10220&issuetype=1";
        }
        if (m.getProjectName().equalsIgnoreCase("ForgeRock")) { // Just testing
            jiraURL += "?pid=10010&issuetype=1";
        }

        if (!jiraURL.contains("pid")) {
            link = "";
        } else {
            link = link.replaceFirst("JIRA-URL", jiraURL);
        }
        return link;
    }
}
