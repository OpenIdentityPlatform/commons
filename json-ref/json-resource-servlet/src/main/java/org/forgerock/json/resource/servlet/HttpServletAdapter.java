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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.servlet.HttpUtils.CHARACTER_ENCODING;
import static org.forgerock.json.resource.servlet.HttpUtils.CONTENT_TYPE;
import static org.forgerock.json.resource.servlet.HttpUtils.ETAG_ANY;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_IF_MATCH;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_IF_NONE_MATCH;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_DELETE;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_GET;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_PATCH;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_POST;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_PUT;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_ACTION;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_DEBUG;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_PRETTY_PRINT;
import static org.forgerock.json.resource.servlet.HttpUtils.asBooleanValue;
import static org.forgerock.json.resource.servlet.HttpUtils.asIntValue;
import static org.forgerock.json.resource.servlet.HttpUtils.asSingleValue;
import static org.forgerock.json.resource.servlet.HttpUtils.checkNotNull;
import static org.forgerock.json.resource.servlet.HttpUtils.fail;
import static org.forgerock.json.resource.servlet.HttpUtils.getJsonContent;
import static org.forgerock.json.resource.servlet.HttpUtils.getMethod;
import static org.forgerock.json.resource.servlet.HttpUtils.isDebugRequested;
import static org.forgerock.json.resource.servlet.ServletConfigurator.getServletConfigurator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ApiInfoContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;

/**
 * HTTP adapter from Servlet calls to JSON resource calls. This class can be
 * used in any Servlet, just create a new instance and override the service()
 * method in your Servlet to delegate all those calls to this class's service()
 * method.
 * <p>
 * For example:
 *
 * <pre>
 * public class TestServlet extends javax.servlet.http.HttpServlet {
 *     private HttpServletAdapter adapter;
 *
 *     public void init() throws ServletException {
 *         super.init();
 *         RequestHandler handler = xxx;
 *         adapter = new HttpServletAdapter(getServletContext(), handler);
 *     }
 *
 *     protected void service(HttpServletRequest req, HttpServletResponse res)
 *             throws ServletException, IOException {
 *         adapter.service(req, res);
 *     }
 * }
 * </pre>
 *
 * Note that this adapter does not provide implementations for the HTTP HEAD,
 * OPTIONS, or TRACE methods. A simpler approach is to use the
 * {@link HttpServlet} class contained within this package to build HTTP
 * Servlets since it provides support for these HTTP methods.
 *
 * @see HttpServlet
 */
public final class HttpServletAdapter {
    /**
     * The default context factory which will be used if none was provided
     * during construction or initialization.
     */
    private static final HttpServletContextFactory DEFAULT_CONTEXT_FACTORY = new HttpServletContextFactory() {

        @Override
        public Context createContext(final HttpServletRequest request) throws ResourceException {
            return new RootContext();
        }
    };

    private static final String THIS_API_URI;
    private static final String THIS_API_VERSION;

    static {
        final ResourceBundle bundle = ResourceBundle.getBundle(HttpServletAdapter.class.getName());
        THIS_API_URI = bundle.getString("rest-api-uri");
        THIS_API_VERSION = bundle.getString("rest-api-version");
    }

    private final HttpServletContextFactory contextFactory;
    private final RequestDispatcher dispatcher;
    private final ServletContext servletContext;

    /**
     * Creates a new servlet adapter with the provided connection factory and a
     * context factory which creates a new {@link RootContext} for each request.
     *
     * @param servletContext
     *            The servlet context.
     * @param connectionFactory
     *            The connection factory.
     * @throws ServletException
     *             If the servlet container does not support Servlet 2.x or
     *             beyond.
     */
    public HttpServletAdapter(final ServletContext servletContext,
            final ConnectionFactory connectionFactory) throws ServletException {
        this(servletContext, connectionFactory, (HttpServletContextFactory) null);
    }

    /**
     * Creates a new servlet adapter with the provided connection factory and
     * parent request context.
     *
     * @param servletContext
     *            The servlet context.
     * @param connectionFactory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context.
     * @throws ServletException
     *             If the servlet container does not support Servlet 2.x or
     *             beyond.
     */
    public HttpServletAdapter(final ServletContext servletContext,
            final ConnectionFactory connectionFactory, final Context parentContext)
            throws ServletException {
        this(servletContext, connectionFactory, new HttpServletContextFactory() {

            @Override
            public Context createContext(final HttpServletRequest request) {
                return parentContext;
            }
        });
    }

