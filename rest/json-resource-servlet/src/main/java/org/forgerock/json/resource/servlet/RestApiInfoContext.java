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

import java.util.ResourceBundle;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ContextAttribute;

/**
 * A {@link Context} containing information about the REST API exposed by the
 * network endpoint (Servlet, listener). A REST API information {@link Context}
 * will be created for each REST request having the {@link #CONTEXT_TYPE type}
 * "rest-api-info" and the {@link ContextAttribute}s defined in this class.
 * <p>
 * The {@link #REST_API_URI URI} which identifies the REST API exposed by this
 * module is {@code org.forgerock.commons.json-resource-servlet}.
 * <p>
 * Here is an example of a REST API information context as JSON:
 *
 * <pre>
 * {
 *   "type"   : "rest-api",
 *   "id"     : ...,
 *   "parent" : ...,
 *
 *   "rest-api-uri"     : "org.forgerock.commons.json-resource-servlet"
 *   "rest-api-version" : "2.0"
 * }
 * </pre>
 */
public final class RestApiInfoContext {
    /**
     * The {@link ContextAttribute#TYPE TYPE} of this context.
     */
    public static final String CONTEXT_TYPE = "rest-api-info";

    /**
     * The reserved {@code String} valued attribute {@code "rest-api-uri"},
     * containing a URI identifying the REST API exposed by the network
     * endpoint.
     */
    public static final ContextAttribute<String> REST_API_URI = new ContextAttribute<String>(
            "rest-api-uri");

    /**
     * The reserved {@code String} valued attribute {@code "rest-api-version"},
     * containing the version of the REST API exposed by the network endpoint.
     */
    public static final ContextAttribute<String> REST_API_VERSION = new ContextAttribute<String>(
            "rest-api-version");

    private static final String THIS_API_URI;
    private static final String THIS_API_VERSION;
    static {
        final ResourceBundle bundle = ResourceBundle.getBundle(RestApiInfoContext.class.getName());
        THIS_API_URI = bundle.getString("rest-api-uri");
        THIS_API_VERSION = bundle.getString("rest-api-version");
    }

    static Context newRestApiInfoContext(final Context parent) {
        return newRestApiInfoContext(parent, THIS_API_URI, THIS_API_VERSION);
    }

    static Context newRestApiInfoContext(final Context parent, final String apiUri,
            final String apiVersion) {
        final Context context = parent.newSubContext(CONTEXT_TYPE);
        REST_API_URI.set(context, apiUri);
        REST_API_VERSION.set(context, apiVersion);
        return context;
    }

    private RestApiInfoContext() {
        // Prevent instantiation.
    }

}
