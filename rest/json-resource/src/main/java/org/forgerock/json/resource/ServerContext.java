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
 * The context associated with a request currently being processed by a JSON
 * request handler within a server. A {@code ServerContext} provides an internal
 * {@code Connection} which can be used for performing internal operations.
 * <p>
 * Contexts <b>MUST</b> support persistence by providing a constructor having
 * the same declaration as {@link #ServerContext(JsonValue, PersistenceConfig)}.
 * See the method's documentation in {@link AbstractContext} for more details.
 * <p>
 * Here is an example of the JSON representation of a server context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.provider.ServerContext",
 *   "parent" : {
 *       ...
 *   }
 * }
 * </pre>
 */
public class ServerContext extends AbstractContext {

    /** the client-friendly name of this context */
    private static final ContextName CONTEXT_NAME = ContextName.valueOf("server");

    /**
     * Creates a new server context having the provided parent, an ID
     * automatically generated using {@code UUID.randomUUID()}, and an internal
     * connection inherited from a parent server context.
     *
     * @param parent
     *            The parent context.
     * @throws IllegalStateException
     *             If it was not possible to inherit a connection from a parent
     *             server context.
     */
    public ServerContext(final Context parent) {
        super(parent);
    }

    /**
     * Creates a new API information context having the provided ID, parent, and
     * an internal connection inherited from a parent server context.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     * @throws IllegalStateException
     *             If it was not possible to inherit a connection from a parent
     *             server context.
     */
    public ServerContext(final String id, final Context parent) {
        super(id, checkNotNull(parent, "Cannot instantiate ServerContext with null parent Context"));
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
    public ServerContext(final JsonValue savedContext, final PersistenceConfig config)
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
}