    /**
     * Creates a new servlet adapter with the provided connection factory and
     * context factory.
     *
     * @param servletContext
     *            The servlet context.
     * @param connectionFactory
     *            The connection factory.
     * @param contextFactory
     *            The context factory which will be used to obtain the parent
     *            context of each request context, or {@code null} if a new
     *            {@link RootContext} should be created for each request.
     * @throws ServletException
     *             If the servlet container does not support Servlet 2.x or
     *             beyond.
     */
    public HttpServletAdapter(final ServletContext servletContext,
            final ConnectionFactory connectionFactory,
            final HttpServletContextFactory contextFactory) throws ServletException {
        this.servletContext = checkNotNull(servletContext);
        this.contextFactory = contextFactory != null ? contextFactory : DEFAULT_CONTEXT_FACTORY;
        this.dispatcher = getServletConfigurator(servletContext).getRequestDispatcher(
                checkNotNull(connectionFactory));
    }

    /**
     * Services the provided HTTP servlet request.
     *
     * @param req
     *            The HTTP servlet request.
     * @param resp
     *            The HTTP servlet response.
     * @throws IOException
     *             If an unexpected IO error occurred while sending the
     *             response.
     */
    public void service(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // Dispatch the request based on method, taking into account \
        // method override header.
        final String method = getMethod(req);
        if (METHOD_DELETE.equals(method)) {
            doDelete(req, resp);
        } else if (METHOD_GET.equals(method)) {
            doGet(req, resp);
        } else if (METHOD_PATCH.equals(method)) {
            doPatch(req, resp);
        } else if (METHOD_POST.equals(method)) {
            doPost(req, resp);
        } else if (METHOD_PUT.equals(method)) {
            doPut(req, resp);
        } else {
            // TODO: i18n
            fail(req, resp, new NotSupportedException("Method " + method + " not supported"));
        }
    }

    void doDelete(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            // Prepare response.
            prepareResponse(resp);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);

