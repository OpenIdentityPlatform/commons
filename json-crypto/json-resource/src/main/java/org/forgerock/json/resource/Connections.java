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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.exception.ResourceException;
import org.forgerock.json.resource.provider.RequestHandler;
import org.forgerock.util.Factory;

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Connections {

    // Internal connection implementation.
    private static final class InternalConnection extends AbstractAsynchronousConnection {
        private final Factory<Context> contextFactory;
        private final RequestHandler requestHandler;

        private InternalConnection(final RequestHandler handler,
                final Factory<Context> contextFactory) {
            this.requestHandler = handler;
            this.contextFactory = contextFactory;
        }

        @Override
        public FutureResult<JsonValue> actionAsync(final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            final FutureResultHandler<JsonValue> future =
                    new FutureResultHandler<JsonValue>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.action(context, request, future);
            return future;
        }

        @Override
        public void close() {
            // No implementation required.
        }

        @Override
        public FutureResult<Resource> createAsync(final CreateRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.create(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> deleteAsync(final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.delete(context, request, future);
            return future;
        }

        @Override
        public boolean isClosed() {
            // Cannot be closed.
            return false;
        }

        @Override
        public boolean isValid() {
            // Always valid.
            return true;
        }

        @Override
        public FutureResult<Resource> patchAsync(final PatchRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.patch(context, request, future);
            return future;
        }

        @Override
        public FutureResult<QueryResult> queryAsync(final QueryRequest request,
                final QueryResultHandler handler) {
            final FutureQueryResultHandler future = new FutureQueryResultHandler(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.query(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> readAsync(final ReadRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.read(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> updateAsync(final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            final Context context = contextFactory.newInstance();
            requestHandler.update(context, request, future);
            return future;
        }

    }

    // Internal connection factory implementation.
    private static final class InternalConnectionFactory implements ConnectionFactory {
        private final Connection connection;
        private final FutureResult<Connection> future;

        private InternalConnectionFactory(final Connection connection) {
            this.connection = connection;
            this.future = new CompletedFutureResult<Connection>(connection);
        }

        @Override
        public Connection getConnection() throws ResourceException {
            return connection;
        }

        @Override
        public FutureResult<Connection> getConnectionAsync(final ResultHandler<Connection> handler) {
            if (handler != null) {
                handler.handleResult(connection);
            }
            return future;
        }
    }

    // Prevent instantiation.
    private Connections() {
        // Nothing to do.
    }

    /**
     * Creates a new connection to a {@link RequestHandler}.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @param context
     *            The request context which should be passed to the request
     *            handler with each request.
     * @return The new internal connection.
     * @throws NullPointerException
     *             If {@code handler} or {@code context} was {@code null}.
     */
    public static Connection newInternalConnection(final RequestHandler handler,
            final Context context) {
        final Factory<Context> contextFactory = new Factory<Context>() {
            @Override
            public Context newInstance() {
                return context;
            }
        };
        return newInternalConnection(handler, contextFactory);
    }

    /**
     * Creates a new connection to a {@link RequestHandler}.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @param contextFactory
     *            A factory which should be used to create a new context for
     *            each request.
     * @return The new internal connection.
     * @throws NullPointerException
     *             If {@code handler} or {@code contextFactory} was {@code null}
     *             .
     */
    public static Connection newInternalConnection(final RequestHandler handler,
            final Factory<Context> contextFactory) {
        return new InternalConnection(handler, contextFactory);
    }

    /**
     * Creates a new connection factory which binds internal client connections
     * to {@link RequestHandler}s.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @param context
     *            The request context which should be passed to the request
     *            handler with each request.
     * @return The new internal connection factory.
     * @throws NullPointerException
     *             If {@code handler} or {@code context} was {@code null}.
     */
    public static ConnectionFactory newInternalConnectionFactory(final RequestHandler handler,
            final Context context) {
        final Connection connection = newInternalConnection(handler, context);
        return new InternalConnectionFactory(connection);
    }

    /**
     * Creates a new connection factory which binds internal client connections
     * to {@link RequestHandler}s.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @param contextFactory
     *            A factory which should be used to create a new context for
     *            each request.
     * @return The new internal connection factory.
     * @throws NullPointerException
     *             If {@code handler} or {@code contextFactory} was {@code null}
     *             .
     */
    public static ConnectionFactory newInternalConnectionFactory(final RequestHandler handler,
            final Factory<Context> contextFactory) {
        final Connection connection = newInternalConnection(handler, contextFactory);
        return new InternalConnectionFactory(connection);
    }

}
