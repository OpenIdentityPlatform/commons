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
 * Copyright 2012-2014 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractContext;
import org.forgerock.json.resource.ClientContext;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

/**
 * A {@link Context} containing information relating to the originating HTTP
 * Servlet request.
 * <p>
 * Here is an example of the JSON representation of an HTTP context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.servlet",
 *   "parent" : {
 *       ...
 *   },
 *   "method"     : "GET",
 *   "path" : "/users/bjensen",
 *   "headers" : {
 *       ...
 *   },
 *   "parameters" : {
 *       ...
 *   }
 * }
 * </pre>
 */
public final class HttpContext extends AbstractContext implements ClientContext {

    /** a client-friendly name for this context. */
    public static final String CONTEXT_NAME = "http";

    // TODO: security parameters such as user name, etc?

    // Persisted attribute names.
    private static final String ATTR_HEADERS = "headers";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_PARAMETERS = "parameters";
    private static final String ATTR_PATH = "path";

    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> parameters;

    /**
     * Restore from JSON representation. This method is for internal use only
     * and should not be called directly.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HttpContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
        this.headers = (Map) data.get(ATTR_HEADERS).required().asMap();
        this.parameters = (Map) data.get(ATTR_PARAMETERS).required().asMap();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExternal() {
        return true;
    }

    HttpContext(final Context parent, final HttpServletRequest req) {
        super(parent);
        data.put(ATTR_METHOD, HttpUtils.getMethod(req));
        data.put(ATTR_PATH, req.getRequestURL().toString());
        this.headers = Collections.unmodifiableMap(new LazyMap<String, List<String>>(
                new Factory<Map<String, List<String>>>() {
                    @Override
                    public Map<String, List<String>> newInstance() {
                        final Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
                        final Enumeration<String> i = req.getHeaderNames();
                        while (i.hasMoreElements()) {
                            final String name = i.nextElement();
                            final Enumeration<String> j = req.getHeaders(name);
                            final List<String> values = new LinkedList<String>();
                            while (j.hasMoreElements()) {
                                values.add(j.nextElement());
                            }
                            result.put(name, values);
                        }
                        return result;
                    }
                }));
        data.put(ATTR_HEADERS, headers);
        this.parameters = Collections.unmodifiableMap(new LazyMap<String, List<String>>(
                new Factory<Map<String, List<String>>>() {
                    @Override
                    public Map<String, List<String>> newInstance() {
                        final Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
                        final Set<Map.Entry<String, String[]>> parameters = req.getParameterMap()
                                .entrySet();
                        for (final Map.Entry<String, String[]> parameter : parameters) {
                            final String name = parameter.getKey();
                            final String[] values = parameter.getValue();
                            result.put(name, Arrays.asList(values));
                        }
                        return result;
                    }
                }));
        data.put(ATTR_PARAMETERS, parameters);
    }

    /**
     * Get this Context's name.
     *
     * @return this object's name
     */
    public String getContextName() {
        return CONTEXT_NAME;
    }

    /**
     * Returns an unmodifiable list containing the values of the named HTTP
     * request header.
     *
     * @param name
     *            The name of the HTTP request header.
     * @return An unmodifiable list containing the values of the named HTTP
     *         request header, which may be empty if the header is not present
     *         in the request.
     */
    public List<String> getHeader(final String name) {
        final List<String> header = data.get(ATTR_HEADERS).get(name).asList(String.class);
        return Collections.unmodifiableList(header != null ? header : Collections.<String> emptyList());
    }

    /**
     * Returns the first value of the named HTTP request header.
     *
     * @param name
     *            The name of the HTTP request header.
     * @return The first value of the named HTTP request header, or {@code null}
     *         if the header is not present in the request.
     */
    public String getHeaderAsString(final String name) {
        final List<String> header = getHeader(name);
        return header.isEmpty() ? null : header.get(0);
    }

    /**
     * Returns an unmodifiable map of the HTTP request headers.
     *
     * @return An unmodifiable map of the HTTP request headers.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns the effective HTTP method, taking into account presence of the
     * {@code X-HTTP-Method-Override} header.
     *
     * @return The effective HTTP method, taking into account presence of the
     *         {@code X-HTTP-Method-Override} header.
     */
    public String getMethod() {
        return data.get(ATTR_METHOD).asString();
    }

    /**
     * Returns an unmodifiable list containing the values of the named HTTP
     * request parameter.
     *
     * @param name
     *            The name of the HTTP request parameter.
     * @return An unmodifiable list containing the values of the named HTTP
     *         request parameter, which may be empty if the parameter is not
     *         present in the request.
     */
    public List<String> getParameter(final String name) {
        final List<String> parameter = data.get(ATTR_PARAMETERS).get(name).asList(String.class);
        return Collections.unmodifiableList(parameter != null ? parameter : Collections.<String> emptyList());
    }

    /**
     * Returns the first value of the named HTTP request parameter.
     *
     * @param name
     *            The name of the HTTP request parameter.
     * @return The first value of the named HTTP request parameter, or
     *         {@code null} if the parameter is not present in the request.
     */
    public String getParameterAsString(final String name) {
        final List<String> parameter = getParameter(name);
        return parameter.isEmpty() ? null : parameter.get(0);
    }

    /**
     * Returns an unmodifiable map of the HTTP request parameters.
     *
     * @return An unmodifiable map of the HTTP request parameters.
     */
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    /**
     * Returns the HTTP request path.
     *
     * @return The HTTP request path.
     */
    public String getPath() {
        return data.get(ATTR_PATH).asString();
    }
}
