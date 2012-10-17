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
package org.forgerock.json.resource.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A {@link ServerContext} which is created when a request has been routed. The
 * context includes a map which contains the parsed URI template variables,
 * keyed on the URI template variable name.
 * <p>
 * Here is an example of the JSON representation of a routing context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.provider.RouterContext",
 *   "parent" : {
 *       ...
 *   },
 *   "uri-template-variables" : {
 *       "userId" : "bjensen",
 *       "deviceId" : "0"
 *   }
 * }
 * </pre>
 */
public final class RouterContext extends ServerContext {
    // Persisted attribute names.
    private static final String ATTR_URI_TEMPLATE_VARIABLES = "uri-template-variables";

    private final Map<String, String> uriTemplateVariables;

    /**
     * Creates a new routing context having the provided parent, URI template
     * variables, and an ID automatically generated using
     * {@code UUID.randomUUID()}.
     *
     * @param parent
     *            The parent server context.
     * @param uriTemplateVariables
     *            A {@code Map} containing the parsed URI template variables,
     *            keyed on the URI template variable name.
     */
    public RouterContext(final ServerContext parent, final Map<String, String> uriTemplateVariables) {
        super(checkNotNull(parent));
        this.uriTemplateVariables = Collections.unmodifiableMap(uriTemplateVariables);
    }

    /**
     * Creates a new routing context having the provided ID, parent, and URI
     * template variables.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     * @param uriTemplateVariables
     *            A {@code Map} containing the parsed URI template variables,
     *            keyed on the URI template variable name.
     */
    public RouterContext(final String id, final Context parent,
            final Map<String, String> uriTemplateVariables) {
        super(id, checkNotNull(parent));
        this.uriTemplateVariables = Collections.unmodifiableMap(uriTemplateVariables);
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
    protected RouterContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
        final Map<String, Object> savedMap = savedContext.get(ATTR_URI_TEMPLATE_VARIABLES)
                .required().asMap();
        final Map<String, String> newMap = new LinkedHashMap<String, String>(savedMap.size());
        for (final Map.Entry<String, Object> e : savedMap.entrySet()) {
            newMap.put(e.getKey(), String.valueOf(e.getValue()));
        }
        this.uriTemplateVariables = Collections.unmodifiableMap(newMap);
    }

    /**
     * Returns an unmodifiable {@code Map} containing the parsed URI template
     * variables, keyed on the URI template variable name.
     *
     * @return The unmodifiable {@code Map} containing the parsed URI template
     *         variables, keyed on the URI template variable name.
     */
    public Map<String, String> getUriTemplateVariables() {
        return uriTemplateVariables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveToJson(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super.saveToJson(savedContext, config);
        savedContext.put(ATTR_URI_TEMPLATE_VARIABLES, uriTemplateVariables);
    }
}
