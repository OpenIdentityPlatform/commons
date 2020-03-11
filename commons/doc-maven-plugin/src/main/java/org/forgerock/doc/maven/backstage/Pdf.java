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
import org.forgerock.doc.maven.utils.NameUtils;

import java.io.File;

/**
 * Rename PDF files for Backstage,
 * <code><i>Product-from-docset-json</i>-<i>Version</i>-<i>Doc-name</i>.pdf</code>.
 *
 * <br>
 *
 * The expected original name is <code><i>Product-name</i>-<i>Doc-name</i>.pdf</code>.
 * If the original name is different, PDF files are not renamed.
 */
public class Pdf {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo          The Mojo that holds the configuration.
     */
    public Pdf(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Prepare PDF files for Backstage.
     *
     * @throws MojoExecutionException Failed to handle PDF files.
     */
    public void execute() throws MojoExecutionException {
        copyPdfFiles();
        renamePdfFiles();
    }

    /**
     * Copy build PDF files to the output directory.
     *
     * @throws MojoExecutionException   PDF format output not available.
     */
    private void copyPdfFiles() throws MojoExecutionException {

        if (!m.getFormats().contains(AbstractDocbkxMojo.Format.pdf)) {
            StringBuilder formatList = new StringBuilder();
            for (AbstractDocbkxMojo.Format format : m.getFormats()) {
                formatList.append(format.toString()).append(' ');
            }
            throw new MojoExecutionException("PDF format documents are required yet not available."
                    + System.getProperty("line.separator")
                    + " Available formats include: " + formatList.toString()
                    + System.getProperty("line.separator")
                    + "You can use -Dformats=pdf to build PDF format documents.");
        }

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-resources-plugin"),
                        version(m.getMavenResourcesVersion())),
                goal("copy-resources"),
                configuration(
                        element(name("encoding"), "UTF-8"),
                        element(name("outputDirectory"), m.path(m.getBackstageDirectory())),
                        element("resources",
                                element("resource",
                                        element("directory", m.path(m.getDocbkxOutputDirectory())),
                                        element("includes",
                                                element("include", "**/*.pdf"))))),
                executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
    }

    /**
     * Rename PDF files for Backstage.
     *
     * @throws MojoExecutionException Failed to rename files.
     */
    private void renamePdfFiles() throws MojoExecutionException {
        final String[] ext = {"pdf"};

        final File pdfDirectory = new File(m.getBackstageDirectory(), "pdf");
        for (File pdf : FileUtils.listFiles(pdfDirectory, ext, false)) { // Not recursive
            String docName = pdf.getName().replaceFirst(m.getProjectName() + "-", "").replaceFirst(".pdf", "");
            String newName = NameUtils.renameDoc(m.getBackstageProductName(), docName, m.getProjectVersion(), "pdf");
            if (!pdf.renameTo(new File(pdf.getParent(), newName))) {
                throw new MojoExecutionException("Failed to rename PDF: " + newName);
            }
        }
    }
}
