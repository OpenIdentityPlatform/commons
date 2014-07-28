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
import org.forgerock.json.resource.descriptor.Version;

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
 *   "apiName"     : "org.forgerock.commons.json-resource-servlet",
 *   "apiVersion" : "2.0"
 * }
 * </pre>
 */
public final class ApiInfoContext extends AbstractContext {

    /** a client-friendly name for this context. */
    private static final String CONTEXT_NAME = "apiInfo";

    // Persisted attribute names.
    private static final String ATTR_API_NAME = "apiName";
    private static final String ATTR_API_VERSION = "apiVersion";
    private static final String ATTR_RESOURCE_VERSION = "resourceVersion";

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
     * @param resourceVersion
     *            The version of the resource version.
     */
    public ApiInfoContext(final Context parent, final String apiName,
                          final String apiVersion, final String resourceVersion) {
        super(checkNotNull(parent, "Cannot instantiate ApiInfoContext with null parent Context"));

        data.put(ATTR_API_NAME, checkNotNull(apiName, "Cannot instantiate ApiInfoContext with null apiName"));

        if (apiVersion != null) {
            data.put(ATTR_API_VERSION, apiVersion);
        }

        if (resourceVersion != null) {
            data.put(ATTR_RESOURCE_VERSION, resourceVersion);
        }
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
    ApiInfoContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
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
     * @return The API version or null if not defined
     */
    public Version getApiVersion() {
        final String apiVersion = data.get(ATTR_API_VERSION).asString();
        return apiVersion == null ? null : Version.valueOf(apiVersion);
    }

    /**
     * @return The resource version or null if not defined
     */
    public Version getResourceVersion() {
        final String resourceVersion = data.get(ATTR_RESOURCE_VERSION).asString();
        return resourceVersion == null ? null : Version.valueOf(resourceVersion);
    }

}