            final Map<String, String[]> parameters = req.getParameterMap();
            final DeleteRequest request = Requests.newDeleteRequest(getResourceName(req))
                    .setRevision(getIfMatch(req));
            for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                final String name = p.getKey();
                final String[] values = p.getValue();
                if (!parseCommonParameter(name, values, request)) {
                    // FIXME: i18n.
                    throw new BadRequestException("Unrecognized delete request parameter '" + name
                            + "'");
                }
            }
            // Invoke the request.
            final Context context = newRequestContext(req);
            dispatcher.dispatchRequest(context, request, req, resp);
        } catch (final Exception e) {
            fail(req, resp, e);
        }
    }

    void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            // Prepare response.
            prepareResponse(resp);

            // Validate request.
            preprocessRequest(req);
            rejectIfMatch(req);
            rejectIfNoneMatch(req);

            final Map<String, String[]> parameters = req.getParameterMap();
            if (parameters.containsKey("_queryId") || parameters.containsKey("_queryFilter")) {
                // Query against collection.
                final QueryRequest request = Requests.newQueryRequest(getResourceName(req));

                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();

                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equals("_sortKey")) {
                        for (final String s : values) {
                            try {
                                request.addSortKey(SortKey.valueOf(s));
                            } catch (final IllegalArgumentException e) {
                                // FIXME: i18n.
                                throw new BadRequestException("The value '" + s
                                        + "' for parameter '" + name
                                        + "' could not be parsed as a valid sort key");
                            }
                        }
                    } else if (name.equals("_queryId")) {
                        request.setQueryId(asSingleValue(name, values));
                    } else if (name.equals("_pagedResultsCookie")) {
                        request.setPagedResultsCookie(asSingleValue(name, values));
                    } else if (name.equals("_pageSize")) {
                        request.setPageSize(asIntValue(name, values));
                    } else if (name.equals("_queryFilter")) {
                        final String s = asSingleValue(name, values);
                        try {
                            request.setQueryFilter(QueryFilter.valueOf(s));
                        } catch (final IllegalArgumentException e) {
                            // FIXME: i18n.
                            throw new BadRequestException("The value '" + s + "' for parameter '"
                                    + name + "' could not be parsed as a valid sort key");
                        }
                    } else {
                        request.setAdditionalQueryParameter(name, asSingleValue(name, values));
                    }
                }

                // Check for incompatible arguments.
                if (request.getQueryId() != null && request.getQueryFilter() != null) {
                    // FIXME: i18n.
                    throw new BadRequestException(
                            "The parameters _query_id and _query_filter are mutually exclusive");
                }

                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            } else {
                // Read of instance within collection or singleton.
                final ReadRequest request = Requests.newReadRequest(getResourceName(req));
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (!parseCommonParameter(name, values, request)) {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized read request parameter '"
                                + name + "'");
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            }
        } catch (final Exception e) {
            fail(req, resp, e);
        }
    }

    void doPatch(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);
            throw new NotSupportedException("Patch operations are not supported");
        } catch (final ResourceException e) {
            fail(req, resp, e);
        }
    }

    void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            // Prepare response.
            prepareResponse(resp);

            // Validate request.
            preprocessRequest(req);
            rejectIfNoneMatch(req);
            rejectIfMatch(req);

            final Map<String, String[]> parameters = req.getParameterMap();
            final String action = asSingleValue(PARAM_ACTION, parameters.get(PARAM_ACTION));

            if (action.equals("create")) {
                final JsonValue content = getJsonContent(req);
                final CreateRequest request = Requests.newCreateRequest(getResourceName(req),
                        content);
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equals(PARAM_ACTION)) {
                        // Ignore - already handled.
                    } else {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized create request parameter '"
                                + name + "'");
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            } else {
                // Action request.
                final JsonValue content = getJsonContent(req);
                final ActionRequest request = Requests.newActionRequest(getResourceName(req),
                        action).setContent(content);
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equals(PARAM_ACTION)) {
                        // Ignore - already handled.
                    } else {
                        request.setAdditionalActionParameter(name, asSingleValue(name, values));
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            }
        } catch (final Exception e) {
            fail(req, resp, e);
        }
    }

    void doPut(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            // Prepare response.
            prepareResponse(resp);

            // Validate request.
            preprocessRequest(req);
            if (req.getHeader(HEADER_IF_MATCH) != null
                    && req.getHeader(HEADER_IF_NONE_MATCH) != null) {
                // FIXME: i18n
                throw new PreconditionFailedException(
                        "Simultaneous use of If-Match and If-None-Match not "
                                + "supported for PUT requests");
            }

            final Map<String, String[]> parameters = req.getParameterMap();
            final JsonValue content = getJsonContent(req);

            final String rev = getIfNoneMatch(req);
            if (ETAG_ANY.equals(rev)) {
                // This is a create with a user provided resource ID: split the
                // path into the parent resource name and resource ID.
                final String resourceName = getResourceName(req);
                final int i = resourceName.lastIndexOf('/');
                final CreateRequest request;
                if (i < 0) {
                    // The Servlet specifically states that the path info
                    // contains a forward slash so this is an internal error.
                    // FIXME: i18n.
                    throw new InternalServerErrorException("Invalid HTTP servlet request path info");
                } else if (i == 0) {
                    request = Requests.newCreateRequest("/", content);
                } else {
                    request = Requests.newCreateRequest(resourceName.substring(0, i), content);
                }
                final String newResourceId = resourceName.substring(i + 1);
                if (newResourceId.isEmpty()) {
                    // FIXME: i18n.
                    throw new BadRequestException("No new resource ID in HTTP PUT request");
                }
                request.setNewResourceId(newResourceId);
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (!parseCommonParameter(name, values, request)) {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized create request parameter '"
                                + name + "'");
                    }
                }

                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            } else {
                final UpdateRequest request = Requests.newUpdateRequest(getResourceName(req),
                        content).setRevision(getIfMatch(req));
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (!parseCommonParameter(name, values, request)) {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized update request parameter '"
                                + name + "'");
                    }
                }

                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            }
        } catch (final Exception e) {
            fail(req, resp, e);
        }
    }

    private void dumpRequest(final HttpServletRequest req) {
        final String newline = System.getProperty("line.separator");
        final StringBuilder builder = new StringBuilder();
        builder.append("Method=" + req.getMethod() + newline);
        final Enumeration<?> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = (String) headerNames.nextElement();
            builder.append("Header " + headerName + "=" + req.getHeader(headerName) + newline);
        }
        builder.append("RequestURI=" + req.getRequestURI() + newline);
        builder.append("ContextPath=" + req.getContextPath() + newline);
        builder.append("ServletPath=" + req.getServletPath() + newline);
        builder.append("PathInfo=" + req.getPathInfo() + newline);
        builder.append("QueryString=" + req.getQueryString() + newline);
        final Enumeration<?> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            final String parameterName = (String) parameterNames.nextElement();
            builder.append("Parameter " + parameterName + "="
                    + Arrays.asList(req.getParameterValues(parameterName)) + newline);
        }
        builder.append(newline);
        servletContext.log(builder.toString());
    }

    private String getIfMatch(final HttpServletRequest req) throws ResourceException {
        final String etag = req.getHeader(HEADER_IF_MATCH);
        if (etag != null) {
            if (etag.length() >= 2) {
                // Remove quotes.
                if (etag.charAt(0) == '"') {
                    return etag.substring(1, etag.length() - 1);
                }
            } else if (etag.equals(ETAG_ANY)) {
                // If-Match * is implied anyway.
                return null;
            }
        }
        return etag;
    }

    private String getIfNoneMatch(final HttpServletRequest req) throws ResourceException {
        final String etag = req.getHeader(HEADER_IF_NONE_MATCH);
        if (etag != null) {
            if (etag.length() >= 2) {
                // Remove quotes.
                if (etag.charAt(0) == '"') {
                    return etag.substring(1, etag.length() - 1);
                }
            } else if (etag.equals(ETAG_ANY)) {
                // If-None-Match *.
                return ETAG_ANY;
            }
        }
        return etag;
    }

    private String getResourceName(final HttpServletRequest req) throws ResourceException {
        // Treat null path info as root resource.
        final String resourceName = req.getPathInfo();
        return resourceName == null ? "/" : resourceName;
    }

    private Context newRequestContext(final HttpServletRequest req) throws ResourceException {
        final Context root = contextFactory.createContext(req);
        return new ApiInfoContext(new HttpContext(root, req), THIS_API_URI, THIS_API_VERSION);
    }

    private boolean parseCommonParameter(final String name, final String[] values,
            final Request request) throws ResourceException {
        if (name.equals("_fieldFilter")) {
            for (final String s : values) {
                try {
                    request.addFieldFilter(s);
                } catch (final IllegalArgumentException e) {
                    // FIXME: i18n.
                    throw new BadRequestException("The value '" + s + "' for parameter '" + name
                            + "' could not be parsed as a valid JSON pointer");
                }
            }
            return true;
        } else if (name.equals(PARAM_PRETTY_PRINT)) {
            // This will be handled by the dispatcher, so just validate.
            asBooleanValue(name, values);
            return true;
        } else if (name.equals(PARAM_DEBUG)) {
            // This will be handled by the dispatcher, so just validate.
            asBooleanValue(name, values);
            return true;
        } else {
            // Unrecognized - must be request specific.
            return false;
        }
    }

    private void prepareResponse(final HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);
    }

    private void preprocessRequest(final HttpServletRequest req) throws ResourceException {
        if (isDebugRequested(req)) {
            dumpRequest(req);
        }

        // Perform preliminary request validation.
        if (req.getContentType() != null && !req.getContentType().equals(CONTENT_TYPE)) {
            // TODO: i18n
            throw new BadRequestException(
                    "The request could not be processed because it specified the content-type '"
                            + req.getContentType() + "' when only the content-type '"
                            + CONTENT_TYPE + "' is supported");
        }

        if (req.getHeader("If-Modified-Since") != null) {
            // TODO: i18n
            throw new ConflictException("Header If-Modified-Since not supported");
        }

        if (req.getHeader("If-Unmodified-Since") != null) {
            // TODO: i18n
            throw new ConflictException("Header If-Unmodified-Since not supported");
        }
    }

    private void rejectIfMatch(final HttpServletRequest req) throws ResourceException,
            PreconditionFailedException {
        if (req.getHeader(HEADER_IF_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-Match not supported for " + getMethod(req)
                    + " requests");
        }
    }

    private void rejectIfNoneMatch(final HttpServletRequest req) throws ResourceException,
            PreconditionFailedException {
        if (req.getHeader(HEADER_IF_NONE_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-None-Match not supported for "
                    + getMethod(req) + " requests");
        }
    }

}
