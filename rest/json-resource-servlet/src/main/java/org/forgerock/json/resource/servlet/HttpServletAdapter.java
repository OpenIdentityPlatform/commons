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

import static org.forgerock.json.resource.servlet.HttpContext.newHttpContext;
import static org.forgerock.json.resource.servlet.HttpUtils.CONTENT_TYPE;
import static org.forgerock.json.resource.servlet.HttpUtils.HEADER_IF_MATCH;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_DELETE;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_GET;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_PATCH;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_POST;
import static org.forgerock.json.resource.servlet.HttpUtils.METHOD_PUT;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_ACTION_ID;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_DEBUG;
import static org.forgerock.json.resource.servlet.HttpUtils.PARAM_PRETTY_PRINT;
import static org.forgerock.json.resource.servlet.HttpUtils.adapt;
import static org.forgerock.json.resource.servlet.HttpUtils.asBooleanValue;
import static org.forgerock.json.resource.servlet.HttpUtils.asIntValue;
import static org.forgerock.json.resource.servlet.HttpUtils.asSingleValue;
import static org.forgerock.json.resource.servlet.HttpUtils.fail;
import static org.forgerock.json.resource.servlet.HttpUtils.getMethod;
import static org.forgerock.json.resource.servlet.HttpUtils.isDebugRequested;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.ConflictException;
import org.forgerock.json.resource.exception.NotSupportedException;
import org.forgerock.json.resource.exception.ResourceException;

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
    private final RequestDispatcher dispatcher;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Context parentContext;
    private final ServletContext servletContext;

    /**
     * Creates a new servlet adapter with the provided connection factory and no
     * parent request context.
     *
     * @param servletContext
     *            The servlet context.
     * @param factory
     *            The connection factory.
     * @throws ServletException
     *             If the servlet container does not support Servlet 2.x or
     *             beyond.
     */
    public HttpServletAdapter(final ServletContext servletContext, final ConnectionFactory factory)
            throws ServletException {
        this(servletContext, factory, null);
    }

    /**
     * Creates a new servlet adapter with the provided connection factory and
     * parent request context.
     *
     * @param servletContext
     *            The servlet context.
     * @param factory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context.
     * @throws ServletException
     *             If the servlet container does not support Servlet 2.x or
     *             beyond.
     */
    public HttpServletAdapter(final ServletContext servletContext, final ConnectionFactory factory,
            final Context parentContext) throws ServletException {
        this.servletContext = servletContext;
        this.parentContext = parentContext != null ? parentContext : Context.newRootContext();

        switch (servletContext.getMajorVersion()) {
        case 1:
            // FIXME: i18n.
            throw new ServletException("Unsupported Servlet version "
                    + servletContext.getMajorVersion());
        case 2:
            this.dispatcher = new Servlet2RequestDispatcher(factory, jsonMapper.getJsonFactory());
            break;
        default:
            this.dispatcher = new Servlet3RequestDispatcher(factory, jsonMapper.getJsonFactory());
            break;
        }
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
            fail(resp, new NotSupportedException("Method " + method + " not supported"));
        }
    }

    void doDelete(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            preprocessRequest(req);

            final DecodedPath dpath = decodePath(req.getPathInfo());
            final Map<String, String[]> parameters = req.getParameterMap();
            final DeleteRequest request =
                    Requests.newDeleteRequest(dpath.getComponent(), dpath.getResourceId())
                            .setRevision(getEtag(req));
            for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                final String name = p.getKey();
                final String[] values = p.getValue();
                if (!parseCommonParameter(name, values, request)) {
                    // FIXME: i18n.
                    throw new BadRequestException("Unrecognized delete request parameter \'" + name
                            + "'");
                }
            }
            // Invoke the request.
            final Context context = newRequestContext(req);
            dispatcher.dispatchRequest(context, request, req, resp);
        } catch (final ResourceException e) {
            fail(resp, e);
        }
    }

    void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            preprocessRequest(req);

            final DecodedPath dpath = decodePath(req.getPathInfo());
            final Map<String, String[]> parameters = req.getParameterMap();

            if (dpath.isCollection() && dpath.getResourceId() == null) {
                // Query against collection.
                final QueryRequest request = Requests.newQueryRequest(dpath.getComponent());

                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();

                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equals("_sort-key")) {
                        for (final String s : values) {
                            try {
                                request.addSortKey(SortKey.valueOf(s));
                            } catch (final IllegalArgumentException e) {
                                // FIXME: i18n.
                                throw new BadRequestException("The value \'" + s
                                        + "\' for parameter '" + name
                                        + "' could not be parsed as a valid sort key");
                            }
                        }
                    } else if (name.equals("_query-id")) {
                        request.setQueryId(asSingleValue(name, values));
                    } else if (name.equals("_paged-results-coookie")) {
                        request.setPagedResultsCookie(asSingleValue(name, values));
                    } else if (name.equals("_page-size")) {
                        request.setPageSize(asIntValue(name, values));
                    } else if (name.equals("_query-filter")) {
                        final String s = asSingleValue(name, values);
                        try {
                            request.setQueryFilter(QueryFilter.valueOf(s));
                        } catch (final IllegalArgumentException e) {
                            // FIXME: i18n.
                            throw new BadRequestException("The value \'" + s + "\' for parameter '"
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
                final ReadRequest request = Requests.newReadRequest(dpath.getComponent());
                if (dpath.getResourceId() != null) {
                    request.setResourceId(dpath.getResourceId());
                }
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (!parseCommonParameter(name, values, request)) {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized read request parameter \'"
                                + name + "'");
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            }
        } catch (final ResourceException e) {
            fail(resp, e);
        }
    }

    void doPatch(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            preprocessRequest(req);
            throw new NotSupportedException("Patch operations are not supported");
        } catch (final ResourceException e) {
            fail(resp, e);
        }
    }

    void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            preprocessRequest(req);

            final DecodedPath dpath = decodePath(req.getPathInfo());
            final Map<String, String[]> parameters = req.getParameterMap();

            // A post may be an action or a create, so check first.
            if (parameters.containsKey(PARAM_ACTION_ID)) {
                // Action request.
                final String actionId =
                        asSingleValue(PARAM_ACTION_ID, parameters.get(PARAM_ACTION_ID));
                final JsonValue content = getJsonContentAsMap(req);
                final ActionRequest request =
                        Requests.newActionRequest(dpath.getComponent(), dpath.getResourceId(),
                                actionId).setContent(content);
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (parseCommonParameter(name, values, request)) {
                        continue;
                    } else if (name.equals(PARAM_ACTION_ID)) {
                        // Ignore - already handled.
                    } else {
                        request.setAdditionalActionParameter(name, asSingleValue(name, values));
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            } else {
                // Create request.
                final JsonValue content = getJsonContentAsMap(req);
                final CreateRequest request =
                        Requests.newCreateRequest(dpath.getComponent(), dpath.getResourceId(),
                                content);
                // See if the client provided a resource ID in the content.
                if (request.getResourceId() == null) {
                    request.setResourceId(content.get("_id").asString());
                }
                for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                    final String name = p.getKey();
                    final String[] values = p.getValue();
                    if (!parseCommonParameter(name, values, request)) {
                        // FIXME: i18n.
                        throw new BadRequestException("Unrecognized create request parameter \'"
                                + name + "'");
                    }
                }
                // Invoke the request.
                final Context context = newRequestContext(req);
                dispatcher.dispatchRequest(context, request, req, resp);
            }
        } catch (final ResourceException e) {
            fail(resp, e);
        }
    }

    void doPut(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            preprocessRequest(req);

            // TODO: a put may be a create if there is an if-none-match header.
            final DecodedPath dpath = decodePath(req.getPathInfo());
            final Map<String, String[]> parameters = req.getParameterMap();
            final JsonValue content = getJsonContentAsMap(req);
            final UpdateRequest request =
                    Requests.newUpdateRequest(dpath.getComponent(), dpath.getResourceId(), content)
                            .setRevision(getEtag(req));
            for (final Map.Entry<String, String[]> p : parameters.entrySet()) {
                final String name = p.getKey();
                final String[] values = p.getValue();
                if (!parseCommonParameter(name, values, request)) {
                    // FIXME: i18n.
                    throw new BadRequestException("Unrecognized update request parameter \'" + name
                            + "'");
                }
            }
            // Invoke the request.
            final Context context = newRequestContext(req);
            dispatcher.dispatchRequest(context, request, req, resp);
        } catch (final ResourceException e) {
            fail(resp, e);
        }
    }

    private DecodedPath decodePath(String path) throws ResourceException {
        // TODO: decode path against request handler.

        // Cases (path may be null):
        //
        // /schema  (cn = schema, rid = null) - read  (singleton)
        // /users   (cn = users,  rid = null) - query (collection)
        // /users/1 (cn = users, rid = 1)     - read  (collection)
        //

        // Dumb and buggy implementation for now: assume that all resources
        // are collections.
        if (path.equals("/")) {
            return new DecodedPath("/", null, true);
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        final int i = path.lastIndexOf('/');
        if (i == 0) {
            return new DecodedPath(path, null, true);
        } else {
            return new DecodedPath(path.substring(0, i), path.substring(i + 1), true);
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

    private String getEtag(final HttpServletRequest req) throws ResourceException {
        // Remove quotes.
        final String etag = req.getHeader(HEADER_IF_MATCH);
        if (etag != null && etag.length() >= 2) {
            if (etag.charAt(0) == '"') {
                return etag.substring(1, etag.length() - 1);
            }
        }
        return etag;
    }

    private JsonValue getJsonContent(final HttpServletRequest req) throws ResourceException {
        try {
            final Object content = jsonMapper.readValue(req.getInputStream(), Object.class);
            return new JsonValue(content);
        } catch (final IOException e) {
            throw adapt(e);
        }
    }

    private JsonValue getJsonContentAsMap(final HttpServletRequest req) throws ResourceException {
        final JsonValue result = getJsonContent(req);
        if (!result.isMap()) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON object");
        }
        return result;
    }

    private Context newRequestContext(final HttpServletRequest req) {
        return newHttpContext(parentContext, req);
    }

    private boolean parseCommonParameter(final String name, final String[] values,
            final Request request) throws ResourceException {
        if (name.equals("_field-filter")) {
            for (final String s : values) {
                try {
                    request.addFieldFilter(s);
                } catch (final IllegalArgumentException e) {
                    // FIXME: i18n.
                    throw new BadRequestException("The value \'" + s + "\' for parameter '" + name
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

}
