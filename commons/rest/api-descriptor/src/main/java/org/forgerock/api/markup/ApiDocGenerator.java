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

import static java.util.Collections.*;
import static org.forgerock.api.markup.asciidoc.AsciiDoc.*;
import static org.forgerock.api.markup.asciidoc.AsciiDocTable.*;
import static org.forgerock.api.markup.asciidoc.AsciiDocTableColumnStyles.*;
import static org.forgerock.api.util.PathUtil.*;
import static org.forgerock.api.util.ValidationUtil.*;
import static org.forgerock.util.Reject.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.markup.asciidoc.AsciiDoc;
import org.forgerock.api.markup.asciidoc.AsciiDocTable;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Operation;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Services;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.util.ReferenceResolver;
import org.forgerock.api.util.ValidationUtil;
import org.forgerock.http.routing.Version;
import org.forgerock.http.util.Json;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Generates static AsciiDoc documentation for CREST API Descriptors.
 */
public final class ApiDocGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModules(new Json.LocalizableStringModule(), new Json.JsonValueModule());

    private static final PreferredLocales PREFERRED_LOCALES = new PreferredLocales();

    enum CrestMethod {
        CREATE, READ, UPDATE, DELETE, PATCH, ACTION, QUERY
    }

    /**
     * Regex pattern that matches services names with format,
     * <ul>
     * <li>name:1</li>
     * <li>name:1.0</li>
     * </ul>
     * Where match group 1 contains the {@code name} and group 2 contains the version.
     */
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("^([^:]+)[:](\\d(?:\\.\\d)?)$");

    /**
     * {@code .adoc} file extension for generated AsciiDoc files.
     */
    private static final String ADOC_EXTENSION = ".adoc";

    /**
     * {@code true} when there is no {@link #outputDirPath filesystem} to write to, and we must build the complete
     * AsciiDoc as a string.
     */
    private final boolean inMemoryMode;

    /**
     * Root output directory.
     */
    private final Path outputDirPath;

    /**
     * Optional input directory or {@code null}.
     */
    private final Path inputDirPath;

    /**
     * Group doc sections by path-name, version, resource-ADOC-filename.
     */
    private final Map<String, Map<Version, String>> pathTree;

    /**
     * Lookup map of rendered AsciiDoc string, for in-memory mode, from filename (key) to AsciiDoc (value).
     */
    private final Map<String, String> adocMap;

    /**
     * Entries from {@link ApiDescription#getServices()} that have been referenced, whereas unreferenced services
     * will be listed under the {@code <undefined>} path in the documentation.
     */
    private final Set<Resource> referencedServices;

    private final ApiDescription apiDescription;

    private final ReferenceResolver referenceResolver;

    /**
     * Constructor that sets the root output directory for AsciiDoc files, which will be created if it does not exist,
     * and an input directory used for overriding AsciiDoc files (e.g., descriptions).
     *
     * @param inputDirPath Input directory or {@code null}
     * @param outputDirPath Root output directory or {@code null} for in-memory mode
     */
    private ApiDocGenerator(final ApiDescription apiDescription, final Path inputDirPath, final Path outputDirPath,
            final ApiDescription... externalApiDescriptions) {

        pathTree = new HashMap<>();
        adocMap = new HashMap<>();
        referencedServices = new HashSet<>();
        this.apiDescription = checkNotNull(apiDescription, "apiDescription required");
        this.inputDirPath = inputDirPath;
        this.outputDirPath = outputDirPath;
        inMemoryMode = outputDirPath == null;

        if (outputDirPath != null && outputDirPath.equals(inputDirPath)) {
            throw new ApiDocGeneratorException("inputDirPath and outputDirPath can not be equal");
        }

        referenceResolver = new ReferenceResolver(apiDescription);
        referenceResolver.registerAll(externalApiDescriptions);
    }

    /**
     * Generates AsciiDoc documentation for a CREST API Descriptor, to an output-directory.
     *
     * @param title API title
     * @param apiDescription API Description
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @param inputDirPath Input directory or {@code null} if not overriding ADOC files
     * @param outputDirPath Root output directory
     */
    public static void execute(final String title, final ApiDescription apiDescription,
            final Path inputDirPath, final Path outputDirPath, final ApiDescription... externalApiDescriptions) {

        final ApiDocGenerator thisInstance = new ApiDocGenerator(apiDescription, inputDirPath, outputDirPath,
                externalApiDescriptions);
        thisInstance.doExecute(title);
    }

    /**
     * Generates AsciiDoc documentation for a CREST API Descriptor, to a {@code String}.
     *
     * @param title API title
     * @param apiDescription API Description
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @param inputDirPath Input directory or {@code null} if not overriding ADOC files
     * @return Resulting AsciiDoc markup as a {@code String}
     */
    public static String execute(final String title, final ApiDescription apiDescription,
            final Path inputDirPath, final ApiDescription... externalApiDescriptions) {

        final ApiDocGenerator thisInstance = new ApiDocGenerator(apiDescription, inputDirPath, null,
                externalApiDescriptions);
        final String rootFilename = thisInstance.doExecute(title);
        return thisInstance.toString(rootFilename);
    }

    private String doExecute(final String title) {
        final String namespace = apiDescription.getId();
        try {
            final String pathsFilename = outputPaths(namespace);
            return outputRoot(checkNotNull(title, "title is required"), pathsFilename, namespace);
        } catch (IOException e) {
            throw new ApiDocGeneratorException("Unable to output doc file", e);
        }
    }

    /**
     * Outputs a top-level AsciiDoc file that imports all other second-level files generated by this class.
     *
     * @param title API title
     * @param pathsFilename Paths file-path suitable for AsciiDoc import-statement
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputRoot(final String title, final String pathsFilename, final String parentNamespace)
            throws IOException {
        final String namespace = normalizeName(parentNamespace, "index");

        final AsciiDoc pathsDoc = asciiDoc()
                .documentTitle(title)
                .rawParagraph(asciiDoc().rawText("*API ID:* ").mono(apiDescription.getId()).toString())
                .rawParagraph(asciiDoc().rawText("*API Version:* ").mono(apiDescription.getVersion()).toString())
                .rawLine(":toc: left")
                .rawLine(":toclevels: 5")
                .newline();
        final String description = toTranslatedString(apiDescription.getDescription());
        final String descriptionFilename = outputDescriptionBlock(description, namespace);
        pathsDoc.include(descriptionFilename);

        if (pathsFilename != null) {
            pathsDoc.include(pathsFilename);
        }

        final String filename = namespace + ADOC_EXTENSION;
        if (inMemoryMode) {
            adocMap.put(filename, pathsDoc.toString());
        } else {
            pathsDoc.toFile(outputDirPath, filename);
        }
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
            if (inMemoryMode) {
                adocMap.put(filename, blockDoc.toString());
            } else {
                blockDoc.toFile(outputDirPath, filename);
            }
        }
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for each path, which imports a file for each version under that path, and another
     * file that imports each path.
     *
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputPaths(final String parentNamespace) throws IOException {
        final String allPathsDocNamespace = normalizeName(parentNamespace, "paths");
        final AsciiDoc allPathsDoc = asciiDoc()
                .sectionTitle1("Paths");

        final Paths paths = apiDescription.getPaths();
        final List<String> pathNames = new ArrayList<>(paths.getNames());
        Collections.sort(pathNames);
        for (final String pathName : pathNames) {
            // path
            final String pathDocNamespace = normalizeName(allPathsDocNamespace, pathName);

            final VersionedPath versionedPath = paths.get(pathName);
            final List<Version> versions = new ArrayList<>(versionedPath.getVersions());
            Collections.sort(versions);

            for (final Version version : versions) {
                final String versionDocNamespace;
                if (VersionedPath.UNVERSIONED.equals(version)) {
                    versionDocNamespace = pathDocNamespace;
                } else {
                    // version
                    final String versionName = version.toString();
                    versionDocNamespace = normalizeName(pathDocNamespace, versionName);
                }

                // resource path
                Resource resource = versionedPath.get(version);
                if (resource.getReference() != null) {
                    resource = referenceResolver.getService(resource.getReference());
                    referencedServices.add(resource);
                }
                final String resourceFilename = outputResource(pathName, version, resource,
                        Collections.<Parameter>emptyList(), versionDocNamespace);
                addPathResource(pathName, version, resourceFilename);
            }
        }

        outputUndefinedServices(allPathsDocNamespace);

        // output paths and versions by traversing pathTree
        final List<String> pathKeys = new ArrayList<>(pathTree.keySet());
        Collections.sort(pathKeys);
        for (final String pathKey : pathKeys) {
            allPathsDoc.sectionTitle2(asciiDoc().mono(pathKey).toString());

            final Map<Version, String> versionMap = pathTree.get(pathKey);
            for (final Map.Entry<Version, String> entry : versionMap.entrySet()) {
                if (!VersionedPath.UNVERSIONED.equals(entry.getKey())) {
                    allPathsDoc.sectionTitle3(asciiDoc().mono(entry.getKey().toString()).toString());
                }
                allPathsDoc.include(entry.getValue());
            }
        }

        // output all-paths-file
        final String filename = allPathsDocNamespace + ADOC_EXTENSION;
        if (inMemoryMode) {
            adocMap.put(filename, allPathsDoc.toString());
        } else {
            allPathsDoc.toFile(outputDirPath, filename);
        }
        return filename;
    }

    /**
     * Search for unreferenced services, and add them to the documentation under the {@code <undefined>} path.
     *
     * @param parentNamespace Parent namespace
     * @throws IOException Unable to output AsciiDoc file
     */
    private void outputUndefinedServices(final String parentNamespace) throws IOException {
        //
        Services services = apiDescription.getServices();
        if (services != null && !services.getNames().isEmpty()) {
            for (final String name : apiDescription.getServices().getNames()) {
                final Resource resource = apiDescription.getServices().get(name);
                if (!referencedServices.contains(resource)) {
                    // parse service version, from end of name, if provided
                    final Matcher m = SERVICE_NAME_PATTERN.matcher(name);
                    final String serviceName;
                    final Version serviceVersion;
                    if (m.matches()) {
                        serviceName = m.group(1);
                        serviceVersion = Version.version(m.group(2));
                    } else {
                        serviceName = name;
                        serviceVersion = VersionedPath.UNVERSIONED;
                    }

                    final String pathDocNamespace = normalizeName(parentNamespace, "<undefined>");
                    final String versionDocNamespace;
                    if (VersionedPath.UNVERSIONED.equals(serviceVersion)) {
                        versionDocNamespace = pathDocNamespace;
                    } else {
                        // version
                        final String versionName = serviceVersion.toString();
                        versionDocNamespace = normalizeName(pathDocNamespace, versionName);
                    }

                    final String pathName = "<undefined>/" + serviceName;
                    final String resourceFilename = outputResource(pathName, serviceVersion, resource,
                            Collections.<Parameter>emptyList(), versionDocNamespace);
                    addPathResource(pathName, serviceVersion, resourceFilename);
                }
            }
        }
    }

    private String outputResource(final String pathName, final Version version, final Resource resource,
            final List<Parameter> parameters, final String parentNamespace) throws IOException {
        final boolean unversioned = VersionedPath.UNVERSIONED.equals(version);
        final int sectionLevel = unversioned ? 3 : 4;

        final String namespace = normalizeName(parentNamespace, "resource");
        final AsciiDoc resourceDoc = asciiDoc();

        final String descriptionFilename = outputDescriptionBlock(
                resource.getDescription().toTranslatedString(PREFERRED_LOCALES), namespace);
        resourceDoc.include(descriptionFilename);

        outputOperation(CrestMethod.CREATE, resource, sectionLevel, parameters, namespace, resourceDoc);
        outputOperation(CrestMethod.READ, resource, sectionLevel, parameters, namespace, resourceDoc);
        outputOperation(CrestMethod.UPDATE, resource, sectionLevel, parameters, namespace, resourceDoc);
        outputOperation(CrestMethod.DELETE, resource, sectionLevel, parameters, namespace, resourceDoc);
        outputOperation(CrestMethod.PATCH, resource, sectionLevel, parameters, namespace, resourceDoc);

        outputActionOperations(resource, sectionLevel, parameters, namespace, resourceDoc);
        outputQueryOperations(resource, sectionLevel, parameters, namespace, resourceDoc);

        // collections path
        outputItems(version, resource, parameters, pathName, parentNamespace);

        // sub-resource paths
        outputSubResources(version, resource.getSubresources(), parameters, pathName, parentNamespace);

        // output resource-file
        final String resourceFilename = namespace + ADOC_EXTENSION;
        if (inMemoryMode) {
            adocMap.put(resourceFilename, resourceDoc.toString());
        } else {
            resourceDoc.toFile(outputDirPath, resourceFilename);
        }
        return resourceFilename;
    }

    private void outputItems(final Version version, final Resource resource, final List<Parameter> parameters,
            final String parentPathName, final String parentNamespace) throws IOException {
        Items items = resource.getItems();
        if (items != null) {
            Parameter pathParameter = items.getPathParameter();
            final String itemsPathName = parentPathName + "/{" + pathParameter.getName() + "}";
            final String itemsPathDocNamespace = normalizeName(parentNamespace, itemsPathName);

            final Resource itemsResource = items.asResource(resource.isMvccSupported(),
                    resource.getResourceSchema(), resource.getTitle(), resource.getDescription());

            final List<Parameter> itemsParameters = mergeParameters(mergeParameters(new ArrayList<>(parameters),
                    resource.getParameters()), pathParameter);

            outputSubResources(version, items.getSubresources(), itemsParameters, itemsPathName, itemsPathDocNamespace);

            final String resourceFilename = outputResource(parentPathName, version,
                    itemsResource, itemsParameters, itemsPathDocNamespace);
            addPathResource(itemsPathName, version, resourceFilename);
        }
    }

    private void outputSubResources(final Version version, final SubResources subResources,
            final List<Parameter> parameters, final String parentPathName, final String parentNamespace)
            throws IOException {
        if (subResources != null) {
            final List<String> subPathNames = new ArrayList<>(subResources.getNames());
            Collections.sort(subPathNames);

            for (final String name : subPathNames) {
                final String subPathName = buildPath(parentPathName, name);

                // create path-parameters, for any path-variables found in subPathName
                final List<Parameter> subresourcesParameters = unmodifiableList(mergeParameters(
                        new ArrayList<>(parameters), buildPathParameters(subPathName)));

                final String subPathDocNamespace = normalizeName(parentNamespace, subPathName);

                Resource subResource = subResources.get(name);
                if (subResource.getReference() != null) {
                    subResource = referenceResolver.getService(subResource.getReference());
                    referencedServices.add(subResource);
                }

                final String resourceFilename = outputResource(subPathName, version,
                        subResource, subresourcesParameters, subPathDocNamespace);
                addPathResource(subPathName, version, resourceFilename);
            }
        }
    }

    /**
     * Outputs an AsciiDoc file for various CREST operations, and a file that imports each of those files.
     *
     * @param crestMethod CREST operation-method
     * @param resource Resource
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @param parentDoc Parent AsciiDoc
     * @throws IOException Unable to output AsciiDoc file
     */
    private void outputOperation(final CrestMethod crestMethod, final Resource resource, final int sectionLevel,
            final List<Parameter> parameters, final String parentNamespace, final AsciiDoc parentDoc)
            throws IOException {

        final Operation operation;
        final boolean responseOnly;
        final String displayName;
        switch (crestMethod) {
        case CREATE:
            displayName = "Create";
            responseOnly = false;
            operation = resource.getCreate();
            break;
        case READ:
            displayName = "Read";
            responseOnly = true;
            operation = resource.getRead();
            break;
        case UPDATE:
            displayName = "Update";
            responseOnly = false;
            operation = resource.getUpdate();
            break;
        case DELETE:
            displayName = "Delete";
            responseOnly = true;
            operation = resource.getDelete();
            break;
        case PATCH:
            displayName = "Patch";
            responseOnly = true;
            operation = resource.getPatch();
            break;
        default:
            // this method only handles a subset of CREST methods
            throw new ApiDocGeneratorException("Unsupported CREST method: " + crestMethod);
        }

        if (operation != null) {
            final String namespace = normalizeName(parentNamespace, displayName);
            final AsciiDoc operationDoc = asciiDoc()
                    .sectionTitle(displayName, sectionLevel);
            final String description = toTranslatedString(operation.getDescription());
            final String descriptionFilename = outputDescriptionBlock(description, namespace);
            operationDoc.include(descriptionFilename);

            final List<String> headers = new ArrayList<>();
            final List<Integer> columnWidths = new ArrayList<>();
            final AsciiDocTable table = operationDoc.tableStart();
            outputStability(operation.getStability(), table, headers, columnWidths);
            outputMvccSupport(resource.isMvccSupported(), table, headers, columnWidths);
            if (crestMethod == CrestMethod.CREATE) {
                final Create create = resource.getCreate();
                outputCreateMode(create.getMode(), table, headers, columnWidths);
                outputSingletonStatus(create.isSingleton(), table, headers, columnWidths);
            } else if (crestMethod == CrestMethod.PATCH) {
                final Patch patch = resource.getPatch();
                outputSupportedPatchOperations(patch.getOperations(), table, headers, columnWidths);
            }
            table.headers(headers)
                    .columnWidths(columnWidths)
                    .tableEnd();

            outputParameters(operation.getParameters(), parameters, namespace, operationDoc);
            outputResourceEntity(resource, responseOnly, operationDoc);
            outputErrors(operation.getApiErrors(), operationDoc);

            parentDoc.horizontalRule();

            if (inMemoryMode) {
                parentDoc.rawText(operationDoc.toString());
            } else {
                final String filename = namespace + ADOC_EXTENSION;
                operationDoc.toFile(outputDirPath, filename);
                parentDoc.include(filename);
            }
        }
    }

    /**
     * Outputs an AsciiDoc file for {@link Action}-operations, and a file that imports each of those files.
     *
     * @param resource Resource
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @param parentDoc Parent AsciiDoc
     * @throws IOException Unable to output AsciiDoc file
     */
    private void outputActionOperations(final Resource resource, final int sectionLevel,
            final List<Parameter> parameters, final String parentNamespace, final AsciiDoc parentDoc)
            throws IOException {
        if (!isEmpty(resource.getActions())) {
            final String namespace = normalizeName(parentNamespace, "action");
            final AsciiDoc operationDoc = asciiDoc();

            for (final Action action : resource.getActions()) {
                final String filename = outputActionOperation(resource, action, sectionLevel, parameters, namespace);
                operationDoc.include(filename);
            }

            if (inMemoryMode) {
                parentDoc.rawText(operationDoc.toString());
            } else {
                final String filename = namespace + ADOC_EXTENSION;
                operationDoc.toFile(outputDirPath, filename);
                parentDoc.include(filename);
            }
        }
    }

    /**
     * Outputs an AsciiDoc file a single {@link Action}-operation, and a file for that action.
     *
     * @param resource Resource
     * @param action Action operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputActionOperation(final Resource resource, final Action action, final int sectionLevel,
            final List<Parameter> parameters, final String parentNamespace) throws IOException {
        final String namespace = normalizeName(parentNamespace, action.getName());
        final AsciiDoc operationDoc = asciiDoc()
                .horizontalRule()
                .sectionTitle(asciiDoc().rawText("Action: ").mono(action.getName()).toString(), sectionLevel);
        final String description = toTranslatedString(action.getDescription());
        final String descriptionFilename = outputDescriptionBlock(description, namespace);
        operationDoc.include(descriptionFilename);

        final List<String> headers = new ArrayList<>();
        final List<Integer> columnWidths = new ArrayList<>();
        final AsciiDocTable table = operationDoc.tableStart();
        outputStability(action.getStability(), table, headers, columnWidths);
        outputMvccSupport(resource.isMvccSupported(), table, headers, columnWidths);
        table.headers(headers)
                .columnWidths(columnWidths)
                .tableEnd();

        outputParameters(action.getParameters(), parameters, namespace, operationDoc);

        if (action.getRequest() != null) {
            final Schema schema = action.getRequest().getReference() == null
                    ? action.getRequest()
                    : referenceResolver.getDefinition(action.getRequest().getReference());

            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Request Entity")
                    .rawParagraph("This operation takes a request resource that conforms to the following schema:")
                    .listingBlock(OBJECT_MAPPER.writeValueAsString(schema.getSchema().getObject()), "json");
            operationDoc.rawText(blockDoc.toString());
        }

        if (action.getResponse() != null) {
            final Schema schema = action.getResponse().getReference() == null
                    ? action.getResponse()
                    : referenceResolver.getDefinition(action.getResponse().getReference());

            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Response Entity")
                    .rawParagraph("This operation returns a response resource that conforms to the following schema:")
                    .listingBlock(OBJECT_MAPPER.writeValueAsString(schema.getSchema().getObject()), "json");
            operationDoc.rawText(blockDoc.toString());
        }

        outputErrors(action.getApiErrors(), operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        if (inMemoryMode) {
            adocMap.put(filename, operationDoc.toString());
        } else {
            operationDoc.toFile(outputDirPath, filename);
        }
        return filename;
    }

    /**
     * Outputs an AsciiDoc file for a group of {@link Query}-operations, and a file that imports each of those files.
     *
     * @param resource Resource
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @param parentDoc Parent AsciiDoc
     * @throws IOException Unable to output AsciiDoc file
     */
    private void outputQueryOperations(final Resource resource, final int sectionLevel,
            final List<Parameter> parameters, final String parentNamespace,
            final AsciiDoc parentDoc) throws IOException {
        if (!isEmpty(resource.getQueries())) {
            final String namespace = normalizeName(parentNamespace, "query");
            final AsciiDoc operationDoc = asciiDoc();

            for (final Query query : resource.getQueries()) {
                final String filename = outputQueryOperation(resource, query, sectionLevel, parameters,
                        namespace);
                operationDoc.include(filename);
            }

            final String filename = namespace + ADOC_EXTENSION;
            if (inMemoryMode) {
                adocMap.put(filename, operationDoc.toString());
            } else {
                operationDoc.toFile(outputDirPath, filename);
            }
            parentDoc.include(filename);
        }
    }

    /**
     * Outputs an AsciiDoc file for a single {@link Query}-operation.
     *
     * @param resource Resource
     * @param query Query operation
     * @param sectionLevel Starting <a href="http://asciidoctor.org/docs/user-manual/#sections">section</a>-level
     * @param parameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @return File path suitable for AsciiDoc import-statement
     * @throws IOException Unable to output AsciiDoc file
     */
    private String outputQueryOperation(final Resource resource, final Query query, final int sectionLevel,
            final List<Parameter> parameters, final String parentNamespace) throws IOException {
        final String namespace;
        final AsciiDoc operationDoc = asciiDoc()
                .horizontalRule();

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

        final String descriptionFilename = outputDescriptionBlock(
                query.getDescription().toTranslatedString(PREFERRED_LOCALES), namespace);
        operationDoc.include(descriptionFilename);

        final List<String> headers = new ArrayList<>();
        final List<Integer> columnWidths = new ArrayList<>();
        final AsciiDocTable table = operationDoc.tableStart();
        outputStability(query.getStability(), table, headers, columnWidths);
        outputMvccSupport(resource.isMvccSupported(), table, headers, columnWidths);

        if (!isEmpty(query.getQueryableFields())) {
            headers.add(asciiDoc().link("query-queryable-fields", "Queryable Fields").toString());
            columnWidths.add(COLUMN_WIDTH_MEDIUM);

            final AsciiDoc blockDoc = asciiDoc();
            for (final String field : query.getQueryableFields()) {
                blockDoc.unorderedList1(asciiDoc().mono(field).toString());
            }
            table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
        }

        if (!isEmpty(query.getPagingModes())) {
            headers.add(asciiDoc().link("query-paging-modes", "Paging Modes").toString());
            columnWidths.add(COLUMN_WIDTH_MEDIUM);

            final AsciiDoc blockDoc = asciiDoc();
            for (final PagingMode pagingMode : query.getPagingModes()) {
                blockDoc.unorderedList1(asciiDoc().mono(pagingMode.toString()).toString());
            }
            table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
        }

        if (!isEmpty(query.getCountPolicies())) {
            headers.add(asciiDoc().link("query-page-count-policies", "Page Count Policies").toString());
            columnWidths.add(COLUMN_WIDTH_MEDIUM);

            final AsciiDoc blockDoc = asciiDoc();
            for (final CountPolicy countPolicy : query.getCountPolicies()) {
                blockDoc.unorderedList1(asciiDoc().mono(countPolicy.toString()).toString());
            }
            table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
        }

        if (!isEmpty(query.getSupportedSortKeys())) {
            headers.add(asciiDoc().link("query-sort-keys", "Supported Sort Keys").toString());
            columnWidths.add(COLUMN_WIDTH_MEDIUM);

            final AsciiDoc blockDoc = asciiDoc();
            for (final String sortKey : query.getSupportedSortKeys()) {
                blockDoc.unorderedList1(asciiDoc().mono(sortKey).toString());
            }
            table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
        }

        table.headers(headers)
                .columnWidths(columnWidths)
                .tableEnd();

        outputParameters(query.getParameters(), parameters, namespace, operationDoc);

        // TODO determine if this needs to be formatted differently here
        outputResourceEntity(resource, true, operationDoc);

        outputErrors(query.getApiErrors(), operationDoc);

        final String filename = namespace + ADOC_EXTENSION;
        if (inMemoryMode) {
            adocMap.put(filename, operationDoc.toString());
        } else {
            operationDoc.toFile(outputDirPath, filename);
        }
        return filename;
    }

    /**
     * Outputs operation stability.
     *
     * @param stability Operation stability or {@code null} to use default {@link Stability#STABLE}
     * @param table AsciiDoc table to write to
     * @param headers Table headers, which will have an entry added
     * @param columnWidths Relative table column-widths (range [1,99]), which will have an entry added
     */
    private static void outputStability(Stability stability, final AsciiDocTable table, final List<String> headers,
            final List<Integer> columnWidths) {
        if (stability == null) {
            stability = Stability.STABLE;
        }

        headers.add(asciiDoc().link("interface-stability-definitions", "Stability").toString());
        columnWidths.add(COLUMN_WIDTH_SMALL);

        table.columnCell(stability.name());
    }

    /**
     * Outputs MVCC support.
     *
     * @param mvccSupported MVCC support flag
     * @param table AsciiDoc table to write to
     * @param headers Table headers, which will have an entry added
     * @param columnWidths Relative table column-widths (range [1,99]), which will have an entry added
     */
    private static void outputMvccSupport(final boolean mvccSupported, final AsciiDocTable table,
            final List<String> headers, final List<Integer> columnWidths) {
        headers.add(asciiDoc().link("MVCC", "MVCC").toString());
        columnWidths.add(COLUMN_WIDTH_SMALL);

        // ✓ or ⃠
        table.columnCell(mvccSupported ? "&#10003;" : "&#8416;");
    }

    /**
     * Outputs an operation's resource schema.
     *
     * @param resource Resource
     * @param responseOnly {@code true} when resource is sent only in response and {@code false} for request/response
     * @param doc AsciiDoc to write to
     */
    private void outputResourceEntity(final Resource resource, final boolean responseOnly, final AsciiDoc doc)
            throws IOException {
        if (resource.getResourceSchema() != null) {
            final Schema schema = resource.getResourceSchema().getReference() == null
                    ? resource.getResourceSchema()
                    : referenceResolver.getDefinition(resource.getResourceSchema().getReference());
            final AsciiDoc blockDoc = asciiDoc()
                    .blockTitle("Resource Entity");
            if (responseOnly) {
                blockDoc.rawParagraph(
                        "This operation returns a response resource that conforms to the following schema:");
            } else {
                blockDoc.rawParagraph("This operation takes a request body and returns a response resource that "
                        + "conforms to the following schema:");
            }
            blockDoc.listingBlock(OBJECT_MAPPER.writeValueAsString(schema.getSchema().getObject()), "json");

            doc.rawText(blockDoc.toString());
        }
    }

    /**
     * Outputs singleton status for {@link Create} operation.
     *
     * @param isSingleton Singleton status
     * @param table AsciiDoc table to write to
     * @param headers Table headers, which will have an entry added
     * @param columnWidths Relative table column-widths (range [1,99]), which will have an entry added
     */
    private static void outputSingletonStatus(final boolean isSingleton, final AsciiDocTable table,
            final List<String> headers, final List<Integer> columnWidths) {
        headers.add(asciiDoc().link("create-singleton", "Singleton").toString());
        columnWidths.add(COLUMN_WIDTH_SMALL);

        // ✓ or ⃠
        table.columnCell(isSingleton ? "&#10003;" : "&#8416;");
    }

    /**
     * Outputs operation parameters.
     *
     * @param operationParameters Operation parameters or {@code null}/empty for pass-through
     * @param inheritedParameters Inherited CREST operation parameters
     * @param parentNamespace Parent namespace
     * @param doc AsciiDoc to write to
     */
    private void outputParameters(final Parameter[] operationParameters, final List<Parameter> inheritedParameters,
            final String parentNamespace, final AsciiDoc doc)
            throws IOException {
        final List<Parameter> parameters = mergeParameters(new ArrayList<>(inheritedParameters), operationParameters);
        if (parameters.isEmpty()) {
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
            final String description = toTranslatedString(parameter.getDescription());
            final String descriptionFilename = outputDescriptionBlock(description, namespace);

            // format table
            table.columnCell(parameter.getName(), MONO_CELL)
                    .columnCell(parameter.getType(), MONO_CELL)
                    .columnCell(asciiDoc().include(descriptionFilename).toString(), ASCII_DOC_CELL)
                    .columnCell(ValidationUtil.nullToFalse(parameter.isRequired()) ? "&#10003;" : null)
                    .columnCell(parameter.getSource().name(), MONO_CELL)
                    .columnCell(enumValuesContent, ASCII_DOC_CELL)
                    .rowEnd();
        }
        table.tableEnd();
    }

    private String toTranslatedString(LocalizableString localizableString) {
        return localizableString == null
                        ? null
                        : localizableString.toTranslatedString(PREFERRED_LOCALES);
    }

    /**
     * Outputs create-mode for a {@link Create} operation.
     *
     * @param createMode Create-mode
     * @param table AsciiDoc table to write to
     * @param headers Table headers, which will have an entry added
     * @param columnWidths Relative table column-widths (range [1,99]), which will have an entry added
     */
    private static void outputCreateMode(final CreateMode createMode, final AsciiDocTable table,
            final List<String> headers, final List<Integer> columnWidths) {
        headers.add("Resource ID");
        columnWidths.add(COLUMN_WIDTH_MEDIUM);

        final AsciiDoc doc = asciiDoc();

        switch (createMode) {
        case ID_FROM_CLIENT:
            doc.rawText("Assigned by client");
            break;
        case ID_FROM_SERVER:
            doc.rawText("Assigned by server (do not supply)");
            break;
        default:
            throw new ApiDocGeneratorException("Unsupported CreateMode: " + createMode);
        }

        table.columnCell(doc.toString());
    }

    /**
     * Outputs supported patch-operations for a {@link Patch} operation.
     *
     * @param patchOperations Supported patch-operations
     * @param table AsciiDoc table to write to
     * @param headers Table headers, which will have an entry added
     * @param columnWidths Relative table column-widths (range [1,99]), which will have an entry added
     */
    private static void outputSupportedPatchOperations(final PatchOperation[] patchOperations,
            final AsciiDocTable table, final List<String> headers, final List<Integer> columnWidths) {
        headers.add(asciiDoc().link("patch-operations", "Patch Operations").toString());
        columnWidths.add(COLUMN_WIDTH_MEDIUM);

        final AsciiDoc blockDoc = asciiDoc();
        for (final PatchOperation patchOperation : patchOperations) {
            blockDoc.unorderedList1(patchOperation.name());
        }
        table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
    }

    /**
     * Outputs operation errors.
     *
     * @param apiErrors Operation errors or {@code null}/empty for pass-through
     * @param doc AsciiDoc to write to
     */
    private void outputErrors(final ApiError[] apiErrors, final AsciiDoc doc) throws IOException {
        if (!isEmpty(apiErrors)) {
            doc.blockTitle("Errors");
            final AsciiDocTable table = doc.tableStart()
                    .headers("Code", "Description")
                    .columnWidths(1, 10);

            // resolve error references before sorting
            final List<ApiError> resolvedErrors = new ArrayList<>(apiErrors.length);
            for (final ApiError error : apiErrors) {
                if (error.getReference() != null) {
                    final ApiError resolved = referenceResolver.getError(error.getReference());
                    if (resolved != null && resolved.getReference() == null) {
                        resolvedErrors.add(resolved);
                    }
                } else {
                    resolvedErrors.add(error);
                }
            }
            Collections.sort(resolvedErrors, ApiError.ERROR_COMPARATOR);

            for (final ApiError error : resolvedErrors) {
                table.columnCell(String.valueOf(error.getCode()), MONO_CELL);
                if (error.getSchema() == null) {
                    table.columnCell(error.getDescription().toTranslatedString(PREFERRED_LOCALES));
                } else {
                    final Schema schema = error.getSchema().getReference() == null
                            ? error.getSchema()
                            : referenceResolver.getDefinition(error.getSchema().getReference());

                    final AsciiDoc blockDoc = asciiDoc()
                            .rawParagraph(error.getDescription().toTranslatedString(PREFERRED_LOCALES))
                            .rawParagraph("This error may contain an underlying `cause` that conforms to the following "
                                    + "schema:")
                            .listingBlock(OBJECT_MAPPER.writeValueAsString(schema.getSchema().getObject()), "json");
                    table.columnCell(blockDoc.toString(), ASCII_DOC_CELL);
                }
                table.rowEnd();
            }
            table.tableEnd();
        }
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

    /**
     * Add resource-file to path tree.
     *
     * @param pathName Full endpoint path.
     * @param version Resource version.
     * @param resourceFilename Resource ADOC filename.
     */
    private void addPathResource(final String pathName, final Version version, final String resourceFilename) {
        if (!pathTree.containsKey(pathName)) {
            pathTree.put(pathName, new LinkedHashMap<Version, String>());
        }
        final Map<Version, String> versionTree = pathTree.get(pathName);
        if (!versionTree.containsKey(version)) {
            versionTree.put(version, resourceFilename);
        }
    }

    /**
     * Merges AsciiDoc include-statements, to build AsciiDoc string. This method is only invoked when
     * {@link #inMemoryMode} is {@code true}.
     *
     * @param rootFilename Name of root AsciiDoc file
     * @return AsciiDoc string
     */
    private String toString(final String rootFilename) {
        if (!inMemoryMode) {
            throw new ApiDocGeneratorException("Expected inMemoryMode");
        }
        String s = adocMap.get(checkNotNull(rootFilename));
        final Matcher m = AsciiDoc.INCLUDE_PATTERN.matcher(s);
        while (m.find()) {
            final String content = adocMap.get(m.group(1));
            s = m.replaceFirst(isEmpty(content) ? "" : Matcher.quoteReplacement(content));
            m.reset(s);
        }
        return s;
    }

}
