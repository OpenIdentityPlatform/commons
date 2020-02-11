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

package org.forgerock.doc.maven.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.pre.Fop;
import org.forgerock.doc.maven.utils.NameUtils;
import org.forgerock.doc.maven.utils.OLinkUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Build FO output formats.
 */
public class Fo {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * The Executor to run docbkx-tools.
     */
    private final Executor executor;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Fo(final AbstractDocbkxMojo mojo) {
        m = mojo;
        this.executor = new Executor();
    }

    /**
     * Supported FO formats include "pdf" and "rtf".
     */
    private String format = "pdf";

    /**
     * Get the format.
     * Defaults to PDF unless the format has been set to RTF.
     *
     * @return The format, either "pdf" or "rtf".
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set the format to PDF or RTF.
     * Defaults to PDF unless RTF is specified (case does not matter).
     *
     * @param format Either {@code pdf} or {@code rtf}.
     */
    public void setFormat(final String format) {
        if (format.equalsIgnoreCase("rtf")) {
            this.format = "rtf";
        } else {
            this.format = "pdf";
        }
    }

    /**
     * Build documents from DocBook XML sources.
     *
     * @throws MojoExecutionException Failed to build output.
     */
    public void execute() throws MojoExecutionException {
        executor.prepareOlinkDB();
        executor.build();
    }

    /**
     * Get absolute path to an Olink target database XML document
     * that points to the individual generated Olink DB files, for FO (PDF, RTF).
     *
     * @return Absolute path to the file.
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String getTargetDB() throws MojoExecutionException {
        File targetDB = new File(m.getBuildDirectory(), "olinkdb-" + getFormat() + ".xml");

        try {
            OLinkUtils.createTargetDatabase(targetDB, getFormat(), m);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + targetDB.getPath(), e);
        }

        return targetDB.getPath();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        // Absolute path to Olink target database XML document.
        private String targetDatabaseDocument;

        /**
         * Get the olink target database XML document path.
         *
         * @return Absolute path to the file.
         * @throws MojoExecutionException Could not write target DB file.
         */
        String getTargetDatabaseDocument() throws MojoExecutionException {
            // If it has not been set yet, then set it now.
            if (targetDatabaseDocument == null || targetDatabaseDocument.isEmpty()) {
                targetDatabaseDocument = getTargetDB();
            }

            return targetDatabaseDocument;
        }

        /**
         * Prepare olink target database from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build target database.
         */
        void prepareOlinkDB() throws MojoExecutionException {

            // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
            // RTF generation does not work with docbkx-tools 2.0.15 or 2.0.16.
            // Rather than try also to fix olinks in RTF,
            // skip this until that issue is resolved.
            if (getFormat().equalsIgnoreCase("rtf")) {
                return;
            }

            for (String docName : m.getDocNames()) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(m.getBaseConfiguration());
                cfg.add(element(name("xincludeSupported"), m.isXincludeSupported()));
                cfg.add(element(name("sourceDirectory"), m.path(m.getDocbkxModifiableSourcesDirectory())));
                cfg.add(element(name("fop1Extensions"), "1"));
                cfg.add(element(name("fopLogLevel"), m.getFopLogLevel()));
                cfg.add(element(name("collectXrefTargets"), "yes"));
                if (getFormat().equalsIgnoreCase("pdf")) {
                    cfg.add(element(name("insertOlinkPdfFrag"), "1"));
                }
                cfg.add(element(name("includes"), docName + "/" + m.getDocumentSrcName()));
                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("targetDatabaseDocument"), getTargetDatabaseDocument()));
                cfg.add(element(name("targetDirectory"), m.path(m.getDocbkxOutputDirectory()) + "/" + getFormat()));
                cfg.add(element(name("targetsFilename"), m.getDocumentSrcName() + ".fo.target.db"));

                executeMojo(
                        plugin(
                                groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(m.getDocbkxVersion())),
                        goal("generate-" + getFormat()),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager())
                );

                File outputDir = FileUtils.getFile(
                        m.getBaseDir(), "target", "docbkx", getFormat(), docName);

                // The following output directory should be where the files are
                // for versions of docbkx-tools that honor <targetsFilename>.
                if (!outputDir.exists()) {
                    outputDir = new File(m.getDocbkxOutputDirectory(),
                            getFormat() + File.separator + docName);
                }

                try {
                    String[] extensions = {"fo", getFormat()};
                    Iterator<File> files =
                            FileUtils.iterateFiles(outputDir, extensions, true);
                    while (files.hasNext()) {
                        FileUtils.forceDelete(files.next());
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Cannot delete a file: " + e.getMessage());
                }
            }
        }

        /**
         * Build documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void build() throws MojoExecutionException {

            for (String docName : m.getDocNames()) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(m.getBaseConfiguration());
                cfg.add(element(name("foCustomization"), m.path(m.getFoCustomization())));
                cfg.add(element(name("fop1Extensions"), "1"));

                if (getFormat().equalsIgnoreCase("pdf")) {
                    cfg.add(element(name("insertOlinkPdfFrag"), "1"));
                }

                // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
                // RTF generation does not work with docbkx-tools 2.0.15 or 2.0.16.
                // New features like <fopLogLevel> cannot be used with RTF for now.
                if (!getFormat().equalsIgnoreCase("rtf")) {
                    cfg.add(element(name("fopLogLevel"), m.getFopLogLevel()));
                }

                // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
                // skip olink resolution with RTF for now.
                if (!getFormat().equalsIgnoreCase("rtf")) {
                    cfg.add(element(name("targetDatabaseDocument"), getTargetDatabaseDocument()));
                }

                cfg.add(element(name("targetDirectory"), m.path(m.getDocbkxOutputDirectory()) + "/" + getFormat()));

                final String fontDir = m.path(m.getFontsDirectory());
                cfg.add(Fop.getFontsElement(fontDir));

                cfg.add(element(name("includes"), docName + "/" + m.getDocumentSrcName()));
                cfg.add(element(name("currentDocid"), docName));

                // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
                // if the format is RTF, stick with 2.0.14 for now.
                String docbkxVersion = m.getDocbkxVersion();
                if (format.equalsIgnoreCase("rtf")) {
                    docbkxVersion = "2.0.14";
                }

                executeMojo(
                        plugin(
                                groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(docbkxVersion),
                                dependencies(
                                        dependency(
                                                groupId("net.sf.offo"),
                                                artifactId("fop-hyph"),
                                                version(m.getFopHyphVersion())))),
                        goal("generate-" + getFormat()),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

                // Avoid each new document overwriting the last.
                File file = FileUtils.getFile(
                        m.getDocbkxOutputDirectory(), getFormat(),
                        FilenameUtils.getBaseName(m.getDocumentSrcName()) + "." + getFormat());

                try {
                    NameUtils.renameDocument(file, docName, m.getProjectName());
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to rename document", e);
                }
            }
        }
    }
}
