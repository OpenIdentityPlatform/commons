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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.markup;

import static org.forgerock.api.markup.asciidoc.AsciiDoc.asciiDoc;
import static org.forgerock.api.markup.asciidoc.AsciiDoc.normalizeName;
import static org.forgerock.api.markup.asciidoc.AsciiDocTableColumnStyles.ASCII_DOC_CELL;
import static org.forgerock.api.markup.asciidoc.AsciiDocTableColumnStyles.MONO_CELL;
import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static org.forgerock.util.Reject.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.markup.asciidoc.AsciiDoc;
import org.forgerock.api.markup.asciidoc.AsciiDocTable;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Error;
import org.forgerock.api.models.Errors;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.JsonValue;

/**
 * Generates static AsciiDoc documentation for CREST API Descriptors.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public class ApiDocGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * {@code .adoc} file extension for generated AsciiDoc files.
     */
    private static final String ADOC_EXTENSION = ".adoc";

    /**
     * Root output directory.
     */
    private final Path outputDirPath;

    /**
     * Optional input directory.
     */
    private final Path inputDirPath;

    /**
     * Map of dynamically generated schema anchors (keys) to {@link JsonValue} instances, for all {@link Schema}s
     * not found in {@link ApiDescription#getDefinitions()}.
     */
    private final Map<String, JsonValue> schemaMap = new HashMap<>();

    /**
     * Constructor that sets the root output directory for AsciiDoc files, which will be created if it does not exist.
     *
     * @param outputDirPath Root output directory
     */
    public ApiDocGenerator(final Path outputDirPath) {
        this(null, outputDirPath);
    }

    /**
     * Constructor that sets the root output directory for AsciiDoc files, which will be created if it does not exist,
     * and an input directory used for overriding AsciiDoc files (e.g., descriptions).
     *
     * @param inputDirPath Input directory or {@code null}
     * @param outputDirPath Root output directory
     */
    public ApiDocGenerator(final Path inputDirPath, final Path outputDirPath) {
        this.inputDirPath = inputDirPath;
        this.outputDirPath = checkNotNull(outputDirPath, "outputDirPath required");
        if (outputDirPath.equals(inputDirPath)) {
            throw new ApiDocGeneratorException("inputDirPath and outputDirPath can not be equal");
        }
    }

    /**
     * Generates AsciiDoc documentation for a CREST API Descriptor.
     *
     * @param apiDescription API Description
     */
    @SuppressWarnings("unchecked")
    public void execute(final ApiDescription apiDescription) {
        final String namespace = apiDescription.getId();
        try {
            // output paths with or without versions
            String pathsFilename = null;
            if (apiDescription.getPaths() != null) {
                try {
                    pathsFilename = outputPaths((Paths<Resource>) apiDescription.getPaths(), namespace);
                } catch (ClassCastException e1) {
                    try {
                        pathsFilename = outputVersionedPaths((Paths<VersionedPath>) apiDescription.getPaths(),
                                namespace);
                    } catch (ClassCastException e2) {
                        throw new ApiDocGeneratorException(
                                "Unsupported Paths type: " + apiDescription.getPaths().getClass().getName());
                    }
                }
            }

            final String errorsFilename = outputErrors(apiDescription.getErrors(), namespace);
            final String definitionsFilename = outputDefinitions(apiDescription.getDefinitions(), namespace);
            outputRoot(apiDescription, pathsFilename, definitionsFilename, errorsFilename, namespace);
        } catch (IOException e) {
            throw new ApiDocGeneratorException("Unable to output doc file", e);
        }
    }

    /**
     * Outputs a top-level AsciiDoc file that imports all other second-level files generated by this class.
     *
     * @param apiDescription API Description
     * @param pathsFilename Paths file-path suitable for AsciiDoc import-statement
     * @param definitionsFilename Definitions-file path suitable for AsciiDoc import-statement
     * @param errorsFilename Errors-file path suitable for AsciiDoc import-statement
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputRoot(final ApiDescription apiDescription, final String pathsFilename,
            final String definitionsFilename, final String errorsFilename,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "index");

        final AsciiDoc pathsDoc = asciiDoc()
                .documentTitle("API Descriptor")
                .rawParagraph(asciiDoc().rawText("*ID:* ").mono(apiDescription.getId()).toString())
                .rawLine(":toc: left")
                .rawLine(":toclevels: 5")
                .newline();

        final String descriptionFilename = outputDescriptionBlock(apiDescription.getDescription(), namespace);
        pathsDoc.include(descriptionFilename);

        if (pathsFilename != null) {
            pathsDoc.include(pathsFilename);
        }
        if (definitionsFilename != null) {
            pathsDoc.include(definitionsFilename);
        }
        if (errorsFilename != null) {
            pathsDoc.include(errorsFilename);
        }

        final String filename = namespace + ADOC_EXTENSION;
        pathsDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a description-block. The file will be blank when no description is defined.
     * <p>
     * This method will use a replacement-file from {@link #inputDirPath}, if it exists, to override the description.
     * </p>
     *
     * @param defaultDescription Default description for the block or {@code null}
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputDescriptionBlock(final String defaultDescription, final String parentNamespace)
            throws IOException {
        final String namespace = normalizeName(parentNamespace, "description");
        final String filename = namespace + ADOC_EXTENSION;
        if (!copyReplacementFile(filename)) {
            final AsciiDoc blockDoc = asciiDoc();
            if (!isEmpty(defaultDescription)) {
                blockDoc.rawParagraph(defaultDescription);
            }
            blockDoc.toFile(outputDirPath, filename);
        }
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for each path, and another file that imports each path.
     *
     * @param paths Paths
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputPaths(final Paths<Resource> paths, final String parentNamespace) throws IOException {
        final String allPathsDocNamespace = normalizeName(parentNamespace, "paths");
        final AsciiDoc allPathsDoc = asciiDoc()
                .sectionTitle1("Paths");
        final List<String> pathNames = new ArrayList<>(paths.getNames());
        Collections.sort(pathNames);
        for (final String pathName : pathNames) {
            // path
            final String pathDocNamespace = normalizeName(allPathsDocNamespace, pathName);
            final AsciiDoc pathDoc = asciiDoc()
                    .sectionTitle2(asciiDoc().mono(pathName).toString());

            // resource
            final String resourceImport = outputResource(paths.get(pathName), 3, pathDocNamespace);
            pathDoc.include(resourceImport);

            // output path-file
            final String pathDocFilename = pathDocNamespace + ADOC_EXTENSION;
            pathDoc.toFile(outputDirPath, pathDocFilename);

            // include path-file
            allPathsDoc.include(pathDocFilename);
        }

        // output all-paths-file
        final String filename = allPathsDocNamespace + ADOC_EXTENSION;
        allPathsDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for each path, which imports a file for each version under that path, and another
     * file that imports each path.
     *
     * @param paths Versioned paths
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputVersionedPaths(final Paths<VersionedPath> paths, final String parentNamespace)
            throws IOException {
        final String allPathsDocNamespace = normalizeName(parentNamespace, "paths");
        final AsciiDoc allPathsDoc = asciiDoc()
                .sectionTitle1("Paths");

        final List<String> pathNames = new ArrayList<>(paths.getNames());
        Collections.sort(pathNames);
        for (final String pathName : pathNames) {
            // path
            final String pathDocNamespace = normalizeName(allPathsDocNamespace, pathName);
            final AsciiDoc pathDoc = asciiDoc()
                    .sectionTitle2(asciiDoc().mono(pathName).toString());

            final VersionedPath versionedPath = paths.get(pathName);
            final List<String> versions = new ArrayList<>(versionedPath.getVersions());
            Collections.sort(versions);
            for (final String versionName : versions) {
                // version
                final String versionDocNamespace = normalizeName(pathDocNamespace, versionName);
                final AsciiDoc versionDoc = asciiDoc()
                        .sectionTitle3(asciiDoc().mono(versionName).toString());

                // resource
                final String resourceImport = outputResource(versionedPath.get(versionName), 4, versionDocNamespace);
                versionDoc.include(resourceImport);

                // output version-file
                final String versionDocFilename = versionDocNamespace + ADOC_EXTENSION;
                versionDoc.toFile(outputDirPath, versionDocFilename);

                // include version-file
                pathDoc.include(versionDocFilename);
            }

            // output path-file
            final String pathDocFilename = pathDocNamespace + ADOC_EXTENSION;
            pathDoc.toFile(outputDirPath, pathDocFilename);

            // include path-file
            allPathsDoc.include(pathDocFilename);
        }

        // output all-paths-file
        final String filename = allPathsDocNamespace + ADOC_EXTENSION;
        allPathsDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for the resource and each operation, and a file that imports each of those files.
     *
     * @param resource Resource
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputResource(final Resource resource, final int sectionLevel, final String parentNamespace)
            throws IOException {
        final String namespace = normalizeName(parentNamespace, "resource");
        final AsciiDoc resourceDoc = asciiDoc();

        final String descriptionFilename = outputDescriptionBlock(resource.getDescription(), namespace);
        resourceDoc.include(descriptionFilename);

        String resourceAnchor = null;
        if (resource.getResourceSchema() != null) {
            resourceAnchor = resolveSchemaAnchor(resource.getResourceSchema(), namespace);
        }

        if (resource.getCreate() != null) {
            final String filename = outputCreateOperation(resource.getCreate(), sectionLevel, resourceAnchor,
                    namespace);
            resourceDoc.horizontalRule()
                    .include(filename);
        }

        if (resource.getRead() != null) {
            final String filename = outputReadOperation(resource.getRead(), sectionLevel, resourceAnchor, namespace);
            resourceDoc.horizontalRule()
                    .include(filename);
        }

        if (resource.getUpdate() != null) {
            final String filename = outputUpdateOperation(resource.getUpdate(), sectionLevel, resourceAnchor,
                    namespace);
            resourceDoc.horizontalRule()
                    .include(filename);
        }

        if (resource.getDelete() != null) {
            final String filename = outputDeleteOperation(resource.getDelete(), sectionLevel, resourceAnchor,
                    namespace);
            resourceDoc.horizontalRule()
                    .include(filename);
        }

        if (resource.getPatch() != null) {
            final String filename = outputPatchOperation(resource.getPatch(), sectionLevel, resourceAnchor, namespace);
            resourceDoc.horizontalRule()
                    .include(filename);
        }

        if (!isEmpty(resource.getActions())) {
            final String filename = outputActionOperations(resource.getActions(), sectionLevel, namespace);
            resourceDoc.include(filename);
        }

        if (!isEmpty(resource.getQueries())) {
            final String filename = outputQueryOperations(resource.getQueries(), sectionLevel, resourceAnchor,
                    namespace);
            resourceDoc.include(filename);
        }

        final String filename = namespace + ADOC_EXTENSION;
        resourceDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a {@link Create}-operation, and a file that imports each of those files.
     *
     * @param create Create operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param resourceAnchor AsciiDoc anchor to a resource schema or {@code null} if not defined
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputCreateOperation(final Create create, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "create");
        final AsciiDoc operationDoc = asciiDoc()
                .sectionTitle("Create", sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(create.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(create.getStability(), operationDoc);
        outputMvccSupport(create.isMvccSupported(), operationDoc);
        outputParameters(create.getParameters(), namespace, operationDoc);
        outputResourceEntity(resourceAnchor, false, operationDoc);
        outputCreateMode(create.getMode(), operationDoc);
        outputSingletonStatus(create.isSingleton(), operationDoc);
        outputErrors(create.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a {@link Read}-operation, and a file that imports each of those files.
     *
     * @param read Read operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputReadOperation(final Read read, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "read");
        final AsciiDoc operationDoc = asciiDoc()
                .sectionTitle("Read", sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(read.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(read.getStability(), operationDoc);
        outputParameters(read.getParameters(), namespace, operationDoc);
        outputResourceEntity(resourceAnchor, true, operationDoc);
        outputErrors(read.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a {@link Update}-operation, and a file that imports each of those files.
     *
     * @param update Update operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param resourceAnchor AsciiDoc anchor to a resource schema or {@code null} if not defined
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputUpdateOperation(final Update update, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "update");
        final AsciiDoc operationDoc = asciiDoc()
                .sectionTitle("Update", sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(update.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(update.getStability(), operationDoc);
        outputMvccSupport(update.isMvccSupported(), operationDoc);
        outputParameters(update.getParameters(), namespace, operationDoc);
        outputResourceEntity(resourceAnchor, false, operationDoc);
        outputErrors(update.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a {@link Delete}-operation, and a file that imports each of those files.
     *
     * @param delete Delete operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param resourceAnchor AsciiDoc anchor to a resource schema or {@code null} if not defined
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputDeleteOperation(final Delete delete, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "delete");
        final AsciiDoc operationDoc = asciiDoc()
                .sectionTitle("Delete", sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(delete.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(delete.getStability(), operationDoc);
        outputMvccSupport(delete.isMvccSupported(), operationDoc);
        outputParameters(delete.getParameters(), namespace, operationDoc);
        outputResourceEntity(resourceAnchor, true, operationDoc);
        outputErrors(delete.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a {@link Patch}-operation, and a file that imports each of those files.
     *
     * @param patch Patch operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param resourceAnchor AsciiDoc anchor to a resource schema or {@code null} if not defined
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputPatchOperation(final Patch patch, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "patch");
        final AsciiDoc operationDoc = asciiDoc()
                .sectionTitle("Patch", sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(patch.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(patch.getStability(), operationDoc);
        outputMvccSupport(patch.isMvccSupported(), operationDoc);
        outputParameters(patch.getParameters(), namespace, operationDoc);
        outputResourceEntity(resourceAnchor, true, operationDoc);
        outputSupportedPatchOperations(patch.getOperations(), operationDoc);
        outputErrors(patch.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for {@link Action}-operations, and a file that imports each of those files.
     *
     * @param actions Action operations
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputActionOperations(final Action[] actions, final int sectionLevel, final String parentNamespace)
            throws IOException {
        final String namespace = normalizeName(parentNamespace, "action");
        final AsciiDoc operationDoc = asciiDoc();

        for (final Action action : actions) {
            final String filename = outputActionOperation(action, sectionLevel, namespace);
            operationDoc.include(filename);
        }

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file a single {@link Action}-operation, and a file for that action.
     *
     * @param action Action operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputActionOperation(final Action action, final int sectionLevel, final String parentNamespace)
            throws IOException {
        final String namespace = normalizeName(parentNamespace, action.getName());
        final AsciiDoc operationDoc = asciiDoc()
                .horizontalRule()
                .sectionTitle(asciiDoc().rawText("Action: ").mono(action.getName()).toString(), sectionLevel);

        final String descriptionFilename = outputDescriptionBlock(action.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(action.getStability(), operationDoc);
        outputParameters(action.getParameters(), namespace, operationDoc);

        if (action.getRequest() != null) {
            final String schemaAnchor = resolveSchemaAnchor(action.getRequest(), namespace);
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Request Entity")
                    .rawText("This operation takes a request body described ")
                    .link(schemaAnchor, "here")
                    .rawText(".");
            operationDoc.rawParagraph(blockDoc.toString());
        }

        if (action.getResponse() != null) {
            final String schemaAnchor = resolveSchemaAnchor(action.getResponse(), namespace);
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Response Entity")
                    .rawText("This operation returns a response body described ")
                    .link(schemaAnchor, "here")
                    .rawText(".");
            operationDoc.rawParagraph(blockDoc.toString());
        }

        outputErrors(action.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for {@link Query}-operations, and a file that imports each of those files.
     *
     * @param queries Query operations
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputQueryOperations(final Query[] queries, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, "query");
        final AsciiDoc operationDoc = asciiDoc();

        for (final Query query : queries) {
            final String filename = outputQueryOperation(query, sectionLevel, resourceAnchor, namespace);
            operationDoc.include(filename);
        }

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    private String outputQueryOperation(final Query query, final int sectionLevel, final String resourceAnchor,
            final String parentNamespace) throws IOException {
        final String namespace;
        final AsciiDoc operationDoc = asciiDoc()
                .horizontalRule();
        // @Checkstyle:off
        switch (query.getType()) {
            case ID:
                namespace = normalizeName(parentNamespace, "id", query.getQueryId());
                operationDoc.sectionTitle(asciiDoc().rawText("Query by ID: ").mono(query.getQueryId()).toString(),
                        sectionLevel);
                break;
            case FILTER:
                namespace = normalizeName(parentNamespace, "filter");
                operationDoc.sectionTitle("Query by Filter", sectionLevel);
                break;
            case EXPRESSION:
                namespace = normalizeName(parentNamespace, "expression");
                operationDoc.sectionTitle("Query by Expression", sectionLevel);
                break;
            default:
                throw new ApiDocGeneratorException("Unsupported QueryType: " + query.getType());
        }
        // @Checkstyle:on

        final String descriptionFilename = outputDescriptionBlock(query.getDescription(), namespace);
        operationDoc.include(descriptionFilename);

        outputStability(query.getStability(), operationDoc);
        outputParameters(query.getParameters(), namespace, operationDoc);

        if (!isEmpty(query.getQueryableFields())) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Queryable Fields");
            for (final String field : query.getQueryableFields()) {
                blockDoc.unorderedList1(asciiDoc().mono(field).toString());
            }
            operationDoc.rawParagraph(blockDoc.toString());
        }

        if (!isEmpty(query.getPagingMode())) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Paging Modes");
            for (final PagingMode pagingMode : query.getPagingMode()) {
                blockDoc.unorderedList1(asciiDoc().mono(pagingMode.toString()).toString());
            }
            operationDoc.rawParagraph(blockDoc.toString());
        }

        if (!isEmpty(query.getCountPolicy())) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Page Count Policies");
            for (final CountPolicy countPolicy : query.getCountPolicy()) {
                blockDoc.unorderedList1(asciiDoc().mono(countPolicy.toString()).toString());
            }
            operationDoc.rawParagraph(blockDoc.toString());
        }

        if (!isEmpty(query.getSupportedSortKeys())) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Supported Sort Keys");
            for (final String sortKey : query.getSupportedSortKeys()) {
                blockDoc.unorderedList1(asciiDoc().mono(sortKey).toString());
            }
            operationDoc.rawParagraph(blockDoc.toString());
        }

        if (resourceAnchor != null) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Resource Entity")
                    .rawText("This operation returns a result structure described ")
                    .link(resourceAnchor, "here")
                    .rawText(".");
            operationDoc.rawParagraph(blockDoc.toString());
        }

        outputErrors(query.getErrors(), namespace, operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        operationDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs operation stability.
     *
     * @param stability Operation stability or {@code null} to use default {@link Stability#STABLE}
     * @param doc AsciiDoc to write to
     */
    private static void outputStability(Stability stability, final AsciiDoc doc) {
        if (stability == null) {
            stability = Stability.STABLE;
        }
        final String s = asciiDoc()
                .rawText("Interface Stability: ").link(stability.name())
                .toString();
        doc.rawParagraph(s);
    }

    /**
     * Outputs MVCC support.
     *
     * @param mvccSupported MVCC support flag
     * @param doc AsciiDoc to write to
     */
    private static void outputMvccSupport(final boolean mvccSupported, final AsciiDoc doc) {
        if (mvccSupported) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Support For MVCC")
                    .rawText("This operation supports MVCC.");
            doc.rawParagraph(blockDoc.toString());
        }
    }

    /**
     * Outputs a link to an operation's resource schema.
     *
     * @param resourceAnchor AsciiDoc anchor to a resource schema or {@code null} if not defined
     * @param responseOnly {@code true} when resource is sent only in response and {@code false} for request/response
     * @param doc AsciiDoc to write to
     */
    private static void outputResourceEntity(final String resourceAnchor, final boolean responseOnly,
            final AsciiDoc doc) {
        if (resourceAnchor != null) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Resource Entity");
            if (responseOnly) {
                blockDoc.rawText("This operation returns a response resource described ");
            } else {
                blockDoc.rawText("This operation takes a request body and returns a response resource described ");
            }
            blockDoc.link(resourceAnchor, "here")
                    .rawText(".");
            doc.rawParagraph(blockDoc.toString());
        }
    }

    /**
     * Outputs singleton status for {@link Create} operation.
     *
     * @param isSingleton Singleton status
     * @param doc AsciiDoc to write to
     */
    private static void outputSingletonStatus(final boolean isSingleton, final AsciiDoc doc) {
        if (isSingleton) {
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Singleton")
                    .rawText("This resource is a singleton.");
            doc.rawParagraph(blockDoc.toString());
        }
    }

    /**
     * Outputs operation parameters.
     *
     * @param parameters Operation parameters or {@code null}/empty for pass-through
     * @param parentNamespace Parent namespace
     * @param doc AsciiDoc to write to
     */
    private void outputParameters(final Parameter[] parameters, final String parentNamespace, final AsciiDoc doc)
            throws IOException {
        if (isEmpty(parameters)) {
            return;
        }
        final String parametersNamespace = normalizeName(parentNamespace, "parameters");
        final AsciiDocTable table = doc.tableStart()
                .title("Parameters")
                .headers("Name", "Type", "Description", "Required", "In", "Values")
                .columnWidths(2, 1, 4, 1, 1, 2);
        for (final Parameter parameter : parameters) {
            // format optional enumValues
            String enumValuesContent = null;
            if (!isEmpty(parameter.getEnumValues()) || !isEmpty(parameter.getDefaultValue())) {
                final AsciiDoc enumValuesDoc = asciiDoc();
                if (!isEmpty(parameter.getDefaultValue())) {
                    enumValuesDoc.italic("Default:")
                            .rawText(" ")
                            .mono(parameter.getDefaultValue())
                            .newline();
                }
                if (!isEmpty(parameter.getEnumValues())) {
                    final String[] enumValues = parameter.getEnumValues();
                    final String[] enumTitles = parameter.getEnumTitles();
                    for (int i = 0; i < enumValues.length; ++i) {
                        final AsciiDoc enumDoc = asciiDoc()
                                .mono(enumValues[i]);
                        if (enumTitles != null) {
                            enumDoc.rawText(": " + enumTitles[i]);
                        }
                        enumValuesDoc.unorderedList1(enumDoc.toString());
                    }
                }
                enumValuesContent = enumValuesDoc.toString();
            }

            final String namespace = normalizeName(parametersNamespace, parameter.getName());
            final String descriptionFilename = outputDescriptionBlock(parameter.getDescription(), namespace);

            // format table
            table.columnCell(parameter.getName(), MONO_CELL)
                    .columnCell(parameter.getType(), MONO_CELL)
                    .columnCell(asciiDoc().include(descriptionFilename).toString(), ASCII_DOC_CELL)
                    .columnCell(parameter.isRequired() ? "✓" : null)
                    .columnCell(parameter.getSource().name(), MONO_CELL)
                    .columnCell(enumValuesContent, ASCII_DOC_CELL)
                    .rowEnd();
        }
        table.tableEnd();
    }

    /**
     * Outputs create-mode for a {@link Create} operation.
     *
     * @param createMode Create-mode
     * @param doc AsciiDoc to write to
     */
    private static void outputCreateMode(final CreateMode createMode, final AsciiDoc doc) {
        final AsciiDoc idConstraintDoc = asciiDoc()
                .blockTitle("Identifier Constraints");
        // @Checkstyle:off
        switch (createMode) {
            case ID_FROM_CLIENT:
                idConstraintDoc.rawText("The identifier is accepted from your client. "
                        + "If the identifier is specified in the path parameter, "
                        + "it must match the identifier you supply in the resource.");
                break;
            case ID_FROM_SERVER:
                idConstraintDoc.rawText("The identifier is provided by the server. Do not supply an identifier.");
                break;
            default:
                throw new ApiDocGeneratorException("Unsupported CreateMode: " + createMode);
        }
        // @Checkstyle:on
        doc.rawParagraph(idConstraintDoc.toString());
    }

    /**
     * Outputs supported patch-operations for a {@link Patch} operation.
     *
     * @param patchOperations Supported patch-operations
     * @param doc AsciiDoc to write to
     */
    private static void outputSupportedPatchOperations(final PatchOperation[] patchOperations, final AsciiDoc doc) {
        final AsciiDoc blockDoc = asciiDoc()
                .blockTitle("Supported Patch Operations");
        for (final PatchOperation patchOperation : patchOperations) {
            blockDoc.unorderedList1(patchOperation.name());
        }
        doc.rawParagraph(blockDoc.toString());
    }

    /**
     * Outputs an {@link Error}, without an AsciiDoc anchor, because this error will not be linked-to directly.
     *
     * @param error Error
     * @param parentNamespace Parent-namespace
     * @param doc AsciiDoc to write to
     */
    private void outputError(final Error error, final String parentNamespace, final AsciiDoc doc) {
        outputError(error, null, parentNamespace, doc);
    }

    /**
     * Outputs an {@link Error}, with optional AsciiDoc anchor.
     *
     * @param error Error
     * @param errorAnchor AsciiDoc anchor, for the error, or {@code null}
     * @param parentNamespace Parent-namespace
     * @param doc AsciiDoc to write to
     */
    private void outputError(final Error error, final String errorAnchor, final String parentNamespace,
            final AsciiDoc doc) {
        final AsciiDoc errorDoc = asciiDoc();
        if (!isEmpty(errorAnchor)) {
            errorDoc.anchor(errorAnchor);
        }
        errorDoc.mono(String.valueOf(error.getCode()))
                .rawText(" " + error.getDescription());
        if (error.getSchema() != null) {
            final String resourceAnchor = resolveSchemaAnchor(error.getSchema(), parentNamespace);
            errorDoc.listContinuation()
                    .rawLine(asciiDoc()
                            .rawText("This error returns an error-detail described ")
                            .link(resourceAnchor, "here")
                            .rawText(".")
                            .toString());
        }
        doc.unorderedList1(errorDoc.toString());
    }

    /**
     * Outputs operation errors.
     *
     * @param errors Operation errors or {@code null}/empty for pass-through
     * @param parentNamespace Parent namespace
     * @param doc AsciiDoc to write to
     */
    private void outputErrors(final Error[] errors, final String parentNamespace, final AsciiDoc doc) {
        if (isEmpty(errors)) {
            return;
        }
        doc.blockTitle("Errors");
        Arrays.sort(errors, Error.ERROR_COMPARATOR);
        for (int i = 0; i < errors.length; ++i) {
            final String namespace = normalizeName(parentNamespace, "error", String.valueOf(i));
            outputError(errors[i], namespace, doc);
        }
    }

    /**
     * Outputs an AsciiDoc file for each schema definition, and a file that imports all schema definitions.
     *
     * @param definitions Schema definitions
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputDefinitions(final Definitions definitions, final String parentNamespace) throws IOException {
        if (definitions == null) {
            return null;
        }

        final String definitionsNamespace = normalizeName(parentNamespace, "definitions");
        final AsciiDoc definitionsDoc = asciiDoc()
                .sectionTitle1("Definitions");

        // named schema definitions
        final Set<String> definitionNames = definitions.getNames();
        for (final String name : definitionNames) {
            final Schema schema = definitions.get(name);
            if (schema.getSchema() != null) {
                // namespace must follow exact format as a valid API Descriptor JSON Reference to a definition object
                final String namespace = normalizeName(definitionsNamespace, name);
                definitionsDoc.newline()
                        .anchor(namespace)
                        .blockTitle(name);

                final String descriptionFilename = outputDescriptionBlock(null, namespace);
                definitionsDoc.include(descriptionFilename);

                final String schemaFilename = outputJsonSchema(schema.getSchema(), namespace);
                definitionsDoc.include(schemaFilename);
            }
        }

        // all other schema definitions
        final Set<String> generatedNames = schemaMap.keySet();
        for (final String namespace : generatedNames) {
            definitionsDoc.newline()
                    .anchor(namespace)
                    .blockTitle(namespace);

            final String descriptionFilename = outputDescriptionBlock(null, namespace);
            definitionsDoc.include(descriptionFilename);

            final String filename = outputJsonSchema(schemaMap.get(namespace), namespace);
            definitionsDoc.include(filename);
        }

        final String filename = definitionsNamespace + ADOC_EXTENSION;
        definitionsDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a JSON schema definition.
     *
     * @param schema Schema as a {@link JsonValue}
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputJsonSchema(final JsonValue schema, final String parentNamespace)
            throws IOException {
        final String jsonSchemaNamespace = normalizeName(parentNamespace, "jsonSchema");
        final AsciiDoc jsonSchemaDoc = asciiDoc()
                .listingBlock(OBJECT_MAPPER.writeValueAsString(schema.getObject()), "json");

        final String filename = jsonSchemaNamespace + ADOC_EXTENSION;
        jsonSchemaDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Outputs an AsciiDoc file containing all defined errors.
     *
     * @param errors Errors
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputErrors(final Errors errors, final String parentNamespace) throws IOException {
        if (errors == null) {
            return null;
        }

        final String errorsNamespace = normalizeName(parentNamespace, "errors");
        final AsciiDoc errorsDoc = asciiDoc()
                .sectionTitle1("Errors");

        final List<Map.Entry<String, Error>> errorEntries = new ArrayList<>(errors.getErrors().entrySet());
        Collections.sort(errorEntries, Errors.ERROR_ENTRY_COMPARATOR);

        for (final Map.Entry<String, Error> errorEntry : errorEntries) {
            // namespace for named errors will resolve via JSON References
            final String namespace = normalizeName(errorsNamespace, errorEntry.getKey());
            outputError(errorEntry.getValue(), namespace, namespace, errorsDoc);
        }

        final String filename = errorsNamespace + ADOC_EXTENSION;
        errorsDoc.toFile(outputDirPath, filename);
        return filename;
    }

    /**
     * Resolves a valid AsciiDoc anchor to the Schema in the Definitions section.
     *
     * @param schema Schema definition
     * @param parentNamespace Parent namespace
     * @return AsciiDoc anchor to the Schema definition
     */
    private String resolveSchemaAnchor(final Schema schema, final String parentNamespace) {
        final String anchor;
        if (schema.getReference() != null) {
            // JSON Reference should normalize to appropriate spot in Definitions
            anchor = normalizeName(schema.getReference().getValue());
        } else {
            // register the schema, so that it will be included in Definitions section
            anchor = normalizeName(parentNamespace, "resourceSchema");
            schemaMap.put(anchor, schema.getSchema());
        }
        return anchor;
    }

    /**
     * Checks for a given {@code filename} within {@link #inputDirPath}, and if it exists, will copy that file
     * into {@link #outputDirPath}.
     *
     * @param filename Filename
     * @return {@code true} if a replacement existed and was copied and {@code false} otherwise
     * @throws IOException Unable to copy file
     */
    private boolean copyReplacementFile(final String filename) throws IOException {
        if (inputDirPath != null) {
            final Path inputFilePath = inputDirPath.resolve(filename);
            final Path outputFilePath = outputDirPath.resolve(filename);
            if (Files.exists(inputFilePath)) {
                // replacement file exists, so copy it
                Files.copy(inputFilePath, outputFilePath, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        }
        return false;
    }

}
