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
package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.fluent.JsonValue;

/**
 * A {@link AbstractContext} containing information about the REST API exposed by the
 * network end-point (Servlet, listener). A REST API information {@link AbstractContext}
 * will be created for each REST request.
 * <p>
 * The name which identifies the REST API exposed by the
 * {@code json-resource-servlet} module is
 * {@code org.forgerock.commons.json-resource-servlet}.
 * <p>
 * Here is an example of the JSON representation of a REST API information
 * context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.ApiInfoContext",
 *   "parent" : {
 *       ...
 *   },
 *   "apiName"     : "org.forgerock.commons.json-resource-servlet",
 *   "apiVersion" : "2.0"
 * }
 * </pre>
 */
public final class ApiInfoContext extends AbstractContext {

    /** a client-friendly name for this context */
    private static final ContextName CONTEXT_NAME = ContextName.valueOf("apiinfo");

    // Persisted attribute names.
    private static final String ATTR_API_NAME = "apiName";
    private static final String ATTR_API_VERSION = "apiVersion";

    /**
     * Creates a new API information context having the provided parent and an
     * ID automatically generated using {@code UUID.randomUUID()}.
     *
     * @param parent
     *            The parent context.
     * @param apiName
     *            The URI identifying the REST API exposed by the network
     *            end-point.
     * @param apiVersion
     *            The version of the REST API exposed by the network end-point.
     */
    public ApiInfoContext(final Context parent, final String apiName, final String apiVersion) {
        super(checkNotNull(parent, "Cannot instantiate ApiInfoContext with null parent Context"));
        data.put(ATTR_API_NAME, checkNotNull(apiName, "Cannot instantiate ApiInfoContext with null apiName"));
        data.put(ATTR_API_VERSION, checkNotNull(apiVersion, "Cannot instantiate ApiInfoContext with null apiVersion"));
    }

    /**
     * Creates a new API information context having the provided ID, and parent.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     * @param apiName
     *            The URI identifying the REST API exposed by the network
     *            end-point.
     * @param apiVersion
     *            The version of the REST API exposed by the network end-point.
     */
    public ApiInfoContext(final String id, final Context parent, final String apiName, final String apiVersion) {
        super(id, checkNotNull(parent, "Cannot instantiate ApiInfoContext with null parent Context"));
        data.put(ATTR_API_NAME, checkNotNull(apiName, "Cannot instantiate ApiInfoContext with null apiName"));
        data.put(ATTR_API_VERSION, checkNotNull(apiVersion, "Cannot instantiate ApiInfoContext with null apiVersion"));
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    ApiInfoContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
    }

    /**
     * Get this Context's {@link org.forgerock.json.resource.ContextName}.
     *
     * @return this object's ContextName
     */
    public ContextName getContextName() {
        return CONTEXT_NAME;
    }

    /**
     * Returns the URI identifying the REST API exposed by the network
     * end-point.
     *
     * @return The URI identifying the REST API exposed by the network
     *         end-point.
     */
    public String getApiName() {
        return data.get(ATTR_API_NAME).asString();
    }

    /**
     * Returns the version of the REST API exposed by the network end-point.
     *
     * @return The version of the REST API exposed by the network end-point.
     */
    public String getApiVersion() {
        return data.get(ATTR_API_VERSION).asString();
    }
}
