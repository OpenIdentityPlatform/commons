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

import static org.forgerock.util.Reject.checkNotNull;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * The context associated with a request currently being processed by a JSON
 * request handler within a server. A {@code ServerContext} provides an internal
 * {@code Connection} which can be used for performing internal operations.
 * <p>
 * Contexts <b>MUST</b> support persistence by providing a constructor having
 * the same declaration as {@link #ServerContext(JsonValue, PersistenceConfig)}
 * and by overriding the {@link #saveToJson(JsonValue, PersistenceConfig)}
 * method. See the method's documentation in {@link Context} for more details.
 * <p>
 * Here is an example of the JSON representation of a server context:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : "org.forgerock.json.resource.provider.ServerContext",
 *   "parent" : {
 *       ...
 *   },
 *   "connection-id" : "12345678-3837-464d-b9ec-56f0fb7e464d"
 * }
 * </pre>
 */
public class ServerContext extends Context {
    private static final String ATTR_CLASS = "class";

    // Persisted attribute names.
    private static final String ATTR_CONNECTION_ID = "connection-id";

    /**
     * Creates a new context from the JSON representation of a previously
     * persisted context.
     *
     * @param savedContext
     *            The JSON representation of the persisted context.
     * @param config
     *            The persistence configuration.
     * @return The new context equivalent to the JSON representation of a
     *         previously persisted context.
     * @throws ResourceException
     *             If the JSON representation could not be created.
     * @see ServerContext#ServerContext(JsonValue, PersistenceConfig)
     */
    public static final ServerContext loadFromJson(final JsonValue savedContext,
            final PersistenceConfig config) throws ResourceException {

        // Determine the context implementation class and instantiate it.
        final String className = savedContext.get(ATTR_CLASS).required().asString();
        try {
            final Class<? extends ServerContext> clazz = Class.forName(className, true,
                    config.getClassLoader()).asSubclass(ServerContext.class);
            final Constructor<? extends ServerContext> constructor = clazz.getDeclaredConstructor(
                    JsonValue.class, PersistenceConfig.class);
            return constructor.newInstance(savedContext, config);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate ServerContext implementation class '" + className + "'",
                    e);
        }
    }

    /**
     * Creates a JSON representation of the context which is suitable for
     * persistence to long term storage.
     *
     * @param context
     *            The context to be persisted.
     * @param config
     *            The persistence configuration.
     * @return The JSON representation of the context which is suitable for
     *         persistence to long term storage.
     * @throws ResourceException
     *             If the JSON representation could not be created.
     * @see Context#saveToJson(JsonValue, PersistenceConfig)
     */
    public static final JsonValue saveToJson(final ServerContext context,
            final PersistenceConfig config) throws ResourceException {
        final JsonValue savedContext = new JsonValue(new LinkedHashMap<String, Object>(4));
        context.saveToJson(savedContext, config);
        return savedContext;
    }

    private final Connection connection;

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
        this(parent, null);
    }

    /**
     * Creates a new server context having the provided parent, internal
     * connection, and an ID automatically generated using
     * {@code UUID.randomUUID()}.
     *
     * @param parent
     *            The parent context.
     * @param connection
     *            The connection which should be used for performing internal
     *            operations, which may be {@code null} if the connection should
     *            be inherited from a parent server context.
     * @throws IllegalStateException
     *             If {@code connection} was {@code null} and it was not
     *             possible to inherit a connection from a parent server
     *             context.
     */
    public ServerContext(final Context parent, final Connection connection) {
        super(checkNotNull(parent, "Cannot instantiate ServerContext with null parent Context"));
        this.connection = connection;
        getConnection(); // Fail-fast if there is no connection available.
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
        this(id, parent, null);
    }

    /**
     * Creates a new API information context having the provided ID, parent, and
     * internal connection.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     * @param connection
     *            The connection which should be used for performing internal
     *            operations, which may be {@code null} if the connection should
     *            be inherited from a parent server context.
     * @throws IllegalStateException
     *             If {@code connection} was {@code null} and it was not
     *             possible to inherit a connection from a parent server
     *             context.
     */
    public ServerContext(final String id, final Context parent, final Connection connection) {
        super(id, checkNotNull(parent, "Cannot instantiate ServerContext ith with null parent Context"));
        this.connection = connection;
        getConnection(); // Fail-fast if there is no connection available.
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
    protected ServerContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
        final JsonValue connectionId = savedContext.get(ATTR_CONNECTION_ID);
        if (connectionId.isNull()) {
            this.connection = null;
        } else {
            this.connection = config.getConnectionProvider().getConnection(connectionId.asString());
        }
    }

    /**
     * Returns the connection which should be used for performing internal
     * operations.
     *
     * @return The connection which should be used for performing internal
     *         operations.
     * @throws IllegalStateException
     *             If this server context does not specify an internal
     *             connection and it was not possible to inherit a connection
     *             from a parent server context.
     */
    public Connection getConnection() {
        if (connection != null) {
            return connection;
        } else {
            return getParent().asContext(ServerContext.class).getConnection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveToJson(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super.saveToJson(savedContext, config);
        if (connection != null) {
            savedContext.put(ATTR_CONNECTION_ID, config.getConnectionProvider().getConnectionId(
                    connection));
        }
    }
}
