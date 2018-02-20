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

package org.forgerock.doc.maven.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility methods to work with documents.
 */
public final class NameUtils {
    /**
     * Pattern to validate the document names.
     *
     * <br>
     *
     * project-doc-version names are not expected in published docs,
     * @see #renameDoc(String, String, String, String)
     *
     * <br>
     *
     * When published, documentation file names
     * should include the version before the document name:
     * Project-Version-Doc-Name.ext
     *
     * @deprecated since 3.0.0
     *
     * <p>Valid names:</p>
     * <ul>
     *     <li>guide</li>
     *     <li>admin-quide</li>
     *     <li>OpenTEST-guide</li>
     *     <li>OpenTEST-admin-guide</li>
     *     <li>OpenTEST-admin-guide-1.1.1.0</li>
     *     <li>OpenTEST-admin-guide-1.1.1.0-SNAPSHOT</li>
     *     <li>OpenTEST-admin-guide-1.1.1.0-express</li>
     *     <li>OpenTEST-admin-guide-1.1.1.0-Xpress</li>
     *     <li>OpenTEST-admin-guide-1.1.1.0-Xpress1</li>
     *     <li>OpenTEST-10.1.0-admin-guide</li>
     *     <li>OpenTEST-10.1.0-SNAPSHOT-admin-guide</li>
     *     <li>OpenTEST-10.1.0-Xpress2-admin-guide</li>
     *     <li>db2-connector-1.1.0.0-SNAPSHOT</li>
     * </ul>
     *
     * <p>Invalid names:</p>
     * <ul>
     *     <li>guide.</li>
     *     <li>guide-1</li>
     *     <li>guide-.</li>
     * </ul>
     */
    @Deprecated
    public static final Pattern DOCUMENT_FILE_PATTERN = Pattern
            .compile("^([a-zA-Z0-9]+)(-?[0-9].[0-9\\.]*[0-9])?(-SNAPSHOT|(-Ex|-ex|-X)press[0-9])"
                    + "?([a-zA-Z-]*)((-?[0-9].[0-9\\.]*[0-9])?-?(SNAPSHOT|(Ex|ex|X)press[0-9]?)?)$");

    /**
     * Rename document to reflect project and document name. For example,
     * index.pdf could be renamed OpenAM-Admin-Guide.pdf.
     *
     * @param projectName
     *            Short name of the project, such as OpenAM, OpenDJ, OpenIDM
     * @param docName
     *            Short name for the document, such as admin-guide,
     *            release-notes, reference
     * @param extension
     *            File name extension not including dot, e.g. pdf
     * @return New name for document. Can be "" if rename failed.
     */
    public static String renameDoc(final String projectName,
                                   final String docName,
                                   final String extension) {
        return renameDoc(projectName, docName, "", extension);
    }

    /**
     * Rename document to reflect project and document name. For example,
     * index.pdf could be renamed OpenAM-10.0.0-Admin-Guide.pdf.
     *
     * @param projectName
     *            Short name of the project, such as OpenAM, OpenDJ, OpenIDM
     * @param docName
     *            Short name for the document, such as admin-guide,
     *            release-notes, reference
     * @param version
     *            Document version such as 10.0.0, 2.5.0, 2.0.2
     * @param extension
     *            File name extension not including dot, e.g. pdf
     * @return New name for document. Can be "" if rename failed.
     */
    public static String renameDoc(final String projectName,
                                   final String docName,
                                   final String version,
                                   final String extension) {

        // Doc name must be non-empty.
        if (!StringUtils.isNotBlank(docName)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // If project name exists, capitalize it, replace spaces with hyphens,
        // and follow the result with a hyphen.
        if (StringUtils.isNotBlank(projectName)) {
            sb.append(spacesToHyphens(capitalize(projectName))).append('-');

            // Version precedes the document name.
            // It only makes sense to use a version if a project name is defined.
            // If version exists, follow it with a hyphen.
            if (StringUtils.isNotBlank(version)) {
                sb.append(version).append('-');
            }
        }

        // Capitalize the doc name.
        sb.append(capitalize(docName));

        // If extension exists, precede it with a .
        if (StringUtils.isNotBlank(extension)) {
            sb.append('.').append(extension);
        }

        return sb.toString();
    }

    /**
     * Capitalize initial letters in a document name.
     *
     * @param docName
     *            Name of the document such as reference or admin-guide
     * @return Capitalized name such as Reference or Admin-Guide
     */
    protected static String capitalize(final String docName) {
        char[] chars = docName.toCharArray();

        boolean isInitial = true;
        for (int i = 0; i < chars.length; i++) {
            if (isInitial && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                isInitial = false;
            } else {
                isInitial = !Character.isLetter(chars[i]);
            }
        }

        return String.valueOf(chars);
    }

    /**
     * Replace spaces with hyphens.
     *
     * @param   string  String in which to replace spaces with hyphens
     * @return  String with spaces replaced by hyphens.
     */
    protected static String spacesToHyphens(final String string) {
        return string.replaceAll(" ", "-");
    }

    /**
     * Returns names of directories that mirror the document names and contain
     * DocBook XML documents to build.
     *
     * @param srcDir
     *            Directory containing DocBook XML sources. Document directories
     *            like admin-guide or reference are one level below this
     *            directory.
     * @param docFile
     *            Name of a file common to all documents to build, such as
     *            index.xml.
     * @return Document names, as in admin-guide or reference
     */
    public static Set<String> getDocumentNames(final File srcDir,
                                               final String docFile) {
        Set<String> documentDirectories = new TreeSet<String>();

        // Match directories containing DocBook document entry point files,
        // and ignore everything else.
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        };

        File[] directories = srcDir.listFiles(filter);
        if (directories != null && directories.length > 0) {

            FilenameFilter nameFilter = new FilenameFilter() {
                @Override
                public boolean accept(final File file, final String name) {
                    return name.equalsIgnoreCase(docFile);
                }
            };

            for (File dir : directories) {
                String[] found = dir.list(nameFilter);
                if (found.length > 0) {
                    documentDirectories.add(dir.getName());
                }
            }
        }

        return documentDirectories;
    }

    /**
     * Rename a single built document.
     * For example, rename {@code index.pdf} to {@code OpenAM-Admin-Guide.pdf}.
     *
     * @param builtDocument File to rename, such as {@code index.pdf}.
     * @param docName       Simple document name such as {@code admin-guide}.
     * @param projectName   Project name, such as {@code OpenAM}.
     * @throws IOException  Something went wrong renaming the file.
     */
    public static void renameDocument(final File builtDocument,
                                      final String docName,
                                      final String projectName)
            throws IOException {
        String ext = FilenameUtils.getExtension(builtDocument.getName());
        File newFile = new File(builtDocument.getParent(), renameDoc(projectName, docName, ext));
        if (!newFile.exists()) {
            FileUtils.moveFile(builtDocument, newFile);
        }
    }

    /**
     * Not used.
     */
    private NameUtils() {
    }
}
