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

package org.forgerock.http.context;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;

/**
 * The context associated with a request currently being processed by a {@code Handler}
 * within a server.
 */
//TODO not sure if this is needed?
public class ServerContext extends AbstractContext {

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
        super(checkNotNull(parent, "Cannot instantiate ServerContext with null parent Context"), "server");
    }

    /**
     * Creates a new server context having the provided parent, an ID
     * automatically generated using {@code UUID.randomUUID()}, and an internal
     * connection inherited from a parent server context.
     *
     * @param parent
     *            The parent context.
     * @param name
     *            The client-friendly name of the context.
     * @throws IllegalStateException
     *             If it was not possible to inherit a connection from a parent
     *             server context.
     */
    public ServerContext(final Context parent, final String name) {
        super(checkNotNull(parent, "Cannot instantiate ServerContext with null parent Context"),
                checkNotNull(name, "Cannot instantiate ServerContext with null name"));
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    protected ServerContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }
}
