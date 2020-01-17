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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods to prepare built HTML docs for publication.
 */
public final class HtmlUtils {
    /**
     * Add a <code>.htaccess</code> file to the base directory, for publication
     * on an Apache HTTPD server.
     * <p>
     * According to Apache documentation on <a
     * href="http://httpd.apache.org/docs/2.4/howto/htaccess.html#how">How
     * directives are applied</a>, "The configuration directives found in a
     * .htaccess file are applied to the directory in which the .htaccess file
     * is found, and to all subdirectories thereof." So there is no need to copy
     * the file recursively to all directories.
     *
     * @param baseDir
     *            Base directory under which to add the file
     * @param htaccess
     *            <code>.htaccess</code> file to copy
     * @throws IOException
     *             Something went wrong during copy procedure.
     */
    public static void addHtaccess(final String baseDir, final File htaccess) throws IOException {
        FileUtils.copyFileToDirectory(htaccess, new File(baseDir));
    }

    /**
     * Add custom CSS with XML wrapper to document source directories.
     *
     * <p>
     *
     * See <a href="http://docbook.sourceforge.net/release/xsl/current/doc/html/custom.css.source.html"
     * >custom.css.source</a> for details.
     *
     * @param cssFile         The CSS file to wrap in XML.
     * @param srcDir          The source directory for DocBook XML documents.
     * @param documentSrcName The top-level entry file to DocBook XML documents.
     * @throws IOException Something went wrong adding CSS.
     */
    public static void addCustomCss(final File cssFile,
                                    final File srcDir,
                                    final String documentSrcName)
            throws IOException {
        if (!cssFile.exists()) {
            throw new IOException(cssFile.getPath() + " not found");
        }

        final String cssString = FileUtils.readFileToString(cssFile);

        Set<String> docNames = NameUtils.getDocumentNames(
                srcDir, documentSrcName);
        if (docNames.isEmpty()) {
            throw new IOException("No document names found.");
        }

        for (String docName : docNames) {

            final File parent = new File(srcDir, docName);
            final File xmlFile = new File(parent, cssFile.getName() + ".xml");

            if (!xmlFile.exists()) { // Do not append the document again to the same file.
                FileUtils.write(xmlFile, "<?xml version=\"1.0\"?>\n", true);
                FileUtils.write(xmlFile, "<style>\n", true);
                FileUtils.write(xmlFile, cssString, true);
                FileUtils.write(xmlFile, "</style>\n", true);
            }
        }
    }

    /**
     * Replace HTML tags with additional content.
     *
     * @param baseDir
     *            Base directory under which to find HTML files recursively
     * @param replacements
     *            Keys are tags to replace. Values are replacements, including
     *            the original tag.
     * @return List of files updated
     * @throws IOException
     *             Something went wrong reading or writing files.
     */
    public static List<File> updateHtml(final String baseDir,
                                 final Map<String, String> replacements)
            throws IOException {
        // Match normal directories, and HTML files.
        IOFileFilter dirFilter = FileFilterUtils
                .and(FileFilterUtils.directoryFileFilter(),
                        HiddenFileFilter.VISIBLE);
        IOFileFilter fileFilter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.suffixFileFilter(".html"));
        FileFilter filter = FileFilterUtils.or(dirFilter, fileFilter);

        FilteredFileUpdater ffu = new FilteredFileUpdater(replacements, filter);
        return ffu.update(new File(baseDir));
    }


    /**
     * Fix links to arbitrary resources in HTML files.
     *
     * <p>
     *
     * Chunked HTML and webhelp have extra directories in the path.
     * Links like {@code ../resources/file.txt} that work in the source
     * do not work as is in these formats.
     * Instead the links need an extra .. as in {@code ../../resources/file.txt}.
     *
     * @param htmlDir              Path to a directory containing HTML.
     * @param resourcesDirBaseName Base name of the resources directory.
     * @throws IOException Something went wrong updating links.
     */
    public static void fixResourceLinks(final String htmlDir, final String resourcesDirBaseName)
            throws IOException {

        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("href=\'../" + resourcesDirBaseName, "href=\'../../" + resourcesDirBaseName);
        replacements.put("href=\"../" + resourcesDirBaseName, "href=\"../../" + resourcesDirBaseName);

        updateHtml(htmlDir, replacements);
    }

    /**
     * Not used.
     */
    private HtmlUtils() {
    }
}
