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
 * Copyright 2012-2016 ForgeRock AS.
 */
package org.forgerock.json.resource.http;

import static org.forgerock.api.commons.CommonsApi.*;
import static org.forgerock.json.resource.Applications.simpleCrestApplication;
import static org.forgerock.json.resource.Requests.newApiRequest;
import static org.forgerock.json.resource.http.HttpUtils.CONTENT_TYPE_REGEX;
import static org.forgerock.json.resource.http.HttpUtils.ETAG_ANY;
import static org.forgerock.json.resource.http.HttpUtils.FIELDS_DELIMITER;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_MATCH;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_MODIFIED_SINCE;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_NONE_MATCH;
import static org.forgerock.json.resource.http.HttpUtils.HEADER_IF_UNMODIFIED_SINCE;
import static org.forgerock.json.resource.http.HttpUtils.MIME_TYPE_APPLICATION_JSON;
import static org.forgerock.json.resource.http.HttpUtils.MIME_TYPE_MULTIPART_FORM_DATA;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_ACTION;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_FIELDS;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_MIME_TYPE;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGED_RESULTS_COOKIE;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGED_RESULTS_OFFSET;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PAGE_SIZE;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_PRETTY_PRINT;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_EXPRESSION;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_FILTER;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_ID;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_SORT_KEYS;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_TOTAL_PAGED_RESULTS_POLICY;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.json.resource.http.HttpUtils.RESTRICTED_HEADER_NAMES;
import static org.forgerock.json.resource.http.HttpUtils.SORT_KEYS_DELIMITER;
import static org.forgerock.json.resource.http.HttpUtils.asBooleanValue;
import static org.forgerock.json.resource.http.HttpUtils.asIntValue;
import static org.forgerock.json.resource.http.HttpUtils.asSingleValue;
import static org.forgerock.json.resource.http.HttpUtils.determineRequestType;
import static org.forgerock.json.resource.http.HttpUtils.fail;
import static org.forgerock.json.resource.http.HttpUtils.getIfMatch;
import static org.forgerock.json.resource.http.HttpUtils.getIfNoneMatch;
import static org.forgerock.json.resource.http.HttpUtils.getJsonActionContent;
import static org.forgerock.json.resource.http.HttpUtils.getJsonContent;
import static org.forgerock.json.resource.http.HttpUtils.getJsonPatchContent;
import static org.forgerock.json.resource.http.HttpUtils.getMethod;
import static org.forgerock.json.resource.http.HttpUtils.getParameter;
import static org.forgerock.json.resource.http.HttpUtils.getRequestedResourceVersion;
import static org.forgerock.json.resource.http.HttpUtils.prepareResponse;
import static org.forgerock.json.resource.http.HttpUtils.rejectIfMatch;
import static org.forgerock.json.resource.http.HttpUtils.rejectIfNoneMatch;
import static org.forgerock.json.resource.http.HttpUtils.staticContextFactory;
import static org.forgerock.util.Reject.checkNotNull;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.forgerock.api.CrestApiProducer;
import org.forgerock.api.jackson.PathsModule;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.transform.OpenApiTransformer;
import com.google.common.base.Optional;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.Handler;
import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.http.swagger.SwaggerUtils;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.CrestApplication;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.swagger.models.Swagger;

/**
 * HTTP adapter from HTTP calls to JSON resource calls. This class can be
 * used in any {@link org.forgerock.http.Handler}, just create a new instance and override the handle(Context, Request)
 * method in your HTTP Handler to delegate all those calls to this class's handle(Context, Request)
 * method.
 * <p>
 * For example:
 *
 * <pre>
 * public class TestHandler extends org.forgerock.http.Handler {
 *     private final  HttpAdapter adapter;
 *
 *     public TestHandler() {
 *         RequestHandler handler = xxx;
 *         ConnectionFactory connectionFactory =
 *                 Resources.newInternalConnectionFactory(handler);
 *         adapter = new HttpAdapter(connectionFactory);
 *     }
 *
 *     protected Promise<Response, ResponseException> handler(Context context,
 *                 org.forgerock.http.Request req)
 *             throws ResponseException {
 *         return adapter.handle(context, req);
 *     }
 * }
 * </pre>
 *
 * Note that this adapter does not provide implementations for the HTTP HEAD,
 * OPTIONS, or TRACE methods. A simpler approach is to use the
 * {@link CrestHttp} class contained within this package to build HTTP
 * Handlers since it provides support for these HTTP methods.
 */
