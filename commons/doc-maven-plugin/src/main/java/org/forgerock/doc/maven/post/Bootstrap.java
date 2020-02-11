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

package org.forgerock.doc.maven.post;

import org.apache.commons.io.IOUtils;

import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.HtmlUtils;
import org.forgerock.doc.maven.utils.BootstrapCopier;
import org.forgerock.doc.maven.utils.NameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * HTML post-processor for Bootstrap-formatted HTML formats.
 */
public class Bootstrap {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Bootstrap(final AbstractDocbkxMojo mojo) {
        m = mojo;

        outputDirectories = new String[1];
        outputDirectories[0] = "";
    }

    /**
     * Post-processes Bootstrap formats.
     *
     * @throws MojoExecutionException Failed to post-process Bootstrap format.
     */
    public void execute() throws MojoExecutionException {

        // Add HtmlForBootstrap files.
        final File htmlDir = new File(m.getDocbkxOutputDirectory(),
                "bootstrap");

        String[] outputDirectories = new String[m.getDocNames().size()];

        int i = 0;
        for (final String docName : m.getDocNames()) {

            final File docDir = new File(htmlDir, docName);

            // If PDFs are also being built, edit Bootstrap HTML with a link
            if (m.getFormats().contains(AbstractDocbkxMojo.Format.pdf)) {
                addPDFLink(docDir.getPath(), docName);
            }

            // Example:
            // ${project.build.directory}/docbkx/html/my-book
            outputDirectories[i] = docDir.getPath();
            ++i;

        }
        editBuiltHtml(htmlDir.getPath());

        if (m.isDraftMode().equals("yes")) {
            addDraftAlert(htmlDir.getPath());
        }

        BootstrapCopier copier =
                new BootstrapCopier(outputDirectories);
        try {
            copier.copy();
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to copy files: " + e.getMessage(), e);
        }

    }

    /**
     * Add essentials to the built Bootstrap HTML.
     *
     * <p>
     *
     * - Add Google Analytics tracking code to the Bootstrap HTML
     *
     *
     * @param htmlDir Directory under which to find Bootstrap output.
     * @throws MojoExecutionException Something went wrong when updating HTML.
     */
    final void editBuiltHtml(final String htmlDir) throws
            MojoExecutionException {
        try {
            HashMap<String, String> replacements = new HashMap<String, String>();

            String gascript = IOUtils.toString(
                    getClass().getResourceAsStream("/endhead-ga.txt"), "UTF-8");
            gascript = gascript.replace("ANALYTICS-ID", m.getGoogleAnalyticsId());
            replacements.put("</head>", gascript);

            HtmlUtils.updateHtml(htmlDir, replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to update output HTML correctly: " + e.getMessage());
        }
    }



    final void addDraftAlert(final String htmlDir) throws
        MojoExecutionException {
        try {
            HashMap<String, String> replacements = new HashMap<String, String>();
            String draftAlert = IOUtils.toString(
                    getClass().getResourceAsStream("/endbody-draftalert.txt"), "UTF-8");
            replacements.put("</body>", draftAlert);
            HtmlUtils.updateHtml(htmlDir, replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to update output HTML correctly: " + e.getMessage());
        }
    }

    /**
     * Add a link to the PDF in the built Bootstrap HTML.
     *
     * <p>
     *
     * If both Bootstrap and PDF formats are being built, link to the PDFs
     * from the Bootstrap.
     *
     *
     * @param htmlDir Directory under which to find Bootstrap output.
     * @param docName The short name of the document, for example "dev-guide".
     * @throws MojoExecutionException Something went wrong when updating HTML.
     */
    final void addPDFLink(final String htmlDir, final String docName) throws
            MojoExecutionException {
        try {
            HashMap<String, String> replacements = new HashMap<String, String>();

            String linkToPdf = getLinkToPdf(docName);
            replacements.put("<ul id=\"pdf-link\">", linkToPdf);

            HtmlUtils.updateHtml(htmlDir, replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to inject PDF link HTML correctly: " + e.getMessage());
        }
    }

    private String getLinkToPdf(final String docName) {
        // Note: No closing UL required, it's already in the HTML
        String link = "<ul id=\"pdf-link\" "
            + "class=\"nav navbar-nav navbar-right hidden-xs\">"
            + "<li><a href=\"PDF-URL\" target=\"_blank\">"
            + "<span class=\"glyphicon glyphicon-save\"></span> "
            + "Download PDF Version</a></li>";

        String pdfUrl = "../../" + NameUtils.renameDoc(m.getProjectName(),
                docName, "pdf");
        link = link.replaceFirst("PDF-URL", pdfUrl);

        return link;
    }

    /**
     * Directories where scripts and CSS are to be added.
     */
    private String[] outputDirectories;
}
