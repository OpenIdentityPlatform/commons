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

package org.forgerock.json.resource.http;

import static org.forgerock.json.resource.ActionRequest.ACTION_ID_CREATE;
import static org.forgerock.json.resource.VersionConstants.ACCEPT_API_VERSION;
import static org.forgerock.json.resource.http.HttpUtils.*;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AcceptAPIVersion;
import org.forgerock.json.resource.AcceptAPIVersionContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.ResourcePath;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.Version;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

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
final class HttpAdapter implements Handler {

    private static final String FIELDS_DELIMITER = ",";
    private static final String SORT_KEYS_DELIMITER = ",";

    private final ConnectionFactory connectionFactory;
    private final HttpContextFactory contextFactory;

    /**
     * Creates a new HTTP adapter with the provided connection factory and a
     * context factory the {@link SecurityContextFactory}.
     *
     * @param connectionFactory
     *            The connection factory.
     */
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
     */
    public HttpAdapter(ConnectionFactory connectionFactory, final Context parentContext) {
        this(connectionFactory, new HttpContextFactory() {
            @Override
            public Context createContext(Context parent, org.forgerock.http.protocol.Request request) throws ResourceException {
                return parentContext;
            }
        });
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
     */
    public HttpAdapter(ConnectionFactory connectionFactory, HttpContextFactory contextFactory) {
        this.contextFactory = contextFactory != null ? contextFactory : SecurityContextFactory
                .getHttpServletContextFactory();
        this.connectionFactory = checkNotNull(connectionFactory);
    }

    /**
     * Handles the incoming HTTP request and converts it to a CREST request.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return Promise containing a {@code Response} or {@code ResponseException}.
     */
    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, org.forgerock.http.protocol.Request request) {

        // Dispatch the request based on method, taking into account \
        // method override header.
        final String method = getMethod(request);
        if (METHOD_DELETE.equals(method)) {
            return doDelete(context, request);
        } else if (METHOD_GET.equals(method)) {
            return doGet(context, request);
        } else if (METHOD_PATCH.equals(method)) {
            return doPatch(context, request);
        } else if (METHOD_POST.equals(method)) {
            return doPost(context, request);
        } else if (METHOD_PUT.equals(method)) {
            return doPut(context, request);
        } else {
            // TODO: i18n
            return fail(request, new NotSupportedException("Method " + method + " not supported"));
        }
    }