final class HttpAdapter implements Handler, Describable<Swagger, org.forgerock.http.protocol.Request>,
        Describable.Listener {

    private static final Logger logger = LoggerFactory.getLogger(HttpAdapter.class);
    private static final ObjectMapper API_OBJECT_MAPPER = new ObjectMapper().registerModules(
            new Json.LocalizableStringModule(),
            new Json.JsonValueModule(),
            new PathsModule());

    private final ConnectionFactory connectionFactory;
    private final HttpContextFactory contextFactory;
    private final String apiId;
    private final String apiVersion;
    private Swagger descriptor;
    private final List<Describable.Listener> apiListeners = new CopyOnWriteArrayList<>();
    private ApiProducer<Swagger> apiProducer;

    /**
     * Creates a new HTTP adapter with the provided connection factory and a
     * context factory the {@link SecurityContextFactory}.
     *
     * @param connectionFactory
     *            The connection factory.
     * @deprecated Use {@link CrestHttp#newHttpHandler(CrestApplication)} instead.
     */
    @Deprecated
    public HttpAdapter(ConnectionFactory connectionFactory) {
        this(connectionFactory, (HttpContextFactory) null);
    }

    /**
     * Creates a new HTTP adapter with the provided connection factory and
     * parent request context.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context.
     * @deprecated Use {@link CrestHttp#newHttpHandler(CrestApplication, Context)} instead.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public HttpAdapter(ConnectionFactory connectionFactory, final Context parentContext) {
        this(connectionFactory, staticContextFactory(parentContext));
    }

    /**
     * Creates a new HTTP adapter with the provided connection factory and
     * context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param contextFactory
     *            The context factory which will be used to obtain the parent
     *            context of each request context, or {@code null} if the
     *            {@link SecurityContextFactory} should be used.
     * @deprecated Use {@link #HttpAdapter(CrestApplication, HttpContextFactory)} instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public HttpAdapter(ConnectionFactory connectionFactory, HttpContextFactory contextFactory) {
        this(simpleCrestApplication(connectionFactory, null, null), contextFactory);
    }

    /**
     * Creates a new HTTP adapter with the provided connection factory and
     * context factory.
     *
     * @param application
     *            The CREST application.
     * @param contextFactory
     *            The context factory which will be used to obtain the parent
     *            context of each request context, or {@code null} if the
     *            {@link SecurityContextFactory} should be used.
     */
    @SuppressWarnings("deprecation")
    public HttpAdapter(CrestApplication application, HttpContextFactory contextFactory) {
        this.contextFactory = contextFactory != null ? contextFactory : SecurityContextFactory
                .getHttpServletContextFactory();
        this.connectionFactory = checkNotNull(application.getConnectionFactory());
        this.apiId = application.getApiId();
        this.apiVersion = application.getApiVersion();

        try {
            Optional<Describable<ApiDescription, Request>> describable = getDescribableConnection();
            if (describable.isPresent()) {
                describable.get().addDescriptorListener(this);
            }
        } catch (ResourceException e) {
            logger.warn("Could not create connection", e);
        }

    }

    /**
     * Handles the incoming HTTP request and converts it to a CREST request.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return Promise containing a {@code Response} or {@code ResponseException}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context,
            org.forgerock.http.protocol.Request request) {
        try {
            RequestType requestType = determineRequestType(request);
            switch (requestType) {
            case CREATE:
                return doCreate(context, request);
            case READ:
                return doRead(context, request);
            case UPDATE:
                return doUpdate(context, request);
            case DELETE:
                return doDelete(context, request);
            case PATCH:
                return doPatch(context, request);
            case ACTION:
                return doAction(context, request);
            case QUERY:
                return doQuery(context, request);
            case API:
                return doApiRequest(context, request);
            default:
                throw new NotSupportedException("Operation " + requestType + " not supported");
            }
        } catch (ResourceException e) {
            return fail(request, e);
        }
    }

    Promise<Response, NeverThrowsException> doDelete(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);

            // use the version-1 meaning of getIfMatch; i.e., treat * as null
            final String ifMatchRevision = getIfMatch(req, PROTOCOL_VERSION_1);
            final Form parameters = req.getForm();
            final DeleteRequest request =
                    Requests.newDeleteRequest(getResourcePath(context, req))
                            .setRevision(ifMatchRevision)
                            .setResourceVersion(requestedResourceVersion);
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doRead(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfMatch(req);

            final Form parameters = req.getForm();
            // Read of instance within collection or singleton.
            final String rev = getIfNoneMatch(req);
            if (ETAG_ANY.equals(rev)) {
                // FIXME: i18n
                throw new PreconditionFailedException("If-None-Match * not appropriate for "
                        + getMethod(req) + " requests");
            }

            final ReadRequest request = Requests.newReadRequest(getResourcePath(context, req))
                    .setResourceVersion(requestedResourceVersion);
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else if (PARAM_MIME_TYPE.equalsIgnoreCase(name)) {
                    if (values.size() != 1 || values.get(0).split(FIELDS_DELIMITER).length > 1) {
                        // FIXME: i18n.
                        throw new BadRequestException("Only one mime type value allowed");
                    }
                    if (parameters.get(PARAM_FIELDS).size() != 1) {
                        // FIXME: i18n.
                        throw new BadRequestException("The mime type parameter requires only "
                                + "1 field to be specified");
                    }
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doQuery(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfMatch(req);

            final Form parameters = req.getForm();
            // Additional pre-validation for queries.
            rejectIfNoneMatch(req);

            // Query against collection.
            final QueryRequest request = Requests.newQueryRequest(getResourcePath(context, req))
                    .setResourceVersion(requestedResourceVersion);

            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();

                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else if (name.equalsIgnoreCase(PARAM_SORT_KEYS)) {
                    for (final String s : values) {
                        try {
                            request.addSortKey(s.split(SORT_KEYS_DELIMITER));
                        } catch (final IllegalArgumentException e) {
                            // FIXME: i18n.
                            throw new BadRequestException("The value '" + s
                                    + "' for parameter '" + name
                                    + "' could not be parsed as a comma "
                                    + "separated list of sort keys");
                        }
                    }
                } else if (name.equalsIgnoreCase(PARAM_QUERY_ID)) {
                    request.setQueryId(asSingleValue(name, values));
                } else if (name.equalsIgnoreCase(PARAM_QUERY_EXPRESSION)) {
                    request.setQueryExpression(asSingleValue(name, values));
                } else if (name.equalsIgnoreCase(PARAM_PAGED_RESULTS_COOKIE)) {
                    request.setPagedResultsCookie(asSingleValue(name, values));
                } else if (name.equalsIgnoreCase(PARAM_PAGED_RESULTS_OFFSET)) {
                    request.setPagedResultsOffset(asIntValue(name, values));
                } else if (name.equalsIgnoreCase(PARAM_PAGE_SIZE)) {
                    request.setPageSize(asIntValue(name, values));
                } else if (name.equalsIgnoreCase(PARAM_QUERY_FILTER)) {
                    final String s = asSingleValue(name, values);
                    try {
                        request.setQueryFilter(QueryFilters.parse(s));
                    } catch (final IllegalArgumentException e) {
                        // FIXME: i18n.
                        throw new BadRequestException("The value '" + s + "' for parameter '"
                                + name + "' could not be parsed as a valid query filter");
                    }
                } else if (name.equalsIgnoreCase(PARAM_TOTAL_PAGED_RESULTS_POLICY)) {
                    final String policy = asSingleValue(name, values);

                    try {
                        request.setTotalPagedResultsPolicy(CountPolicy.valueOf(policy.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        // FIXME: i18n.
                        throw new BadRequestException("The value '" + policy + "' for parameter '"
                                + name + "' could not be parsed as a valid count policy");
                    }
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }

            // Check for incompatible arguments.
            if (request.getQueryId() != null && request.getQueryFilter() != null) {
                // FIXME: i18n.
                throw new BadRequestException("The parameters " + PARAM_QUERY_ID + " and "
                        + PARAM_QUERY_FILTER + " are mutually exclusive");
            }

            if (request.getQueryId() != null && request.getQueryExpression() != null) {
                // FIXME: i18n.
                throw new BadRequestException("The parameters " + PARAM_QUERY_ID + " and "
                        + PARAM_QUERY_EXPRESSION + " are mutually exclusive");
            }

            if (request.getQueryFilter() != null && request.getQueryExpression() != null) {
                // FIXME: i18n.
                throw new BadRequestException("The parameters " + PARAM_QUERY_FILTER + " and "
                        + PARAM_QUERY_EXPRESSION + " are mutually exclusive");
            }

            if (request.getPagedResultsOffset() > 0 && request.getPagedResultsCookie() != null) {
                // FIXME: i18n.
                throw new BadRequestException("The parameters " + PARAM_PAGED_RESULTS_OFFSET + " and "
                        + PARAM_PAGED_RESULTS_COOKIE + " are mutually exclusive");
            }

            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doPatch(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            if (req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
                // FIXME: i18n
                throw new PreconditionFailedException(
                        "Use of If-None-Match not supported for PATCH requests");
            }

            // use the version 1 meaning of getIfMatch; i.e., treat * as null
            final String ifMatchRevision = getIfMatch(req, PROTOCOL_VERSION_1);
            final Form parameters = req.getForm();
            final PatchRequest request =
                    Requests.newPatchRequest(getResourcePath(context, req))
                            .setRevision(ifMatchRevision)
                            .setResourceVersion(requestedResourceVersion);
            request.getPatchOperations().addAll(getJsonPatchContent(req));
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doCreate(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);
            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);

            if ("POST".equals(getMethod(req))) {

                rejectIfNoneMatch(req);
                rejectIfMatch(req);

                final Form parameters = req.getForm();
                final JsonValue content = getJsonContent(req);
                final CreateRequest request =
                        Requests.newCreateRequest(getResourcePath(context, req), content)
                                .setResourceVersion(requestedResourceVersion);
                for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final List<String> values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equalsIgnoreCase(PARAM_ACTION)) {
                        // Ignore - already handled.
                    } else {
                        request.setAdditionalParameter(name, asSingleValue(name, values));
                    }
                }
                return doRequest(context, req, resp, request);
            } else {

                if (req.getHeaders().getFirst(HEADER_IF_MATCH) != null
                        && req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
                    // FIXME: i18n
                    throw new PreconditionFailedException(
                            "Simultaneous use of If-Match and If-None-Match not supported for PUT requests");
                }

                final Form parameters = req.getForm();
                final JsonValue content = getJsonContent(req);

                // This is a create with a user provided resource ID: split the
                // path into the parent resource name and resource ID.
                final ResourcePath resourcePath = getResourcePath(context, req);
                if (resourcePath.isEmpty()) {
                    // FIXME: i18n.
                    throw new BadRequestException("No new resource ID in HTTP PUT request");
                }

                // We have a pathInfo of the form "{container}/{id}"
                final CreateRequest request =
                        Requests.newCreateRequest(resourcePath.parent(), content)
                                .setNewResourceId(resourcePath.leaf())
                                .setResourceVersion(requestedResourceVersion);
                for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final List<String> values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else {
                        request.setAdditionalParameter(name, asSingleValue(name, values));
                    }
                }
                return doRequest(context, req, resp, request);
            }
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doAction(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);
            rejectIfMatch(req);

            final Form parameters = req.getForm();
            final String action = asSingleValue(PARAM_ACTION, getParameter(req, PARAM_ACTION));
            // Action request.
            final JsonValue content = getJsonActionContent(req);
            final ActionRequest request =
                    Requests.newActionRequest(getResourcePath(context, req), action)
                            .setContent(content)
                            .setResourceVersion(requestedResourceVersion);
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else if (name.equalsIgnoreCase(PARAM_ACTION)) {
                    // Ignore - already handled.
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doUpdate(Context context, org.forgerock.http.protocol.Request req) {
        try {
            Version requestedResourceVersion = getRequestedResourceVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);

            if (req.getHeaders().getFirst(HEADER_IF_MATCH) != null
                    && req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
                // FIXME: i18n
                throw new PreconditionFailedException(
                        "Simultaneous use of If-Match and If-None-Match not supported for PUT requests");
            }

            // use the version 1 meaning of getIfMatch; i.e., treat * as null
            final String ifMatchRevision = getIfMatch(req, PROTOCOL_VERSION_1);
            final Form parameters = req.getForm();
            final JsonValue content = getJsonContent(req);

            final UpdateRequest request =
                    Requests.newUpdateRequest(getResourcePath(context, req), content)
                            .setRevision(ifMatchRevision)
                            .setResourceVersion(requestedResourceVersion);
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Promise<Response, NeverThrowsException> doApiRequest(Context context,
            final org.forgerock.http.protocol.Request req) {
        try {
            Optional<Describable<ApiDescription, Request>> describable = getDescribableConnection();
            if (!describable.isPresent()) {
                throw new NotSupportedException();
            }
            Request request = newApiRequest(getResourcePath(context, req));
            context = prepareRequest(context, req, request);
            ApiDescription api = describable.get().handleApiRequest(context, request);

            ObjectWriter writer = Json.makeLocalizingObjectWriter(API_OBJECT_MAPPER, request.getPreferredLocales());

            // Enable pretty printer if requested.
            final List<String> values = getParameter(req, PARAM_PRETTY_PRINT);
            if (values != null) {
                if (asBooleanValue(PARAM_PRETTY_PRINT, values)) {
                    writer = writer.withDefaultPrettyPrinter();
                }
            }

            return newResultPromise(new Response(Status.OK).setEntity(writer.writeValueAsBytes(api)));
        } catch (Exception e) {
            return fail(req, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Describable<ApiDescription, Request>> getDescribableConnection()
            throws ResourceException {
        if (apiId == null || apiVersion == null) {
        	if (logger.isDebugEnabled())
        		logger.debug("CREST API Descriptor API ID and Version are not set. Not describing.");
            return Optional.absent();
        }
        Connection connection = connectionFactory.getConnection();
        if (connection instanceof Describable) {
            return Optional.of((Describable<ApiDescription, Request>) connection);
        } else {
            return Optional.absent();
        }
    }

    private Promise<Response, NeverThrowsException> doRequest(Context context, org.forgerock.http.protocol.Request req,
            Response resp, Request request) throws Exception {
        Context ctx = prepareRequest(context, req, request);
        final RequestRunner runner = new RequestRunner(ctx, request, req, resp);
        return connectionFactory.getConnectionAsync()
                .thenAsync(new AsyncFunction<Connection, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(Connection connection) {
                        return runner.handleResult(connection);
                    }
                }, new AsyncFunction<ResourceException, Response, NeverThrowsException>() {
                    @Override
                    public Promise<Response, NeverThrowsException> apply(ResourceException error) {
                        return runner.handleError(error);
                    }
                });
    }

    private Context prepareRequest(Context context, org.forgerock.http.protocol.Request req, Request request)
            throws ResourceException, org.forgerock.http.header.MalformedHeaderException {
        Context ctx = newRequestContext(context, req);
        final AcceptLanguageHeader acceptLanguageHeader = req.getHeaders().get(AcceptLanguageHeader.class);
        request.setPreferredLocales(acceptLanguageHeader != null
                ? acceptLanguageHeader.getLocales()
                : new PreferredLocales(null));
        return ctx;
    }

    /**
     * Gets the raw (still url-encoded) resource name from the request. Removes leading and trailing forward slashes.
     */
    private ResourcePath getResourcePath(Context context, org.forgerock.http.protocol.Request req)
            throws ResourceException {
        try {
            if (context.containsContext(UriRouterContext.class)) {
                ResourcePath reqPath = ResourcePath.valueOf(req.getUri().getRawPath());
                return reqPath.subSequence(getMatchedUri(context).size(), reqPath.size());
            } else {
                return ResourcePath.valueOf(req.getUri().getRawPath()); //TODO is this a valid assumption?
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private ResourcePath getMatchedUri(Context context) {
        List<ResourcePath> matched = new ArrayList<>();
        Context ctx = context;
        while (ctx.containsContext(UriRouterContext.class)) {
            UriRouterContext uriRouterContext = ctx.asContext(UriRouterContext.class);
            matched.add(ResourcePath.valueOf(uriRouterContext.getMatchedUri()));
            ctx = uriRouterContext.getParent();
        }
        Collections.reverse(matched);
        ResourcePath matchedUri = new ResourcePath();
        for (ResourcePath resourcePath : matched) {
            matchedUri = matchedUri.concat(resourcePath);
        }
        return matchedUri;
    }

    private Context newRequestContext(Context context, org.forgerock.http.protocol.Request req)
            throws ResourceException {
        final Context parent = contextFactory.createContext(context, req);
        return new AdviceContext(new HttpContext(parent, req), RESTRICTED_HEADER_NAMES);
    }

    private boolean parseCommonParameter(final String name, final List<String> values,
            final Request request) throws ResourceException {
        if (name.equalsIgnoreCase(PARAM_FIELDS)) {
            for (final String s : values) {
                try {
                    request.addField(s.split(","));
                } catch (final IllegalArgumentException e) {
                    // FIXME: i18n.
                    throw new BadRequestException("The value '" + s + "' for parameter '" + name
                            + "' could not be parsed as a comma separated list of JSON pointers");
                }
            }
            return true;
        } else if (name.equalsIgnoreCase(PARAM_PRETTY_PRINT)) {
            // This will be handled by the completionHandlerFactory, so just validate.
            asBooleanValue(name, values);
            return true;
        } else {
            // Unrecognized - must be request specific.
            return false;
        }
    }

    private void preprocessRequest(org.forgerock.http.protocol.Request req) throws ResourceException {
        // TODO: check Accept (including charset parameter) and Accept-Charset headers

        // Check content-type.
        final String contentType = ContentTypeHeader.valueOf(req).getType();
        if (!req.getMethod().equalsIgnoreCase(HttpUtils.METHOD_GET)
                && contentType != null
                && !CONTENT_TYPE_REGEX.matcher(contentType).matches()
                && !HttpUtils.isMultiPartRequest(contentType)) {
            // TODO: i18n
            throw new BadRequestException(
                    "The request could not be processed because it specified the content-type '"
                            + contentType + "' when only the content-type '"
                            + MIME_TYPE_APPLICATION_JSON + "' and '"
                            + MIME_TYPE_MULTIPART_FORM_DATA + "' are supported");
        }

        if (req.getHeaders().getFirst(HEADER_IF_MODIFIED_SINCE) != null) {
            // TODO: i18n
            throw new ConflictException("Header If-Modified-Since not supported");
        }

        if (req.getHeaders().getFirst(HEADER_IF_UNMODIFIED_SINCE) != null) {
            // TODO: i18n
            throw new ConflictException("Header If-Unmodified-Since not supported");
        }
    }

    @Override
    public Swagger api(ApiProducer<Swagger> producer) {
        this.apiProducer = producer;
        updateDescriptor();
        return descriptor;
    }

    private void updateDescriptor() {
        if (apiProducer == null) {
            // Not yet attached to CHF
            return;
        }
        try {
            Optional<Describable<ApiDescription, Request>> describable = getDescribableConnection();
            if (describable.isPresent()) {
                ApiDescription api = describable.get().api(new CrestApiProducer(apiId, apiVersion));
                if (api != null) {
                    descriptor = apiProducer.addApiInfo(OpenApiTransformer.execute(api, COMMONS_API_DESCRIPTION));
                }
            }
        } catch (ResourceException e) {
            throw new IllegalStateException("Cannot get connection", e);
        }
    }

    @Override
    public Swagger handleApiRequest(Context context, org.forgerock.http.protocol.Request request) {
        return descriptor != null
                //? SwaggerUtils.clone(descriptor).host(context.asContext(ClientContext.class).getLocalAddress())
        		? SwaggerUtils.clone(descriptor)
                : null;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        apiListeners.add(listener);
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        apiListeners.remove(listener);
    }

    @Override
    public void notifyDescriptorChange() {
        updateDescriptor();
        for (Listener listener : apiListeners) {
            listener.notifyDescriptorChange();
        }
    }
}
