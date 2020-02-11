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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.doc.maven.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.helper.NameMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build olink target database files.
 *
 * <br>
 *
 * O(utside)links are a DocBook convention for describing links between XML documents.
 * For background, read Bob Stayton's explanation of
 * <a href="http://www.sagehill.net/docbookxsl/Olinking.html"
 * >Olinking between documents</a>.
 *
 * <br>
 *
 * The olink resolution mechanism described therein and used here
 * depends on the relative locations of output files.
 */
public final class OLinkUtils {

    /**
     * Construct an olink target database document, except for chunked HTML.
     *
     * <br>
     *
     * The document is an XML file that wraps target data documents.
     * On a first pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to generate such target data documents
     * for each top-level DocBook document in a build.
     * On a second pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to resolve olinks in output documents.
     * During the second pass, the docbkx-tools plugin requires
     * this wrapper document to handle the target data documents
     * with the correct relative locations.
     *
     * <br>
     *
     * You pass the path to the target database document to docbkx-tools
     * as the value of the {@code &lt;targetDatabaseDocument>} parameter.
     *
     * @param file              The file in which to write the target database.
     * @param format            Output format such as {@code pdf}, {@code html}.
     * @param mojo              Mojo with configuration information about the project.
     *
     * @throws IOException              Failed to write to the target database file.
     * @throws MojoExecutionException   Failed to read document names from the mojo.
     */
    public static void createTargetDatabase(File file,
                                            String format,
                                            AbstractDocbkxMojo mojo)
            throws IOException, MojoExecutionException {
        createTargetDatabase(file, format, mojo, false);
    }

    /**
     * Construct an olink target database document for chunked HTML.
     *
     * <br>
     *
     * The document is an XML file that wraps target data documents.
     * On a first pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to generate such target data documents
     * for each top-level DocBook document in a build.
     * On a second pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to resolve olinks in output documents.
     * During the second pass, the docbkx-tools plugin requires
     * this wrapper document to handle the target data documents
     * with the correct relative locations.
     *
     * <br>
     *
     * You pass the path to the target database document to docbkx-tools
     * as the value of the {@code &lt;targetDatabaseDocument>} parameter.
     *
     * @param file              The file in which to write the target database.
     * @param format            Output format such as {@code pdf}, {@code html}.
     * @param mojo              Mojo with configuration information about the project.
     * @param isChunkedHtml     Set {@code true} for chunked HTML.
     *
     * @throws IOException              Failed to write to the target database file.
     * @throws MojoExecutionException   Failed to read document names from the mojo.
     */
    public static void createTargetDatabase(File file,
                                            String format,
                                            AbstractDocbkxMojo mojo,
                                            boolean isChunkedHtml)
            throws IOException, MojoExecutionException {

        createTargetDatabase(
                file,
                mojo.getBuildDirectory().getAbsolutePath(),
                mojo.getDocNames(),
                FilenameUtils.getBaseName(mojo.getDocumentSrcName()),
                format,
                isChunkedHtml,
                mojo.getProjectName(),
                mojo.getProjectVersion());
    }

