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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ContextAttribute;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

/**
 * A {@link Context} containing information relating to the originating HTTP
 * Servlet request. An HTTP {@link Context} will be created for each REST
 * request having the {@link #CONTEXT_TYPE type} "http" and the
 * {@link ContextAttribute}s defined in this class.
 */
public final class HttpContext {
    /**
     * The {@link ContextAttribute#TYPE TYPE} of this context.
     */
    public static final String CONTEXT_TYPE = "http";

    /**
     * The reserved {@code Map<String, Object>} valued attribute
     * {@code "headers"}, containing a map of the HTTP headers.
     */
    public static final ContextAttribute<Map<String, Object>> HEADERS =
            new ContextAttribute<Map<String, Object>>("headers");

    /**
     * The reserved {@code String} valued attribute {@code "method"}, containing
     * the effective HTTP method, taking into account presence of the
     * {@code X-HTTP-Method-Override} header.
     */
    public static final ContextAttribute<String> METHOD = new ContextAttribute<String>("method");

    /**
     * The reserved {@code String} valued attribute {@code "path"}, containing
     * the HTTP request path.
     */
    public static final ContextAttribute<String> PATH = new ContextAttribute<String>("path");

    /**
     * The reserved {@code Map<String, Object>} valued attribute {@code "query"}
     * , containing a map of the HTTP request parameters.
     */
    public static final ContextAttribute<Map<String, Object>> QUERY =
            new ContextAttribute<Map<String, Object>>("query");

    /**
     * The reserved {@code Map<String, Object>} valued attribute
     * {@code "security"}, containing a map of security related properties.
     */
    public static final ContextAttribute<Map<String, Object>> SECURITY =
            new ContextAttribute<Map<String, Object>>("security");

    static Context newHttpContext(final Context parent, final HttpServletRequest req) {
        final Context context = parent.newSubContext(CONTEXT_TYPE);

        METHOD.set(context, HttpUtils.getMethod(req));
        PATH.set(context, req.getRequestURL().toString());

        HEADERS.set(context, new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                final Map<String, Object> result = new LinkedHashMap<String, Object>();
                final Enumeration<String> i = req.getHeaderNames();
                while (i.hasMoreElements()) {
                    final String name = i.nextElement();
                    final Enumeration<String> j = req.getHeaders(name);
                    if (!j.hasMoreElements()) {
                        result.put(name, null);
                    } else {
                        final String s = j.nextElement();
                        if (!j.hasMoreElements()) {
                            result.put(name, s);
                        } else {
                            final List<String> values = new LinkedList<String>();
                            values.add(s);
                            while (j.hasMoreElements()) {
                                values.add(j.nextElement());
                            }
                            result.put(name, values);
                        }
                    }
                }
                return result;
            }
        }));

        QUERY.set(context, new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                final Map<String, Object> result = new LinkedHashMap<String, Object>();
                final Set<Map.Entry<String, String[]>> parameters =
                        req.getParameterMap().entrySet();
                for (final Map.Entry<String, String[]> parameter : parameters) {
                    final String name = parameter.getKey();
                    final String[] values = parameter.getValue();
                    switch (values.length) {
                    case 0:
                        result.put(name, null);
                        break;
                    case 1:
                        result.put(name, values[0]);
                        break;
                    default:
                        result.put(name, Arrays.asList(values));
                        break;
                    }
                }
                return result;
            }
        }));

        SECURITY.set(context, new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                final Map<String, Object> result = new LinkedHashMap<String, Object>(1);
                // FIXME: is this sufficient?
                result.put("user", req.getRemoteUser());
                return result;
            }
        }));

        return context;
    }

    private HttpContext() {
        // Prevent instantiation.
    }

}
