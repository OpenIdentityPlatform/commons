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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.forgerock.json.resource.RoutingMode.EQUALS;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Resources {
    private static final class CollectionHandler implements RequestHandler {
        private final CollectionResourceProvider provider;

        private CollectionHandler(final CollectionResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionCollection(context, request, handler);
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            provider.createInstance(context, request, handler);
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource collection %s cannot be deleted", request.getResourceName()));
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource collection %s cannot be patched", request.getResourceName()));
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            provider.queryCollection(context, request, handler);
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException("The resource collection %s cannot be read",
                    request.getResourceName()));
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource collection %s cannot be updated", request.getResourceName()));
        }
    }

    private static final class CollectionInstance implements RequestHandler {
        private final CollectionResourceProvider provider;

        private CollectionInstance(final CollectionResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionInstance(context, id(context), request, handler);
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource instance %s cannot be created", request.getResourceName()));
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            provider.deleteInstance(context, id(context), request, handler);
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            provider.patchInstance(context, id(context), request, handler);
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource instance %s cannot be queried", request.getResourceName()));
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            provider.readInstance(context, id(context), request, handler);
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            provider.updateInstance(context, id(context), request, handler);
        }

        private String id(final ServerContext context) {
            return context.asContext(RouterContext.class).getUriTemplateVariables().get("id");
        }
    }

    // Internal connection implementation.
    private static final class InternalConnection extends AbstractAsynchronousConnection {
        private final RequestHandler requestHandler;

        private InternalConnection(final RequestHandler handler) {
            this.requestHandler = handler;
        }

        @Override
        public FutureResult<JsonValue> actionAsync(final Context context,
                final ActionRequest request, final ResultHandler<JsonValue> handler) {
            final FutureResultHandler<JsonValue> future =
                    new FutureResultHandler<JsonValue>(handler);
            requestHandler.handleAction(getServerContext(context), request, future);
            return future;
        }

        @Override
        public void close() {
            // Do nothing.
        }

        @Override
        public FutureResult<Resource> createAsync(final Context context,
                final CreateRequest request, final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.handleCreate(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> deleteAsync(final Context context,
                final DeleteRequest request, final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.handleDelete(getServerContext(context), request, future);
            return future;
        }

        @Override
        public boolean isClosed() {
            // Always open.
            return false;
        }

        @Override
        public boolean isValid() {
            // Always valid.
            return true;
        }

        @Override
        public FutureResult<Resource> patchAsync(final Context context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.handlePatch(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<QueryResult> queryAsync(final Context context,
                final QueryRequest request, final QueryResultHandler handler) {
            final FutureQueryResultHandler future = new FutureQueryResultHandler(handler);
            requestHandler.handleQuery(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> readAsync(final Context context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.handleRead(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> updateAsync(final Context context,
                final UpdateRequest request, final ResultHandler<Resource> handler) {
            final FutureResultHandler<Resource> future = new FutureResultHandler<Resource>(handler);
            requestHandler.handleUpdate(getServerContext(context), request, future);
            return future;
        }

        private ServerContext getServerContext(final Context context) {
            if (context instanceof ServerContext) {
                return (ServerContext) context;
            } else {
                final Connection connection;
                if (context.containsContext(ServerContext.class)) {
                    connection = context.asContext(ServerContext.class).getConnection();
                } else {
                    connection = this;
                }
                return new ServerContext(context, connection);
            }
        }

    }

    // Internal connection factory implementation.
    private static final class InternalConnectionFactory implements ConnectionFactory {
        private final RequestHandler handler;

        private InternalConnectionFactory(final RequestHandler handler) {
            this.handler = handler;
        }

        @Override
        public Connection getConnection() {
            return newInternalConnection(handler);
        }

        @Override
        public FutureResult<Connection> getConnectionAsync(final ResultHandler<Connection> handler) {
            final Connection connection = getConnection();
            final FutureResult<Connection> future =
                    new CompletedFutureResult<Connection>(connection);
            if (handler != null) {
                handler.handleResult(connection);
            }
            return future;
        }
    }

    private static final class SingletonHandler implements RequestHandler {
        private final SingletonResourceProvider provider;

        private SingletonHandler(final SingletonResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionInstance(context, request, handler);
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be created", request.getResourceName()));
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be deleted", request.getResourceName()));
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            provider.patchInstance(context, request, handler);
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be queried", request.getResourceName()));
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            provider.readInstance(context, request, handler);
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            provider.updateInstance(context, request, handler);
        }
    }

    /**
     * Returns a JSON object containing only the specified fields from the
     * provided resource. If the list of fields is empty then the resource is
     * returned unchanged.
     * <p>
     * <b>NOTE:</b> this method only performs a shallow copy of extracted
     * fields, so changes to the filtered resource may impact the original
     * resource, and vice-versa.
     *
     * @param resource
     *            The resource whose fields are to be filtered.
     * @param fields
     *            The list of fields to be extracted.
     * @return The filtered resource.
     */
    public static Resource filterResource(final Resource resource,
            final Collection<JsonPointer> fields) {
        final JsonValue unfiltered = resource.getContent();
        final JsonValue filtered = filterResource(unfiltered, fields);
        if (filtered == unfiltered) {
            return resource; // Unchanged.
        } else {
            return new Resource(resource.getId(), resource.getRevision(), filtered);
        }
    }

    /**
     * Returns a JSON object containing only the specified fields from the
     * provided JSON value. If the list of fields is empty then the value is
     * returned unchanged.
     * <p>
     * <b>NOTE:</b> this method only performs a shallow copy of extracted
     * fields, so changes to the filtered JSON value may impact the original
     * JSON value, and vice-versa.
     *
     * @param resource
     *            The JSON value whose fields are to be filtered.
     * @param fields
     *            The list of fields to be extracted.
     * @return The filtered JSON value.
     */
    public static JsonValue filterResource(final JsonValue resource,
            final Collection<JsonPointer> fields) {
        if (fields.isEmpty() || resource.isNull() || resource.size() == 0) {
            return resource;
        } else {
            final Map<String, Object> filtered = new LinkedHashMap<String, Object>(fields.size());
            for (JsonPointer field : fields) {
                if (field.isEmpty()) {
                    // Special case - copy resource fields (assumes Map).
                    filtered.putAll(resource.asMap());
                } else {
                    // FIXME: what should we do if the field refers to an array element?
                    final JsonValue value = resource.get(field);
                    if (value != null) {
                        final String key = field.leaf();
                        filtered.put(key, value.getObject());
                    }
                }
            }
            return new JsonValue(filtered);
        }
    }

    /**
     * Returns a new request handler which will forward requests on to the
     * provided collection resource provider. Incoming requests which are not
     * appropriate for a resource collection or resource instance will result in
     * a bad request error being returned to the client.
     *
     * @param provider
     *            The collection resource provider.
     * @return A new request handler which will forward requests on to the
     *         provided collection resource provider.
     */
    public static RequestHandler newCollection(final CollectionResourceProvider provider) {
        // Route requests to the collection/instance using a router.
        final Router router = new Router();

        // Create a route for the collection.
        final RequestHandler collectionHandler = new CollectionHandler(provider);
        router.addRoute(EQUALS, "/", collectionHandler);

        // Create a route for the instances within the collection.
        final RequestHandler instanceHandler = new CollectionInstance(provider);
        router.addRoute(EQUALS, "/{id}", instanceHandler);

        return router;
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
        return new InternalConnectionFactory(handler);
    }

    /**
     * Returns a new request handler which will forward requests on to the
     * provided singleton resource provider. Incoming requests which are not
     * appropriate for a singleton resource (e.g. query) will result in a bad
     * request error being returned to the client.
     *
     * @param provider
     *            The singleton resource provider.
     * @return A new request handler which will forward requests on to the
     *         provided singleton resource provider.
     */
    public static RequestHandler newSingleton(final SingletonResourceProvider provider) {
        return new SingletonHandler(provider);
    }



    static <T> T checkNotNull(final T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    // Ensure that URI contains a trailing '/' in order to make parsing a
    // matching simpler.
    static String normalizeUri(final String uri) {
        return uri.endsWith("/") ? uri : uri + "/";
    }

    static String removeUriLeadingSlash(final String uri) {
        return (uri.length() > 1 && uri.startsWith("/")) ? uri.substring(1, uri.length()) : uri;
    }

    static String removeUriTrailingSlash(final String uri) {
        return (uri.length() > 1 && uri.endsWith("/")) ? uri.substring(0, uri.length() - 1) : uri;
    }

    private static ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

    // Prevent instantiation.
    private Resources() {
        // Nothing to do.
    }

}
