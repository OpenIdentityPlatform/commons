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
package org.forgerock.api.transform;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.forgerock.api.markup.asciidoc.AsciiDoc.normalizeName;
import static org.forgerock.api.util.PathUtil.buildPath;
import static org.forgerock.api.util.PathUtil.buildPathParameters;
import static org.forgerock.api.util.PathUtil.mergeParameters;
import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.listOf;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.markup.asciidoc.AsciiDoc;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.util.PathUtil;
import org.forgerock.api.util.ReferenceResolver;
import org.forgerock.api.util.ValidationUtil;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.Version;
import org.forgerock.http.swagger.SwaggerExtended;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.util.Function;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

/**
 * Transforms an {@link ApiDescription} into an OpenAPI/Swagger model.
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md">OpenAPI 2.0</a> spec
 */
public class OpenApiTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiTransformer.class);

    private static final String EMPTY_STRING = "";

    private static final String PARAMETER_FIELDS = "_fields";
    private static final String PARAMETER_PRETTY_PRINT = "_prettyPrint";
    private static final String PARAMETER_MIME_TYPE = "_mimeType";
    private static final String PARAMETER_IF_MATCH = "If-Match";
    private static final String PARAMETER_IF_NONE_MATCH = "If-None-Match";
    private static final String PARAMETER_IF_NONE_MATCH_ANY_ONLY = "If-None-Match: *";
    private static final String PARAMETER_IF_NONE_MATCH_REV_ONLY = "If-None-Match: <rev>";
    private static final String PARAMETER_LOCATION = "Location";

    static final String DEFINITIONS_REF = "#/definitions/";
    private static final String I18N_PREFIX = LocalizableString.TRANSLATION_KEY_PREFIX + "ApiDescription#";
    private static final String FIELDS_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.fields";
    private static final String PRETTYPRINT_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.prettyprint";
    private static final String MIMETYPE_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.mimetype";
    private static final String LOCATION_PARAMETER_DESCRIPTION = I18N_PREFIX + "common.parameters.location";

    @VisibleForTesting
    final Swagger swagger;
    private final ReferenceResolver referenceResolver;
    private final ApiDescription apiDescription;
    private final Map<String, Model> definitionMap = new HashMap<>();

    /** {@code Location}-header property. */
    private final LocalizableStringProperty locationProperty = new LocalizableStringProperty()
            .description(new LocalizableString(LOCATION_PARAMETER_DESCRIPTION, getClass().getClassLoader()));

    /** Default constructor that is only used by unit tests. */
    @VisibleForTesting
    OpenApiTransformer() {
        swagger = null;
        referenceResolver = null;
        apiDescription = null;
    }

    /**
     * Constructor.
     *
     * @param title API title
     * @param host Hostname or IP address, with optional port
     * @param basePath Base-path on host
     * @param secure {@code true} when host is using HTTPS and {@code false} when using HTTP
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     */
    @VisibleForTesting
    OpenApiTransformer(final LocalizableString title, final String host, final String basePath, final boolean secure,
            final ApiDescription apiDescription, final ApiDescription... externalApiDescriptions) {
        this.apiDescription = checkNotNull(apiDescription, "apiDescription required");

        swagger = new SwaggerExtended()
                .scheme(secure ? Scheme.HTTPS : Scheme.HTTP)
                .host(host)
                .consumes("application/json")
                .consumes("text/plain")
                .consumes("multipart/form-data")
                .produces("application/json")
                .info(buildInfo(title));

        if (!isEmpty(basePath)) {
            // make sure path starts with forward-slash (OpenAPI 2.0 spec), and does not end with one
            swagger.basePath(PathUtil.buildPath(basePath));
        }

        referenceResolver = new ReferenceResolver(apiDescription);
        if (externalApiDescriptions != null) {
            referenceResolver.registerAll(externalApiDescriptions);
        }
    }

    /**
     * Transforms an {@link ApiDescription} into a {@code Swagger} model.
     *
     * @param title API title
     * @param host Hostname or IP address, with optional port
     * @param basePath Base-path on host
     * @param secure {@code true} when host is using HTTPS and {@code false} when using HTTP
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @return {@code Swagger} model
     */
    public static Swagger execute(final LocalizableString title, final String host, final String basePath,
            final boolean secure, final ApiDescription apiDescription,
            final ApiDescription... externalApiDescriptions) {
        final OpenApiTransformer transformer = new OpenApiTransformer(title, host, basePath, secure, apiDescription,
                externalApiDescriptions);
        return transformer.doExecute();
    }

    /**
     * Transforms an {@link ApiDescription} into a {@code Swagger} model.
     * <p>
     * Note: The returned descriptor does not contain an {@code Info} object, a base path, a host or a scheme, as
     * these will all depend on the deployment and/or request.
     * </p>
     *
     * @param apiDescription CREST API Descriptor
     * @param externalApiDescriptions External CREST API Descriptions, for resolving {@link Reference}s, or {@code null}
     * @return {@code Swagger} model
     */
    public static Swagger execute(ApiDescription apiDescription, ApiDescription... externalApiDescriptions) {
        final OpenApiTransformer transformer = new OpenApiTransformer(null, null, null, false, apiDescription,
                externalApiDescriptions);
        return transformer.doExecute();
    }

    /**
     * Do the work to transform an {@link ApiDescription} into a {@code Swagger} model.
     *
     * @return {@code Swagger} model
     */
    private Swagger doExecute() {
        buildParameters();
        buildPaths();
        buildDefinitions();
        return swagger;
    }

    /** Build globally-defined parameters, which are referred to by-reference. */
    private void buildParameters() {
        ClassLoader loader = getClass().getClassLoader();

        // _fields
        final LocalizableQueryParameter fieldsParameter = new LocalizableQueryParameter();
        fieldsParameter.setName(PARAMETER_FIELDS);
        fieldsParameter.setType("string");
        fieldsParameter.setCollectionFormat("csv");
        fieldsParameter.description(new LocalizableString(FIELDS_PARAMETER_DESCRIPTION, loader));
        swagger.addParameter(fieldsParameter.getName(), fieldsParameter);

        // _prettyPrint
        final LocalizableQueryParameter prettyPrintParameter = new LocalizableQueryParameter();
        prettyPrintParameter.setName(PARAMETER_PRETTY_PRINT);
        prettyPrintParameter.setType("boolean");
        prettyPrintParameter.description(new LocalizableString(PRETTYPRINT_PARAMETER_DESCRIPTION, loader));
        swagger.addParameter(prettyPrintParameter.getName(), prettyPrintParameter);

        // _mimeType
        final LocalizableQueryParameter mimeTypeParameter = new LocalizableQueryParameter();
        mimeTypeParameter.setName(PARAMETER_MIME_TYPE);
        mimeTypeParameter.setType("string");
        mimeTypeParameter.description(new LocalizableString(MIMETYPE_PARAMETER_DESCRIPTION, loader));
        swagger.addParameter(mimeTypeParameter.getName(), mimeTypeParameter);

        // PUT-operation IF-NONE-MATCH always has * value
        final LocalizableHeaderParameter putIfNoneMatchParameter = new LocalizableHeaderParameter();
        putIfNoneMatchParameter.setName(PARAMETER_IF_NONE_MATCH);
        putIfNoneMatchParameter.setType("string");
        putIfNoneMatchParameter.required(true);
        putIfNoneMatchParameter.setEnum(asList("*"));
        swagger.addParameter(PARAMETER_IF_NONE_MATCH_ANY_ONLY, putIfNoneMatchParameter);

        // READ-operation IF-NONE-MATCH cannot have * value
        final LocalizableHeaderParameter readIfNoneMatchParameter = new LocalizableHeaderParameter();
        readIfNoneMatchParameter.setName(PARAMETER_IF_NONE_MATCH);
        readIfNoneMatchParameter.setType("string");
        swagger.addParameter(PARAMETER_IF_NONE_MATCH_REV_ONLY, readIfNoneMatchParameter);

        // IF-MATCH
        final LocalizableHeaderParameter ifMatchParameter = new LocalizableHeaderParameter();
        ifMatchParameter.setName(PARAMETER_IF_MATCH);
        ifMatchParameter.setType("string");
        ifMatchParameter.setDefault("*");
        swagger.addParameter(ifMatchParameter.getName(), ifMatchParameter);
    }

    /** Traverse CREST API Descriptor paths, to build the Swagger model. */
    private void buildPaths() {
        final Paths paths = apiDescription.getPaths();
        if (paths != null) {
            final Map<String, Path> pathMap = new LinkedHashMap<>();
            final List<String> pathNames = new ArrayList<>(paths.getNames());
            Collections.sort(pathNames);
            for (final String pathName : pathNames) {
                final VersionedPath versionedPath = paths.get(pathName);
                final List<Version> versions = new ArrayList<>(versionedPath.getVersions());
                Collections.sort(versions);
                for (final Version version : versions) {
                    final String versionName;
                    if (VersionedPath.UNVERSIONED.equals(version)) {
                        versionName = EMPTY_STRING;
                    } else {
                        // versionName is start of URL-fragment for path (e.g., /myPath#1.0)
                        versionName = version.toString();
                    }

                    final Resource resource = resolveResourceReference(versionedPath.get(version));

                    // make sure path starts with forward-slash (OpenAPI 2.0 spec), and does not end with one
                    final String normalizedPathName = pathName.isEmpty() ? "/" : PathUtil.buildPath(pathName);

                    buildResourcePaths(resource, normalizedPathName, null, versionName,
                            Collections.<Parameter>emptyList(), pathMap);
                }
            }
            swagger.setPaths(pathMap);
        }
    }

    private Resource resolveResourceReference(Resource resource) {
        Reference resourceReference = resource.getReference();
        if (resourceReference != null) {
            resource = referenceResolver.getService(resourceReference);
            if (resource == null) {
                throw new TransformerException("Unresolvable reference: " + resourceReference.getValue());
            }
        }
        return resource;
    }

    /**
     * Constructs paths, for a given resource, and works with OpenAPI's current inability to overload paths for a
     * given REST operation (e.g., multiple {@code get} operations) by adding a URL-fragment {@code #} suffix
     * to the end of the path.
     *
     * @param resource CREST resource
     * @param pathName Resource path-name
     * @param parentTag Tag for grouping operations together by resource/version or {@code null} if there is no parent
     * @param resourceVersion Resource version-name or empty-string
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildResourcePaths(final Resource resource, final String pathName, final LocalizableString parentTag,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, Path> pathMap) {
        // always show version at end of paths, inside the URL fragment
        final boolean hasResourceVersion = !isEmpty(resourceVersion);
        final String pathNamespace = hasResourceVersion
                ? normalizeName(pathName, resourceVersion) : normalizeName(pathName);

        // group resource endpoints by tag
        LocalizableString tag = parentTag;
        if (tag == null || isEmpty(tag.toString())) {
            final LocalizableString title = resource.getTitle();
            final String titleString = title.toString();
            tag = new LocalizableString(hasResourceVersion ? titleString + " v" + resourceVersion : titleString) {
                @Override
                public String toTranslatedString(PreferredLocales locales) {
                    String tag = !isEmpty(titleString)
                            ? title.toTranslatedString(locales)
                            : pathName;
                    if (hasResourceVersion) {
                        tag += " v" + resourceVersion;
                    }
                    return tag;
                }
            };
            swagger.addTag(new LocalizableTag().name(tag));
        }

        Schema resourceSchema = null;
        if (resource.getResourceSchema() != null) {
            resourceSchema = resource.getResourceSchema();
        }

        // resource-parameters are inherited by operations, items, and subresources
        final List<Parameter> operationParameters = unmodifiableList(
                mergeParameters(new ArrayList<>(parameters), resource.getParameters()));

        // create Swagger operations from CREST operations
        buildCreate(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildRead(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildUpdate(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildDelete(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildPatch(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);
        buildActions(resource, pathName, pathNamespace, tag, resourceVersion,
                operationParameters, pathMap);
        buildQueries(resource, pathName, pathNamespace, tag, resourceVersion, resourceSchema,
                operationParameters, pathMap);

        // create collection-items and sub-resources
        buildItems(resource, pathName, tag, resourceVersion, parameters, pathMap);
        buildSubResources(resource.getSubresources(), pathName, resourceVersion, parameters, pathMap);
    }

    /**
     * Builds {@link Resource} collection-items.
     *
     * @param resource CREST resource
     * @param pathName Resource path-name
     * @param parentTag Tag for grouping operations together by resource/version or {@code null} if there is no parent
     * @param resourceVersion Resource version-name or empty-string
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildItems(final Resource resource, final String pathName, final LocalizableString parentTag,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getItems() != null) {
            final Items items = resource.getItems();

            // an items-resource inherits some fields from its parent, so build combined resource
            final Resource itemsResource = items.asResource(resource.isMvccSupported(),
                    resource.getResourceSchema(), resource.getTitle(), resource.getDescription());

            final Parameter pathParameter = items.getPathParameter();
            final List<Parameter> itemsParameters = unmodifiableList(mergeParameters(mergeParameters(
                    new ArrayList<>(parameters), resource.getParameters()), pathParameter));

            final String itemsPath = buildPath(pathName, "/{" + pathParameter.getName() + "}");
            buildSubResources(items.getSubresources(), itemsPath, resourceVersion, itemsParameters, pathMap);
            buildResourcePaths(itemsResource, itemsPath, parentTag, resourceVersion,
                    itemsParameters, pathMap);
        }
    }

    /**
     * Builds {@link Resource} sub-resources.
     *
     * @param subResources CREST sub-resources
     * @param pathName Resource path-name
     * @param resourceVersion Resource version-name or empty-string
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildSubResources(final SubResources subResources, final String pathName,
            final String resourceVersion, final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (subResources != null) {
            // recursively build sub-resources
            final List<String> subPathNames = new ArrayList<>(subResources.getNames());
            Collections.sort(subPathNames);
            for (final String name : subPathNames) {
                // create path-parameters, for any path-variables found in subPathName
                final List<Parameter> subresourcesParameters = mergeParameters(new ArrayList<>(parameters),
                        buildPathParameters(name));

                final String subPathName = buildPath(pathName, name);
                Resource subResource = resolveResourceReference(subResources.get(name));
                buildResourcePaths(subResource, subPathName, null, resourceVersion,
                        unmodifiableList(subresourcesParameters), pathMap);
            }
        }
    }

    /**
     * Build create-operation.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildCreate(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getCreate() != null) {
            final Create create = resource.getCreate();
            switch (create.getMode()) {
            case ID_FROM_CLIENT:
                final String createPutNamespace = normalizeName(pathNamespace, "create", "put");
                final String createPutPathFragment = normalizeName(resourceVersion, "create", "put");
                final LocalizableOperation putOperation = buildOperation(create, createPutNamespace, resourceSchema,
                        resourceSchema, parameters);
                putOperation.setSummary("Create with Client-Assigned ID");

                if (resource.isMvccSupported()) {
                    putOperation.addParameter(new RefParameter(PARAMETER_IF_NONE_MATCH_ANY_ONLY));
                }

                addOperation(putOperation, "put", pathName, createPutPathFragment, resourceVersion, tag, pathMap);
                break;
            case ID_FROM_SERVER:
                final String createPostNamespace = normalizeName(pathNamespace, "create", "post");
                final String createPostPathFragment = normalizeName(resourceVersion, "create", "post");
                final LocalizableOperation postOperation = buildOperation(create, createPostNamespace, resourceSchema,
                        resourceSchema, parameters);
                postOperation.setSummary("Create with Server-Assigned ID");

                addOperation(postOperation, "post", pathName, createPostPathFragment, resourceVersion, tag,
                        pathMap);
                break;
            default:
                throw new TransformerException("Unsupported CreateMode: " + create.getMode());
            }
        }
    }

    /**
     * Build read-operation.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildRead(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getRead() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "read");
            final String operationPathFragment = normalizeName(resourceVersion, "read");
            final Read read = resource.getRead();

            final LocalizableOperation operation = buildOperation(read, operationNamespace, null, resourceSchema,
                    parameters);
            operation.setSummary("Read");
            operation.addParameter(new RefParameter(PARAMETER_MIME_TYPE));

            if (resource.isMvccSupported()) {
                operation.addParameter(new RefParameter(PARAMETER_IF_NONE_MATCH_REV_ONLY));
            }

            addOperation(operation, "get", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    /**
     * Build update-operation.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildUpdate(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getUpdate() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "update");
            final String operationPathFragment = normalizeName(resourceVersion, "update");
            final Update update = resource.getUpdate();

            final LocalizableOperation operation = buildOperation(update, operationNamespace, resourceSchema,
                    resourceSchema, parameters);
            operation.setSummary("Update");

            if (resource.isMvccSupported()) {
                operation.addParameter(new RefParameter(PARAMETER_IF_MATCH));
            }

            addOperation(operation, "put", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    /**
     * Build delete-operation.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildDelete(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getDelete() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "delete");
            final String operationPathFragment = normalizeName(resourceVersion, "delete");
            final Delete delete = resource.getDelete();

            final LocalizableOperation operation = buildOperation(delete, operationNamespace, null, resourceSchema,
                    parameters);
            operation.setSummary("Delete");

            if (resource.isMvccSupported()) {
                operation.addParameter(new RefParameter(PARAMETER_IF_MATCH));
            }

            addOperation(operation, "delete", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    /**
     * Build patch-operation.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildPatch(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (resource.getPatch() != null) {
            final String operationNamespace = normalizeName(pathNamespace, "patch");
            final String operationPathFragment = normalizeName(resourceVersion, "patch");
            final Patch patch = resource.getPatch();

            final Schema requestSchema = buildPatchRequestPayload(patch.getOperations());
            final LocalizableOperation operation = buildOperation(patch, operationNamespace, requestSchema,
                    resourceSchema, parameters);
            operation.setSummary("Update via Patch Operations");

            if (resource.isMvccSupported()) {
                operation.addParameter(new RefParameter(PARAMETER_IF_MATCH));
            }

            addOperation(operation, "patch", pathName, operationPathFragment, resourceVersion, tag, pathMap);
        }
    }

    /**
     * Build action-operations.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildActions(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final List<Parameter> parameters,
            final Map<String, Path> pathMap) {
        if (!isEmpty(resource.getActions())) {
            final String operationNamespace = normalizeName(pathNamespace, "action");
            final String operationPathFragment = normalizeName(resourceVersion, "action");
            for (final Action action : resource.getActions()) {
                final String actionNamespace = normalizeName(operationNamespace, action.getName());
                final String actionPathFragment = normalizeName(operationPathFragment, action.getName());

                final LocalizableOperation operation = buildOperation(action, actionNamespace, action.getRequest(),
                        action.getResponse(), parameters);
                operation.setSummary("Action: " + action.getName());

                final LocalizableQueryParameter actionParameter = new LocalizableQueryParameter();
                actionParameter.setName("_action");
                actionParameter.setType("string");
                actionParameter.setEnum(asList(action.getName()));
                actionParameter.setRequired(true);
                operation.addParameter(actionParameter);

                addOperation(operation, "post", pathName, actionPathFragment, resourceVersion, tag, pathMap);
            }
        }
    }

    /**
     * Build query-operations.
     *
     * @param resource CREST resource
     * @param pathName Path-name, which is the actual HTTP path
     * @param pathNamespace Unique path-namespace
     * @param tag Tag for grouping operations together by resource/version
     * @param resourceVersion Resource version-name or empty-string
     * @param resourceSchema Resource schema or {@code null}
     * @param parameters CREST operation parameters
     * @param pathMap Output for OpenAPI paths that are constructed
     */
    private void buildQueries(final Resource resource, final String pathName, final String pathNamespace,
            final LocalizableString tag, final String resourceVersion, final Schema resourceSchema,
            final List<Parameter> parameters, final Map<String, Path> pathMap) {
        if (!isEmpty(resource.getQueries())) {
            final String operationNamespace = normalizeName(pathNamespace, "query");
            final String operationPathFragment = normalizeName(resourceVersion, "query");
            for (final Query query : resource.getQueries()) {
                final String queryNamespace;
                final String queryPathFragment;
                final String summary;
                final LocalizableQueryParameter queryParameter;
                switch (query.getType()) {
                case ID:
                    queryNamespace = normalizeName(operationNamespace, "id", query.getQueryId());
                    queryPathFragment = normalizeName(operationPathFragment, "id", query.getQueryId());
                    summary = "Query by ID: " + query.getQueryId();

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryId");
                    queryParameter.setType("string");
                    queryParameter.setEnum(asList(query.getQueryId()));
                    queryParameter.setRequired(true);
                    break;
                case FILTER:
                    queryNamespace = normalizeName(operationNamespace, "filter");
                    queryPathFragment = normalizeName(operationPathFragment, "filter");
                    summary = "Query by Filter";

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryFilter");
                    queryParameter.setType("string");
                    queryParameter.setRequired(true);
                    break;
                case EXPRESSION:
                    queryNamespace = normalizeName(operationNamespace, "expression");
                    queryPathFragment = normalizeName(operationPathFragment, "expression");
                    summary = "Query by Expression";

                    queryParameter = new LocalizableQueryParameter();
                    queryParameter.setName("_queryExpression");
                    queryParameter.setType("string");
                    queryParameter.setRequired(true);
                    break;
                default:
                    throw new TransformerException("Unsupported QueryType: " + query.getType());
                }

                final Schema responsePayload;
                if (resourceSchema.getSchema() != null
                        && !"array".equals(resourceSchema.getSchema().get("type").asString())) {
                    // make query-response schema an array of values
                    responsePayload = Schema.schema().schema(
                            json(object(
                                    field("type", "array"),
                                    field("items", resourceSchema.getSchema())
                            ))).build();
                } else {
                    // already an array or a reference (TODO might not be an array)
                    responsePayload = resourceSchema;
                }

                final LocalizableOperation operation = buildOperation(query, queryNamespace, null, responsePayload,
                        parameters);
                operation.setSummary(summary);
                operation.addParameter(queryParameter);

                final LocalizableQueryParameter pageSizeParamter = new LocalizableQueryParameter();
                pageSizeParamter.setName("_pageSize");
                pageSizeParamter.setType("integer");
                operation.addParameter(pageSizeParamter);

                if (query.getPagingModes() != null) {
                    for (final PagingMode pagingMode : query.getPagingModes()) {
                        final LocalizableQueryParameter parameter = new LocalizableQueryParameter();
                        switch (pagingMode) {
                        case COOKIE:
                            parameter.setName("_pagedResultsCookie");
                            parameter.setType("string");
                            break;
                        case OFFSET:
                            parameter.setName("_pagedResultsOffset");
                            parameter.setType("integer");
                            break;
                        default:
                            throw new TransformerException("Unsupported PagingMode: " + pagingMode);
                        }
                        operation.addParameter(parameter);
                    }
                }

                final LocalizableQueryParameter totalPagedResultsPolicyParameter = new LocalizableQueryParameter();
                totalPagedResultsPolicyParameter.setName("_totalPagedResultsPolicy");
                totalPagedResultsPolicyParameter.setType("string");
                final List<String> totalPagedResultsPolicyValues = new ArrayList<>();
                if (query.getCountPolicies() == null || query.getCountPolicies().length == 0) {
                    totalPagedResultsPolicyValues.add(CountPolicy.NONE.name());
                } else {
                    for (final CountPolicy countPolicy : query.getCountPolicies()) {
                        totalPagedResultsPolicyValues.add(countPolicy.name());
                    }
                }
                totalPagedResultsPolicyParameter._enum(totalPagedResultsPolicyValues);
                operation.addParameter(totalPagedResultsPolicyParameter);

                if (query.getType() != QueryType.ID) {
                    // _sortKeys parameter is not supported for ID queries
                    final LocalizableQueryParameter sortKeysParameter = new LocalizableQueryParameter();
                    sortKeysParameter.setName("_sortKeys");
                    sortKeysParameter.setType("string");
                    if (!isEmpty(query.getSupportedSortKeys())) {
                        sortKeysParameter.setEnum(asList(query.getSupportedSortKeys()));
                    }
                    operation.addParameter(sortKeysParameter);
                }

                addOperation(operation, "get", pathName, queryPathFragment, resourceVersion, tag, pathMap);
            }
        }
    }

    /**
     * Builds a Swagger operation.
     *
     * @param operationModel CREST operation
     * @param operationNamespace Unique operation-namespace
     * @param requestPayload Request payload or {@code null}
     * @param responsePayload Response payload
     * @param parameters CREST operation parameters
     * @return Swagger operation
     */
    private LocalizableOperation buildOperation(final org.forgerock.api.models.Operation operationModel,
            final String operationNamespace, final Schema requestPayload, final Schema responsePayload,
            final List<Parameter> parameters) {
        final LocalizableOperation operation = new LocalizableOperation();
        operation.setOperationId(operationNamespace);
        operation.description(operationModel.getDescription());
        applyOperationStability(operationModel.getStability(), operation);
        applyOperationParameters(mergeParameters(new ArrayList<>(parameters), operationModel.getParameters()),
                operation);
        applyOperationRequestPayload(requestPayload, operation);
        applyOperationResponsePayloads(responsePayload, operationModel.getApiErrors(), operationModel, operation);
        return operation;
    }

    /**
     * Adds an OpenAPI {@code Operation} to a given path, and handles OpenAPI's inability to overload paths/operations
     * by adding a URL-fragment to the path when necessary.
     *
     * @param operation OpenAPI operation
     * @param method HTTP method (e.g., get, post, etc.)
     * @param pathName Path name
     * @param pathFragment Unique path-fragment, for overloading paths
     * @param resourceVersion Resource version-name or empty-string
     * @param tag Tag used to group OpenAPI operations or {@code null}
     * @param pathMap Path map
     */
    private void addOperation(final LocalizableOperation operation, final String method, final String pathName,
            final String pathFragment, final String resourceVersion, final LocalizableString tag,
            final Map<String, Path> pathMap) {
        boolean showPathFragment = false;
        if (!isEmpty(resourceVersion)) {
            showPathFragment = true;
            operation.setVendorExtension("x-resourceVersion", resourceVersion);
            operation.addParameter(new HeaderParameter()
                    .name(AcceptApiVersionHeader.NAME)
                    .type("string")
                    .required(true)
                    ._enum(singletonList(AcceptApiVersionHeader.RESOURCE + "=" + resourceVersion)));
        }
        if (!isEmpty(tag.toString())) {
            operation.addTag(tag);
        }

        Path operationPath = pathMap.get(pathName);
        if (operationPath == null) {
            operationPath = new Path();
        } else if (!showPathFragment) {
            // path already exists, so make sure it is unique
            switch (method) {
            case "get":
                showPathFragment = operationPath.getGet() != null;
                break;
            case "post":
                showPathFragment = operationPath.getPost() != null;
                break;
            case "put":
                showPathFragment = operationPath.getPut() != null;
                break;
            case "delete":
                showPathFragment = operationPath.getDelete() != null;
                break;
            case "patch":
                showPathFragment = operationPath.getPatch() != null;
                break;
            default:
                throw new TransformerException("Unsupported method: " + method);
            }
        }

        if (showPathFragment) {
            // create a unique path by adding a URL-fragment at end
            if (pathName.indexOf('#') != -1) {
                throw new TransformerException("pathName cannot contain # character");
            }
            final String uniquePath = pathName + '#' + pathFragment;
            if (pathMap.containsKey(uniquePath)) {
                throw new TransformerException("pathFragment is not unique for given pathName");
            }
            operationPath = new Path();
            pathMap.put(uniquePath, operationPath);
        } else {
            pathMap.put(pathName, operationPath);
        }

        if (operationPath.set(method, operation) == null) {
            throw new TransformerException("Unsupported method: " + method);
        }
    }

    /**
     * Marks a Swagger operation as <em>deprecated</em> when CREST operation is deprecated or removed.
     *
     * @param stability CREST operation stability or {@code null}
     * @param operation Swagger operation
     */
    private void applyOperationStability(final Stability stability, final Operation operation) {
        if (stability == Stability.DEPRECATED || stability == Stability.REMOVED) {
            operation.setDeprecated(TRUE);
        }
    }

    /**
     * Converts CREST operation parameters (e.g., path variables, query fields) into Swagger operation parameters.
     * <p>
     * This method assumes at {@link org.forgerock.api.enums.ParameterSource#ADDITIONAL} parameters are
     * query-parameters, which would need to be changed with post-processing if, for example, they should be HTTP
     * headers.
     * </p>
     *
     * @param parameters CREST operation parameters
     * @param operation Swagger operation
     */
    private void applyOperationParameters(final List<Parameter> parameters, final Operation operation) {
        if (!parameters.isEmpty()) {
            for (final Parameter parameter : parameters) {
                final LocalizableSerializableParameter operationParameter;
                // NOTE: request-payload BodyParameter is applied elsewhere
                switch (parameter.getSource()) {
                case PATH:
                    operationParameter = new LocalizablePathParameter();
                    break;
                case ADDITIONAL:
                    // we assume that additional parameters are query-parameters, which would need to be changed
                    // with post-processing if, for example, they should be HTTP headers
                    operationParameter = new LocalizableQueryParameter();
                    break;
                default:
                    throw new TransformerException("Unsupported ParameterSource: " + parameter.getSource());
                }
                operationParameter.setName(parameter.getName());
                operationParameter.setType(parameter.getType());
                operationParameter.description(parameter.getDescription());
                operationParameter.setRequired(ValidationUtil.nullToFalse(parameter.isRequired()));
                if (!isEmpty(parameter.getEnumValues())) {
                    operationParameter.setEnum(asList(parameter.getEnumValues()));

                    if (!isEmpty(parameter.getEnumTitles())) {
                        // enum_titles only provided with enum values
                        operationParameter.getVendorExtensions().put("x-enum_titles",
                                asList(parameter.getEnumTitles()));
                    }
                }
                operation.addParameter(operationParameter);
            }
        }

        // apply common parameters
        operation.addParameter(new RefParameter(PARAMETER_FIELDS));
        operation.addParameter(new RefParameter(PARAMETER_PRETTY_PRINT));
    }

    /**
     * Defines a request-payload for a Swagger operation.
     *
     * @param schema JSON Schema or {@code null}
     * @param operation Swagger operation
     */
    private void applyOperationRequestPayload(final Schema schema, final Operation operation) {
        if (schema != null) {
            final Model model;
            if (schema.getSchema() != null) {
                model = buildModel(schema.getSchema());
            } else {
                final String ref = getDefinitionsReference(schema.getReference());
                if (ref == null) {
                    throw new TransformerException("Invalid JSON ref");
                }
                model = new RefModel(ref);
            }
            final LocalizableBodyParameter parameter = new LocalizableBodyParameter();
            parameter.setName("requestPayload");
            parameter.setSchema(model);
            parameter.setRequired(true);
            operation.addParameter(parameter);
        }
    }

    /**
     * Defines response-payloads, which may be a combination of success and error responses, for a Swagger operation.
     *
     * @param schema Success-response JSON schema
     * @param apiErrorResponses ApiError responses
     * @param operationModel CREST operation
     * @param operation Swagger operation
     */
    private void applyOperationResponsePayloads(final Schema schema, final ApiError[] apiErrorResponses,
            final org.forgerock.api.models.Operation operationModel, final Operation operation) {
        final Map<String, Response> responses = new HashMap<>();
        if (schema != null) {
            final Response response = new Response();
            response.description("Success");
            if (schema.getSchema() != null) {
                // https://github.com/swagger-api/swagger-core/issues/1306
                final Model model = buildModel(schema.getSchema());
                final String name = UUID.randomUUID() + "-response";
                definitionMap.put(name, model);
                response.schema(new RefProperty(name));
            } else {
                final String ref = getDefinitionsReference(schema.getReference());
                if (ref == null) {
                    throw new TransformerException("Invalid JSON ref");
                }
                response.schema(new RefProperty(ref));
            }
            if (operationModel instanceof Create) {
                response.addHeader(PARAMETER_LOCATION, locationProperty);
                responses.put("201", response);
            } else {
                responses.put("200", response);
            }
        }

        if (!isEmpty(apiErrorResponses)) {
            // sort by apiError codes, so that same-codes can be merged together, because Swagger cannot overload codes
            // resolve error references before sorting
            final List<ApiError> resolvedErrors = new ArrayList<>(apiErrorResponses.length);
            for (final ApiError error : apiErrorResponses) {
                resolvedErrors.add(resolveErrorReference(error));
            }
            Collections.sort(resolvedErrors, ApiError.ERROR_COMPARATOR);

            final int n = resolvedErrors.size();
            for (int i = 0; i < n; ++i) {
                final ApiError apiError = resolvedErrors.get(i);

                // for a given apiError-code, create a bulleted-list of descriptions, if there is more than one to merge
                final int code = apiError.getCode();
                final List<LocalizableString> descriptions = new ArrayList<>();
                if (apiError.getDescription() != null) {
                    descriptions.add(apiError.getDescription());
                }
                for (int k = i + 1; k < n; ++k) {
                    final ApiError error = resolvedErrors.get(k);
                    if (error.getCode() == code) {
                        // TODO build composite schema with detailsSchema??? error.getSchema();
                        if (error.getDescription() != null) {
                            descriptions.add(error.getDescription());
                        }
                        ++i;
                    }
                }

                Object errorCause = null;
                if (apiError.getSchema() != null && apiError.getSchema().getSchema() != null) {
                    // TODO support detailsSchema reference
                    errorCause = apiError.getSchema().getSchema();
                }

                final JsonValue errorJsonSchema = json(object(
                        field("type", "object"),
                        field("required", array("code", "message")),
                        field("properties", object(
                                field("code", object(
                                        field("type", "integer"),
                                        field("title", "Code"),
                                        field("description", "3-digit apiError code,"
                                                + " corresponding to HTTP status codes.")
                                )),
                                field("message", object(
                                        field("type", "string"),
                                        field("title", "Message"),
                                        field("description", "ApiError message.")
                                )),
                                field("reason", object(
                                        field("type", "string"),
                                        field("title", "Reason"),
                                        field("description", "Short description corresponding to apiError code.")
                                )),
                                field("detail", object(
                                        field("type", "string"),
                                        field("title", "Detail"),
                                        field("description", "Detailed apiError message.")
                                )),
                                fieldIfNotNull("cause", errorCause)
                        ))
                ));

                final LocalizableResponse response = new LocalizableResponse();
                if (descriptions.size() == 1) {
                    response.description(descriptions.get(0));
                } else if (!descriptions.isEmpty()) {
                    response.description(new LocalizableString("Aggregated bullet description list") {
                        @Override
                        public String toTranslatedString(PreferredLocales locales) {
                            // Create a bulleted-list using single-asterisk, as supported by GitHub Flavored Markdown
                            final AsciiDoc bulletedList = AsciiDoc.asciiDoc();
                            for (final LocalizableString listItem : descriptions) {
                                bulletedList.unorderedList1(listItem.toTranslatedString(locales));
                            }
                            return bulletedList.toString();
                        }
                    });
                }

                // https://github.com/swagger-api/swagger-core/issues/1306
                final Model model = buildModel(errorJsonSchema);
                final String name = UUID.randomUUID() + "-error";
                definitionMap.put(name, model);
                response.schema(new RefProperty(name));

                responses.put(String.valueOf(code), response);
            }
        }
        operation.setResponses(responses);
    }

    private ApiError resolveErrorReference(ApiError apiError) {
        if (apiError.getReference() != null) {
            apiError = referenceResolver.getError(apiError.getReference());
            if (apiError == null) {
                throw new TransformerException("Error reference not found in global error definitions");
            }
        }
        return apiError;
    }

    /**
     * Builds a request-payload for a patch-operation.
     *
     * @param patchOperations Supported CREST path-operations
     * @return JSON schema for request-payload
     */
    @VisibleForTesting
    Schema buildPatchRequestPayload(final PatchOperation[] patchOperations) {
        // see org.forgerock.json.resource.PatchOperation#PatchOperation
        final List<Object> enumArray = new ArrayList<>(patchOperations.length);
        for (final PatchOperation op : patchOperations) {
            enumArray.add(op.name().toLowerCase(Locale.ROOT));
        }
        final JsonValue schemaValue = json(object(
                field("type", "array"),
                field("items", object(
                        field("type", "object"),
                        field("properties", object(
                                field("operation", object(
                                        field("type", "string"),
                                        field("enum", enumArray),
                                        field("required", true)
                                )),
                                field("field", object(field("type", "string"))),
                                field("from", object(field("type", "string"))),
                                field("value", object(field("type", "string")))
                        ))
                ))
        ));
        return Schema.schema().schema(schemaValue).build();
    }

    /**
     * Builds Swagger info-model, which describes the API (e.g., title, version, description).
     *
     * @param title API title
     * @return Info model
     */
    @VisibleForTesting
    Info buildInfo(final LocalizableString title) {
        return new LocalizableInfo()
            .title(title != null ? title : new LocalizableString(apiDescription.getId()))
            .description(apiDescription.getDescription())
            .version(apiDescription.getVersion());
    }

    /** Converts global CREST schema definitions into glabal Swagger schema definitions. */
    @VisibleForTesting
    void buildDefinitions() {
        final Definitions definitions = apiDescription.getDefinitions();
        if (definitions != null) {
            // named schema definitions
            final Set<String> definitionNames = definitions.getNames();
            for (final String name : definitionNames) {
                final Schema schema = definitions.get(name);
                if (schema.getSchema() != null) {
                    definitionMap.put(name, buildModel(schema.getSchema()));
                }
            }
        }

        if (!definitionMap.isEmpty()) {
            swagger.setDefinitions(definitionMap);
        }
    }

    /**
     * Converts a JSON schema into the appropriate Swagger model (e.g., object, array, string, integer, etc.).
     *
     * @param schema JSON schema
     * @return Swagger schema model
     */
    @VisibleForTesting
    Model buildModel(final JsonValue schema) {
        final String type = schema.get("type").asString();
        if (type == null) {
            if (schema.isDefined("allOf")) {
                return buildAllOfModel(schema);
            } else if (schema.isDefined("$ref")) {
                return buildReferenceModel(schema);
            }
            throw new TransformerException(unsupportedJsonSchema(schema));
        }
        switch (type) {
        case "object":
            return buildObjectModel(schema);
        case "array":
            return buildArrayModel(schema);
        case "null":
            return new ModelImpl().type(type);
        case "boolean":
        case "integer":
        case "number":
        case "string":
            return buildScalarModel(schema, type);
        default:
            throw new TransformerException("Unsupported JSON Schema type '" + type + "' in schema " + schema);
        }
    }

    private Model buildAllOfModel(final JsonValue schema) {
        final List<Model> allOf = schema.get("allOf").as(listOf(model()));
        if (allOf == null || allOf.isEmpty()) {
            throw new TransformerException(unsupportedJsonSchema(schema));
        }
        final LocalizableComposedModel model = new LocalizableComposedModel();
        setTitleAndDescriptionFromSchema(model, schema);
        model.setAllOf(allOf);

        // TODO external-docs URLs

        return model;
    }

    private String unsupportedJsonSchema(final JsonValue schema) {
        return "Unsupported JSON schema: expected 'type', '$ref' or non-empty 'allOf' property in: '" + schema + "'";
    }

    private Model buildReferenceModel(JsonValue schema) {
        final LocalizableRefModel model = new LocalizableRefModel();
        setTitleAndDescriptionFromSchema(model, schema);
        model.setReference(schema.get("$ref").asString());
        model.setProperties(buildProperties(schema));

        // TODO external-docs URLs

        return model;
    }

    private Function<JsonValue, Model, JsonValueException> model() {
        return new Function<JsonValue, Model, JsonValueException>() {
            @Override
            public Model apply(JsonValue value) throws JsonValueException {
                return buildModel(value);
            }
        };
    }

    private Model buildObjectModel(final JsonValue schema) {
        final LocalizableModelImpl model = new LocalizableModelImpl();
        model.type("object");
        model.setDiscriminator(schema.get("discriminator").asString());
        model.setProperties(buildProperties(schema));
        final List<String> required = getArrayOfJsonString("required", schema);
        if (!required.isEmpty()) {
            model.setRequired(required);
        }
        model.setAdditionalProperties(buildProperty(schema.get("additionalProperties")));
        setTitleAndDescriptionFromSchema(model, schema);

        // TODO external-docs URLs

        return model;
    }

    private LocalizableModelImpl buildScalarModel(final JsonValue schema, final String type) {
        final LocalizableModelImpl model = new LocalizableModelImpl();
        model.type(type);
        setTitleAndDescriptionFromSchema(model, schema);
        if (schema.get("default").isNotNull()) {
            model.setDefaultValue(schema.get("default").asString());
        }

        final List<String> enumValues = getArrayOfJsonString("enum", schema);
        if (!enumValues.isEmpty()) {
            model.setEnum(enumValues);

            // enum_titles only provided with enum values
            final JsonValue options = schema.get("options");
            if (options.isNotNull()) {
                final List<String> enumTitles = getArrayOfJsonString("enum_titles", options);
                if (!enumTitles.isEmpty()) {
                    model.setVendorExtension("x-enum_titles", enumTitles);
                }
            }
        }

        if (schema.get("format").isNotNull()) {
            // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#dataTypeFormat
            model.setFormat(schema.get("format").asString());
            if ("full-date".equals(model.getFormat()) && "string".equals(type)) {
                // Swagger normalizes full-date to date format
                model.setFormat("date");
            }
        }

        // TODO external-docs URLs

        return model;
    }

    /**
     * Converts a JSON schema, representing an array-type, into a Swagger array-model.
     *
     * @param schema JSON schema
     * @return Swagger array-schema model
     */
    private Model buildArrayModel(final JsonValue schema) {
        final LocalizableArrayModel model = new LocalizableArrayModel();
        setTitleAndDescriptionFromSchema(model, schema);
        model.setProperties(buildProperties(schema));
        model.setItems(buildProperty(schema.get("items")));

        // TODO external-docs URLs

        return model;
    }

    /**
     * Convert JSON schema-properties into a Swagger named-properties map, where the key is the JSON field and
     * the value is the JSON schema for that field.
     *
     * @param schema JSON schema containing a <em>properties</em> field
     * @return Swagger named-properties map
     */
    @VisibleForTesting
    Map<String, Property> buildProperties(final JsonValue schema) {
        if (schema != null && schema.isNotNull()) {
            final JsonValue properties = schema.get("properties");
            if (properties.isNotNull()) {
                final Map<String, Object> propertiesMap = properties.asMap();
                final Map<String, Property> resultMap = new LinkedHashMap<>(propertiesMap.size() * 2);

                boolean sortByPropertyOrder = false;
                for (final Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                    final Property property;
                    try {
                        property = buildProperty(json(entry.getValue()));
                    } catch (RuntimeException re) {
                        //json schema can be valid but fail on building the properties
                        logger.info("Json schema error: " + entry.getValue() + "\n"
                                + re.getMessage(), re.fillInStackTrace());
                        throw re;
                    }
                    if (!sortByPropertyOrder && property.getVendorExtensions().containsKey("x-propertyOrder")) {
                        sortByPropertyOrder = true;
                    }
                    resultMap.put(entry.getKey(), property);
                }

                if (sortByPropertyOrder && resultMap.size() > 1) {
                    // sort by x-propertyOrder vendor extension
                    final List<Map.Entry<String, Property>> entries = new ArrayList<>(resultMap.entrySet());
                    Collections.sort(entries, new Comparator<Map.Entry<String, Property>>() {
                        @Override
                        public int compare(final Map.Entry<String, Property> o1, final Map.Entry<String, Property> o2) {
                            // null values appear at end after sorting
                            final Integer v1 = (Integer) o1.getValue().getVendorExtensions().get("x-propertyOrder");
                            final Integer v2 = (Integer) o2.getValue().getVendorExtensions().get("x-propertyOrder");
                            if (v1 != null) {
                                if (v2 != null) {
                                    return v1.compareTo(v2);
                                }
                                return -1;
                            }
                            if (v2 != null) {
                                return 1;
                            }
                            return 0;
                        }
                    });

                    final Map<String, Property> sortedMap = new LinkedHashMap<>(propertiesMap.size() * 2);
                    for (final Map.Entry<String, Property> entry : entries) {
                        sortedMap.put(entry.getKey(), entry.getValue());
                    }
                    return sortedMap;
                } else {
                    return resultMap;
                }
            }
        }
        return null;
    }

    /**
     * Builds a Swagger property representing a JSON Schema definition, where custom JSON Schema extensions are
     * added as Swagger vendor-extensions.
     *
     * @param schema JSON Schema
     * @return Swagger property representing the JSON Schema
     */
    @VisibleForTesting
    Property buildProperty(final JsonValue schema) {
        if (schema == null || schema.isNull()) {
            return null;
        }

        if (schema.get("$ref").isNotNull()) {
            final String ref = getDefinitionsReference(schema.get("$ref").asString());
            if (ref == null) {
                throw new TransformerException("Invalid JSON ref: " + schema.get("$ref").asString());
            }
            return new RefProperty(ref);
        }

        // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#dataTypeFormat
        final String format = schema.get("format").asString();
        final LocalizableProperty abstractProperty = toLocalizableProperty(schema, format);
        if (abstractProperty == null) {
            return null;
        }

        if (!isEmpty(format)) {
            abstractProperty.setFormat(format);
        }
        if (!(abstractProperty instanceof LocalizableObjectProperty
                || abstractProperty instanceof LocalizableArrayProperty)
                && schema.get("default").isNotNull()) {
            // object and array are handled in toLocalizableProperty
            abstractProperty.setDefault(schema.get("default").getObject().toString());
        }
        setTitleAndDescriptionFromSchema(abstractProperty, schema);

        final String readPolicy = schema.get("readPolicy").asString();
        if (!isEmpty(readPolicy)) {
            abstractProperty.setVendorExtension("x-readPolicy", readPolicy);
        }
        if (schema.get("returnOnDemand").isNotNull()) {
            abstractProperty.setVendorExtension("x-returnOnDemand", schema.get("returnOnDemand").asBoolean());
        }

        final Boolean readOnly = schema.get("readOnly").asBoolean();
        if (TRUE.equals(readOnly)) {
            abstractProperty.setReadOnly(TRUE);
        } else {
            // write-policy only relevant when NOT read-only
            final String writePolicy = schema.get("writePolicy").asString();
            if (!isEmpty(writePolicy)) {
                abstractProperty.setVendorExtension("x-writePolicy", writePolicy);
                if (schema.get("errorOnWritePolicyFailure").isNotNull()) {
                    abstractProperty.setVendorExtension("x-errorOnWritePolicyFailure",
                            schema.get("errorOnWritePolicyFailure").asBoolean());
                }
            }
        }

        // https://github.com/jdorn/json-editor#property-ordering
        final Integer propertyOrder = schema.get("propertyOrder").asInteger();
        if (propertyOrder != null) {
            abstractProperty.setVendorExtension("x-propertyOrder", propertyOrder);
        }

        return abstractProperty;
    }

    private LocalizableProperty toLocalizableProperty(final JsonValue schema, final String format) {
        final String type = schema.get("type").asString();
        switch (type) {
        case "object": {
            final LocalizableObjectProperty property = new LocalizableObjectProperty();
            property.setProperties(buildProperties(schema));
            property.setRequiredProperties(getArrayOfJsonString("required", schema));
            if (schema.get("default").isNotNull()) {
                property.setDefault(schema.get("default").getObject());
            }
            return property;
        }
        case "array": {
            final LocalizableArrayProperty property = new LocalizableArrayProperty();
            property.setItems(buildProperty(schema.get("items")));
            property.setMinItems(schema.get("minItems").asInteger());
            property.setMaxItems(schema.get("maxItems").asInteger());
            property.setUniqueItems(schema.get("uniqueItems").asBoolean());
            if (schema.get("default").isNotNull()) {
                property.setDefault(schema.get("default").asList());
            }
            return property;
        }
        case "boolean":
            return new LocalizableBooleanProperty();
        case "integer": {
            final AbstractNumericProperty property;
            if ("int64".equals(format)) {
                property = new LocalizableLongProperty();
            } else {
                property = new LocalizableIntegerProperty();
            }
            property.setMinimum(schema.get("minimum").asBigDecimal());
            property.setMaximum(schema.get("maximum").asBigDecimal());
            property.setExclusiveMinimum(schema.get("exclusiveMinimum").asBoolean());
            property.setExclusiveMaximum(schema.get("exclusiveMaximum").asBoolean());
            return (LocalizableProperty) property;
        }
        case "number": {
            final AbstractNumericProperty property;
            if (isEmpty(format)) {
                // ambiguous
                property = new LocalizableDoubleProperty();
            } else {
                switch (format) {
                case "int32":
                    property = new LocalizableIntegerProperty();
                    break;
                case "int64":
                    property = new LocalizableLongProperty();
                    break;
                case "float":
                    property = new LocalizableFloatProperty();
                    break;
                case "double":
                default:
                    property = new LocalizableDoubleProperty();
                    break;
                }
            }
            property.setMinimum(schema.get("minimum").asBigDecimal());
            property.setMaximum(schema.get("maximum").asBigDecimal());
            property.setExclusiveMinimum(schema.get("exclusiveMinimum").asBoolean());
            property.setExclusiveMaximum(schema.get("exclusiveMaximum").asBoolean());
            return (LocalizableProperty) property;
        }
        case "null":
            return null;
        case "string": {
            if (isEmpty(format)) {
                final LocalizableStringProperty property = new LocalizableStringProperty();
                property.setMinLength(schema.get("minLength").asInteger());
                property.setMaxLength(schema.get("maxLength").asInteger());
                property.setPattern(schema.get("pattern").asString());
                return property;
            }

            switch (format) {
            case "byte":
                return new LocalizableByteArrayProperty();
            case "binary": {
                final LocalizableBinaryProperty property = new LocalizableBinaryProperty();
                property.setMinLength(schema.get("minLength").asInteger());
                property.setMaxLength(schema.get("maxLength").asInteger());
                property.setPattern(schema.get("pattern").asString());
                return property;
            }
            case "date":
            case "full-date":
                return new LocalizableDateProperty();
            case "date-time":
                return new LocalizableDateTimeProperty();
            case "password": {
                final LocalizablePasswordProperty property = new LocalizablePasswordProperty();
                property.setMinLength(schema.get("minLength").asInteger());
                property.setMaxLength(schema.get("maxLength").asInteger());
                property.setPattern(schema.get("pattern").asString());
                return property;
            }
            case "uuid": {
                final LocalizableUUIDProperty property = new LocalizableUUIDProperty();
                property.setMinLength(schema.get("minLength").asInteger());
                property.setMaxLength(schema.get("maxLength").asInteger());
                property.setPattern(schema.get("pattern").asString());
                return property;
            }
            default: {
                final LocalizableStringProperty property = new LocalizableStringProperty();
                property.setMinLength(schema.get("minLength").asInteger());
                property.setMaxLength(schema.get("maxLength").asInteger());
                property.setPattern(schema.get("pattern").asString());
                return property;
            }
            }
        }
        default:
            throw new TransformerException("Unsupported JSON schema type: " + type);
        }
    }

    /**
     * Reads an array of JSON strings, given a field name.
     *
     * @param field Field name
     * @param schema Schema
     * @return result or empty-list, if field is undefined or value is {@code null}
     */
    private List<String> getArrayOfJsonString(final String field, final JsonValue schema) {
        final JsonValue value = schema.get(field);
        if (value.isNotNull() && value.isCollection()) {
            return value.asList(String.class);
        }
        return Collections.emptyList();
    }

    /**
     * Locates a JSON reference segment from an API Descriptor JSON reference, and strips everything before the
     * name of the reference under <em>definitions</em>.
     *
     * @param reference API Descriptor JSON reference
     * @return JSON reference segment or {@code null}
     */
    @VisibleForTesting
    String getDefinitionsReference(final Reference reference) {
        if (reference != null) {
            return getDefinitionsReference(reference.getValue());
        }
        return null;
    }

    /**
     * Locates a JSON reference segment from an API Descriptor JSON reference, and strips everything before the
     * name of the reference under <em>definitions</em>.
     *
     * @param reference API Descriptor JSON reference-value
     * @return JSON reference segment or {@code null}
     */
    @VisibleForTesting
    String getDefinitionsReference(final String reference) {
        if (!isEmpty(reference)) {
            final int start = reference.indexOf(DEFINITIONS_REF);
            if (start != -1) {
                final String s = reference.substring(start + DEFINITIONS_REF.length());
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    private void setTitleAndDescriptionFromSchema(LocalizableTitleAndDescription<?> model, JsonValue schema) {
        setTitleFromJsonValue(model, schema.get("title"));
        setDescriptionFromJsonValue(model, schema.get("description"));
    }

    static void setTitleFromJsonValue(LocalizableTitleAndDescription<?> model, JsonValue source) {
        if (source.isString()) {
            model.title(source.asString());
        } else {
            model.title((LocalizableString) source.getObject());
        }
    }

    static void setDescriptionFromJsonValue(LocalizableTitleAndDescription<?> model, JsonValue source) {
        if (source.isString()) {
            model.description(source.asString());
        } else {
            model.description((LocalizableString) source.getObject());
        }
    }
}
