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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Header;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

/** A {@link Context} containing information relating to the originating HTTP request. */
public final class HttpContext extends AbstractContext {

    // TODO: security parameters such as user name, etc?
    /**
     * Attribute in the serialized JSON form that holds the request headers.
     * @see #HttpContext(JsonValue, ClassLoader)
     */
    public static final String ATTR_HEADERS = "headers";

    /**
     * Attribute in the serialized JSON form that holds the query and/or form parameters.
     * @see #HttpContext(JsonValue, ClassLoader)
     */
    public static final String ATTR_PARAMETERS = "parameters";

    /**
     * Attribute in the serialised JSON form that holds the HTTP method of the request.
     * @see #HttpContext(JsonValue, ClassLoader)
     */
    public static final String ATTR_METHOD = "method";

    /**
     * Attribute in the serialised JSON form that holds the full URI of the request, excluding anything beyond the
     * path component (i.e., no query parameters).
     * @see #HttpContext(JsonValue, ClassLoader)
     */
    public static final String ATTR_PATH = "path";

    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> parameters;

    HttpContext(Context parent, final org.forgerock.http.protocol.Request req) {
        super(parent, "http");
        data.put(ATTR_METHOD, HttpUtils.getMethod(req));
        data.put(ATTR_PATH, getRequestPath(req));
        this.headers = Collections.unmodifiableMap(new LazyMap<>(
            new Factory<Map<String, List<String>>>() {
                @Override
                public Map<String, List<String>> newInstance() {
                    Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (Map.Entry<String, Header> header : req.getHeaders().asMapOfHeaders().entrySet()) {
                        String name = header.getKey();
                        List<String> values = header.getValue().getValues();
                        result.put(name, values);
                    }
                    return result;
                }
            }));
        data.put(ATTR_HEADERS, headers);
        this.parameters = Collections.unmodifiableMap(new LazyMap<>(
            new Factory<Map<String, List<String>>>() {
                @Override
                public Map<String, List<String>> newInstance() {
                    Map<String, List<String>> result = new LinkedHashMap<>();
                    Set<Map.Entry<String, List<String>>> parameters = req.getForm().entrySet();
                    for (Map.Entry<String, List<String>> parameter : parameters) {
                        String name = parameter.getKey();
                        List<String> values = parameter.getValue();
                        result.put(name, values);
                    }
                    return result;
                }
            }));
        data.put(ATTR_PARAMETERS, parameters);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed. Must be a JSON Object that contains {@link #ATTR_HEADERS} and
     *            {@link #ATTR_PARAMETERS} attributes.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public HttpContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
        this.headers = data.get(ATTR_HEADERS).required().asMapOfList(String.class);
        this.parameters = data.get(ATTR_PARAMETERS).required().asMapOfList(String.class);
    }

    private String getRequestPath(org.forgerock.http.protocol.Request req) {
        return new StringBuilder()
            .append(req.getUri().getScheme())
            .append("://")
            .append(req.getUri().getRawAuthority())
            .append(req.getUri().getRawPath()).toString();
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
    public List<String> getHeader(String name) {
        List<String> header = headers.get(name);
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
    public String getHeaderAsString(String name) {
        List<String> header = getHeader(name);
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
    public List<String> getParameter(String name) {
        final List<String> parameter = parameters.get(name);
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
    public String getParameterAsString(String name) {
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