    Promise<Response, NeverThrowsException> doDelete(Context context, org.forgerock.http.protocol.Request req) {
        try {
            // Parse out the required API versions.
            final AcceptAPIVersion acceptVersion = parseAcceptAPIVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);

            final Form parameters = req.getForm();
            final DeleteRequest request =
                    Requests.newDeleteRequest(getResourcePath(context, req)).setRevision(getIfMatch(req));
            for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                final String name = p.getKey();
                final List<String> values = p.getValue();
                if (parseCommonParameter(name, values, request)) {
                    continue;
                } else {
                    request.setAdditionalParameter(name, asSingleValue(name, values));
                }
            }
            return doRequest(context, req, resp, acceptVersion, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doGet(Context context, org.forgerock.http.protocol.Request req) {
        try {
            // Parse out the required API versions.
            final AcceptAPIVersion acceptVersion = parseAcceptAPIVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfMatch(req);

            final Form parameters = req.getForm();
            if (hasParameter(req, PARAM_QUERY_ID) || hasParameter(req, PARAM_QUERY_EXPRESSION)
                    || hasParameter(req, PARAM_QUERY_FILTER)) {
                // Additional pre-validation for queries.
                rejectIfNoneMatch(req);

                // Query against collection.
                final QueryRequest request = Requests.newQueryRequest(getResourcePath(context, req));

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

                return doRequest(context, req, resp, acceptVersion, request);
            } else {
                // Read of instance within collection or singleton.
                final String rev = getIfNoneMatch(req);
                if (ETAG_ANY.equals(rev)) {
                    // FIXME: i18n
                    throw new PreconditionFailedException("If-None-Match * not appropriate for "
                            + getMethod(req) + " requests");
                }

                final ReadRequest request = Requests.newReadRequest(getResourcePath(context, req));
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
                return doRequest(context, req, resp, acceptVersion, request);
            }
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doPatch(Context context, org.forgerock.http.protocol.Request req) {
        try {
            // Parse out the required API versions.
            final AcceptAPIVersion acceptVersion = parseAcceptAPIVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            if (req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
                // FIXME: i18n
                throw new PreconditionFailedException(
                        "Use of If-None-Match not supported for PATCH requests");
            }

            final Form parameters = req.getForm();
            final PatchRequest request =
                    Requests.newPatchRequest(getResourcePath(context, req)).setRevision(getIfMatch(req));
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
            return doRequest(context, req, resp, acceptVersion, request);
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doPost(Context context, org.forgerock.http.protocol.Request req) {
        try {
            // Parse out the required API versions.
            final AcceptAPIVersion acceptVersion = parseAcceptAPIVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);
            rejectIfMatch(req);

            final Form parameters = req.getForm();
            final String action = asSingleValue(PARAM_ACTION, getParameter(req, PARAM_ACTION));
            if (action.equalsIgnoreCase(ACTION_ID_CREATE)) {
                final JsonValue content = getJsonContent(req);
                final CreateRequest request =
                        Requests.newCreateRequest(getResourcePath(context, req), content);
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
                return doRequest(context, req, resp, acceptVersion, request);
            } else {
                // Action request.
                final JsonValue content = getJsonActionContent(req);
                final ActionRequest request =
                        Requests.newActionRequest(getResourcePath(context, req), action).setContent(content);
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
                return doRequest(context, req, resp, acceptVersion, request);
            }
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    Promise<Response, NeverThrowsException> doPut(Context context, org.forgerock.http.protocol.Request req) {
        try {
            // Parse out the required API versions.
            final AcceptAPIVersion acceptVersion = parseAcceptAPIVersion(req);

            // Prepare response.
            Response resp = prepareResponse(req);

            // Validate request.
            preprocessRequest(req);

            if (req.getHeaders().getFirst(HEADER_IF_MATCH) != null
                    && req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
                // FIXME: i18n
                throw new PreconditionFailedException(
                        "Simultaneous use of If-Match and If-None-Match not "
                                + "supported for PUT requests");
            }

            final Form parameters = req.getForm();
            final JsonValue content = getJsonContent(req);

            final String rev = getIfNoneMatch(req);
            if (ETAG_ANY.equals(rev)) {
                // This is a create with a user provided resource ID: split the
                // path into the parent resource name and resource ID.
                final ResourcePath resourcePath = getResourcePath(context, req);
                if (resourcePath.isEmpty()) {
                    // FIXME: i18n.
                    throw new BadRequestException("No new resource ID in HTTP PUT request");
                }

                // We have a pathInfo of the form "{container}/{id}"
                final CreateRequest request =
                        Requests.newCreateRequest(resourcePath.parent(), content).setNewResourceId(
                                resourcePath.leaf());
                for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final List<String> values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else {
                        request.setAdditionalParameter(name, asSingleValue(name, values));
                    }
                }
                return doRequest(context, req, resp, acceptVersion, request);
            } else {
                final UpdateRequest request =
                        Requests.newUpdateRequest(getResourcePath(context, req), content).setRevision(
                                getIfMatch(req));
                for (final Map.Entry<String, List<String>> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final List<String> values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else {
                        request.setAdditionalParameter(name, asSingleValue(name, values));
                    }
                }
                return doRequest(context, req, resp, acceptVersion, request);
            }
        } catch (final Exception e) {
            return fail(req, e);
        }
    }

    private Promise<Response, NeverThrowsException> doRequest(Context context, org.forgerock.http.protocol.Request req,
            Response resp, AcceptAPIVersion acceptVersion, Request request) throws Exception {

        Context ctx = newRequestContext(context, req, acceptVersion);
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

    /**
     * Gets the raw (still url-encoded) resource name from the request. Removes leading and trailing forward slashes.
     */
    private ResourcePath getResourcePath(Context context, org.forgerock.http.protocol.Request req) throws ResourceException {
        try {
            if (context.containsContext(RouterContext.class)) {
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
        for (Context ctx = context; ctx != null; ctx = ctx.getParent()) {
            if (!ctx.containsContext(RouterContext.class)) {
                break;
            } else {
                matched.add(ResourcePath.valueOf(ctx.asContext(RouterContext.class).getMatchedUri()));
            }
        }
        Collections.reverse(matched);
        ResourcePath matchedUri = new ResourcePath();
        for (ResourcePath resourcePath : matched) {
            matchedUri = matchedUri.concat(resourcePath);
        }
        return matchedUri;
    }

    private Context newRequestContext(Context context, org.forgerock.http.protocol.Request req, AcceptAPIVersion acceptVersion)
            throws ResourceException {
        final Context parent = contextFactory.createContext(context, req);
        return new AdviceContext(
                new AcceptAPIVersionContext(
                        new HttpContext(parent, req), PROTOCOL_NAME, acceptVersion), RESTRICTED_HEADER_NAMES);
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
        if (!req.getMethod().equalsIgnoreCase(HttpUtils.METHOD_GET) && contentType != null
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

    /**
     * Attempts to parse the version header and return a corresponding {@link AcceptAPIVersion} representation.
     * Further validates that the specified versions are valid. That being not in the future and no earlier
     * that the current major version.
     *
     * @param req
     *         The HTTP servlet request
     *
     * @return A non-null {@link AcceptAPIVersion} instance
     *
     * @throws BadRequestException
     *         If an invalid version is requested
     */
    private AcceptAPIVersion parseAcceptAPIVersion(org.forgerock.http.protocol.Request req) throws BadRequestException {
        // Extract out the protocol and resource versions.
        final String versionString = req.getHeaders().getFirst(ACCEPT_API_VERSION);

        final AcceptAPIVersion acceptAPIVersion = AcceptAPIVersion
                .newBuilder(versionString)
                .withDefaultProtocolVersion(PROTOCOL_VERSION)
                .expectsProtocolVersion()
                .build();

        final Version protocolVersion = acceptAPIVersion.getProtocolVersion();

        if (protocolVersion.getMajor() != PROTOCOL_VERSION.getMajor()) {
            throw new BadRequestException("Unsupported major version: " + protocolVersion);
        }

        if (protocolVersion.getMinor() > PROTOCOL_VERSION.getMinor()) {
            throw new BadRequestException("Unsupported minor version: " + protocolVersion);
        }

        return acceptAPIVersion;
    }

}
