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
package org.forgerock.json.resource;

import static org.forgerock.json.resource.Resources.checkNotNull;

import org.forgerock.json.fluent.JsonValue;

/**
 * A {@link Context} containing information about the REST API exposed by the
 * network end-point (Servlet, listener). A REST API information {@link Context}
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
 *   "api-name"     : "org.forgerock.commons.json-resource-servlet",
 *   "api-version" : "2.0"
 * }
 * </pre>
 */
public final class ApiInfoContext extends Context {
    // Persisted attribute names.
    private static final String ATTR_API_NAME = "api-name";
    private static final String ATTR_API_VERSION = "api-version";

    // TODO: as this grows it may be better to provide a Builder.
    private final String apiName;
    private final String apiVersion;

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
        super(checkNotNull(parent));
        this.apiName = checkNotNull(apiName);
        this.apiVersion = checkNotNull(apiVersion);
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
    public ApiInfoContext(final String id, final Context parent, final String apiName,
            final String apiVersion) {
        super(id, checkNotNull(parent));
        this.apiName = checkNotNull(apiName);
        this.apiVersion = checkNotNull(apiVersion);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    protected ApiInfoContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
        this.apiName = savedContext.get(ATTR_API_NAME).required().asString();
        this.apiVersion = savedContext.get(ATTR_API_VERSION).required().asString();
    }

    /**
     * Returns the URI identifying the REST API exposed by the network
     * end-point.
     *
     * @return The URI identifying the REST API exposed by the network
     *         end-point.
     */
    public String getApiName() {
        return apiName;
    }

    /**
     * Returns the version of the REST API exposed by the network end-point.
     *
     * @return The version of the REST API exposed by the network end-point.
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveToJson(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super.saveToJson(savedContext, config);
        savedContext.put(ATTR_API_NAME, apiName);
        savedContext.put(ATTR_API_VERSION, apiVersion);
    }
}