    /**
     * Construct an olink target database document.
     *
     * <br>
     *
     * The document is an XML file that wraps target data documents.
     * On a first pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to generate such target data documents
     * for each top-level DocBook document in a build.
     * On a second pass, the docbkx-tools plugin applies the DocBook stylesheets
     * with settings to resolve olinks in output documents.
     * During the second pass, the docbkx-tools plugin requires
     * this wrapper document to handle the target data documents
     * with the correct relative locations.
     *
     * <br>
     *
     * You pass the path to the target database document to docbkx-tools
     * as the value of the {@code &lt;targetDatabaseDocument>} parameter.
     *
     * @param file              The file in which to write the target database.
     * @param basePath          Full path to parent of docbkx directory,
     *                          such as {@code /path/to/target}.
     * @param docNames          Names of documents in the set.
     * @param documentSrcName   Top-level DocBook XML source document base name.
     * @param format            Output format such as {@code pdf}, {@code html}.
     * @param isChunked         Set {@code true} for chunked HTML.
     * @param projectName       Name of the current project, such as {@code OpenAM}.
     * @param projectVersion    Version for the current project. Can be empty.
     *
     * @throws IOException  Failed to write to the target database file.
     */
    private static void createTargetDatabase(File file,
                                            String basePath,
                                            Set<String> docNames,
                                            String documentSrcName,
                                            String format,
                                            boolean isChunked,
                                            String projectName,
                                            String projectVersion)
            throws IOException {

        // This implementation uses FreeMarker templates.
        // For details, see http://freemarker.org.

        // FreeMarker templates require a configuration.
        Configuration configuration = getConfiguration();

        // FreeMarker templates are data driven. The model here is a map.
        Map<String, Object> map = getModel(
                basePath,
                docNames,
                documentSrcName,
                format,
                isChunked,
                projectName,
                projectVersion);

        // Apply the FreeMarker template using the data.
        Template template = configuration.getTemplate("olinkdb.ftl");
        FileOutputStream out = FileUtils.openOutputStream(file);
        Writer writer = new OutputStreamWriter(out);
        try {
            template.process(map, writer);
        } catch (TemplateException e) {
            throw new IOException("Failed to write template", e);
        } finally {
            writer.close();
            out.close();
        }
    }

    private static Configuration configuration;

    /**
     * Get a FreeMarker configuration for applying templates.
     *
     * @return              A FreeMarker configuration.
     */
    private static Configuration getConfiguration() {
        if (configuration != null) {
            return configuration;
        }

        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassForTemplateLoading(OLinkUtils.class, "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

        return configuration;
    }

    /**
     * Get the FreeMaker data for use when applying templates.
     *
     * @param basePath          Full path to parent of docbkx directory,
     *                          such as {@code /path/to/target}.
     * @param docNames          Names of documents in the set.
     * @param documentSrcName   Top-level DocBook XML source document base name.
     * @param format            Output format such as {@code pdf}, {@code html}.
     * @param isChunked         Set {@code true} for chunked HTML.
     * @param projectName       Name of the current project, such as {@code OpenAM}.
     * @param projectVersion    Version for the current project. Can be empty.
     *
     * @return                  FreeMarker data for use when applying templates.
     */
    private static Map<String, Object> getModel(String basePath,
                                                Set<String> docNames,
                                                String documentSrcName,
                                                String format,
                                                boolean isChunked,
                                                String projectName,
                                                String projectVersion) {

        /*
         baseName:        base name for document source, such as index
         basePath:        base of absolute path to target data file
         docNames:        list of document names such as reference and admin-guide
         extension:       output file extension such as html, pdf, or xhtml
         format:          output format such as xhtml5 or epub
         isChunked:       whether the output format is chunked HTML
         name():          wrapper to call NameUtils.renameDoc()
         projectName:     project name such as OpenAM
         projectVersion:  project version such as 3.1.0
         type:            output file type such as html, pdf, or xhtml
         */

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("baseName", documentSrcName);
        map.put("basePath", basePath);

        List<String> docs = new ArrayList<String>(docNames.size());
        docs.addAll(docNames);
        map.put("docNames", docs);

        String extension = format;
        if (format.equals("xhtml5")) {
            format = "xhtml";
            extension = "xhtml";
        }
        if (format.equals("bootstrap")) {
            extension = "html";
        }
        map.put("extension", extension);

        map.put("format", format);
        map.put("isChunked", isChunked);

        map.put("name", new NameMethod());

        map.put("projectName", projectName);
        map.put("projectVersion", projectVersion);

        String type = format;
        if (format.equals("pdf") || format.equals("rtf")) {
            type = "fo";
        }
        if (format.equals("xhtml")) {
            type = "xhtml";
        }
        if (format.equals("bootstrap")) {
            type = "html";
        }
        map.put("type", type);

        return map;
    }

    private OLinkUtils() {
        // Not used.
    }
}
