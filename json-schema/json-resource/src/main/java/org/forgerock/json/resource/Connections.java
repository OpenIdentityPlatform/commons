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

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Connections {

    // Internal connection implementation.
    private static final class InternalConnection extends AbstractAsynchronousConnection {
        private volatile boolean isClosed = false;
        private final RequestHandler requestHandler;

        private InternalConnection(final RequestHandler handler) {
            this.requestHandler = handler;
        }

        @Override
        public FutureResult<JsonValue> actionAsync(final Context context,
                final ActionRequest request, final ResultHandler<JsonValue> handler) {
            checkConnectionState();
            final FutureResultHandler<JsonValue> future =
                    new FutureResultHandler<JsonValue>(handler);
            requestHandler.action(context, request, future);
            return future;
        }

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public FutureResult<Resource> createAsync(final Context context,
                final CreateRequest request, final ResultHandler<Resource> handler) {
            checkConnectionState();
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.create(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> deleteAsync(final Context context,
                final DeleteRequest request, final ResultHandler<Resource> handler) {
            checkConnectionState();
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.delete(context, request, future);
            return future;
        }

        @Override
        public boolean isClosed() {
            return isClosed;
        }

        @Override
        public boolean isValid() {
            return !isClosed;
        }

        @Override
        public FutureResult<Resource> patchAsync(final Context context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            checkConnectionState();
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.patch(context, request, future);
            return future;
        }

        @Override
        public FutureResult<QueryResult> queryAsync(final Context context,
                final QueryRequest request, final QueryResultHandler handler) {
            checkConnectionState();
            final FutureQueryResultHandler future = new FutureQueryResultHandler(handler);
            requestHandler.query(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> readAsync(final Context context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            checkConnectionState();
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.read(context, request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> updateAsync(final Context context,
                final UpdateRequest request, final ResultHandler<Resource> handler) {
            checkConnectionState();
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.update(context, request, future);
            return future;
        }

        private void checkConnectionState() {
            if (isClosed()) {
                throw new IllegalStateException("Connection already closed");
            }
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

    /**
     * Creates a new connection to a {@link RequestHandler}.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @return The new internal connection.
     * @throws NullPointerException
     *             If {@code handler} was {@code null}.
     */
    public static Connection newInternalConnection(final RequestHandler handler) {
        return new InternalConnection(handler);
    }

    /**
     * Creates a new connection factory which binds internal client connections
     * to {@link RequestHandler}s.
     *
     * @param handler
     *            The request handler to which client requests should be
     *            forwarded.
     * @return The new internal connection factory.
     * @throws NullPointerException
     *             If {@code handler} was {@code null}.
     */
    public static ConnectionFactory newInternalConnectionFactory(final RequestHandler handler) {
        final Connection connection = newInternalConnection(handler);
        return new InternalConnectionFactory(connection);
    }

    // Prevent instantiation.
    private Connections() {
        // Nothing to do.
    }

}
