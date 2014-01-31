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

import static org.forgerock.json.resource.RoutingMode.EQUALS;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.Promises;

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Resources {

    /**
     * Implementation class for {@link #asRequestHandler}.
     */
    private static final class SynchronousRequestHandlerAdapter implements RequestHandler {
        private final SynchronousRequestHandler syncHandler;

        private SynchronousRequestHandlerAdapter(final SynchronousRequestHandler syncHandler) {
            this.syncHandler = syncHandler;
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            try {
                handler.handleResult(syncHandler.handleUpdate(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handleRead(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            try {
                handler.handleResult(syncHandler.handleRead(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handleQuery(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            try {
                final Collection<Resource> resources = new LinkedList<Resource>();
                final QueryResult result = syncHandler.handleQuery(context, request, resources);
                for (final Resource resource : resources) {
                    handler.handleResource(resource);
                }
                handler.handleResult(result);
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            try {
                handler.handleResult(syncHandler.handlePatch(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handleDelete(final ServerContext context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            try {
                handler.handleResult(syncHandler.handleDelete(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            try {
                handler.handleResult(syncHandler.handleCreate(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            try {
                handler.handleResult(syncHandler.handleAction(context, request));
            } catch (final ResourceException e) {
                handler.handleError(e);
            }
        }
    }

    private static final class CollectionHandler implements RequestHandler {
        private final CollectionResourceProvider provider;

        private CollectionHandler(final CollectionResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionCollection(parentOf(context), request, handler);
        }

        @Override
        public void handleCreate(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            provider.createInstance(parentOf(context), request, handler);
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
            provider.queryCollection(parentOf(context), request, handler);
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
            provider.actionInstance(parentOf(context), idOf(context), request, handler);
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
            provider.deleteInstance(parentOf(context), idOf(context), request, handler);
        }

        @Override
        public void handlePatch(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            provider.patchInstance(parentOf(context), idOf(context), request, handler);
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
            provider.readInstance(parentOf(context), idOf(context), request, handler);
        }

        @Override
        public void handleUpdate(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            provider.updateInstance(parentOf(context), idOf(context), request, handler);
        }
    }

    // Internal connection implementation.
    private static final class InternalConnection extends AbstractAsynchronousConnection {
        private final RequestHandler requestHandler;

        private InternalConnection(final RequestHandler handler) {
            this.requestHandler = handler;
        }

        @Override
        public Promise<JsonValue, ResourceException> actionAsync(final Context context,
                final ActionRequest request) {
            final PromiseImpl<JsonValue, ResourceException> promise = PromiseImpl.create();
            requestHandler.handleAction(getServerContext(context), request,
                    new ResultHandler<JsonValue>() {
                        @Override
                        public void handleResult(JsonValue result) {
                            promise.handleResult(result);
                        }

                        @Override
                        public void handleError(ResourceException error) {
                            promise.handleError(error);
                        }
                    });
            return promise;
        }

        @Override
        public void close() {
            // Do nothing.
        }

        @Override
        public Promise<Resource, ResourceException> createAsync(final Context context,
                final CreateRequest request) {
            final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
            requestHandler
                    .handleCreate(getServerContext(context), request, adapt(request, promise));
            return promise;
        }

        private ResultHandler<Resource> adapt(final Request request,
                final PromiseImpl<Resource, ResourceException> promise) {
            return new ResultHandler<Resource>() {
                @Override
                public void handleResult(Resource result) {
                    promise.handleResult(filterResource(result, request.getFields()));
                }

                @Override
                public void handleError(ResourceException error) {
                    promise.handleError(error);
                }
            };
        }

        @Override
        public Promise<Resource, ResourceException> deleteAsync(final Context context,
                final DeleteRequest request) {
            final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
            requestHandler
                    .handleDelete(getServerContext(context), request, adapt(request, promise));
            return promise;
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
        public Promise<Resource, ResourceException> patchAsync(final Context context,
                final PatchRequest request) {
            final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
            requestHandler.handlePatch(getServerContext(context), request, adapt(request, promise));
            return promise;
        }

        @Override
        public Promise<QueryResult, ResourceException> queryAsync(final Context context,
                final QueryRequest request, final QueryResultHandler handler) {
            final PromiseImpl<QueryResult, ResourceException> promise = PromiseImpl.create();
            requestHandler.handleQuery(getServerContext(context), request,
                    new QueryResultHandler() {

                        @Override
                        public void handleResult(QueryResult result) {
                            promise.handleResult(result);
                            if (handler != null) {
                                handler.handleResult(result);
                            }
                        }

                        @Override
                        public boolean handleResource(Resource resource) {
                            if (handler != null) {
                                return handler.handleResource(filterResource(resource, request
                                        .getFields()));
                            } else {
                                return true;
                            }
                        }

                        @Override
                        public void handleError(ResourceException error) {
                            promise.handleError(error);
                            if (handler != null) {
                                handler.handleError(error);
                            }
                        }
                    });
            return promise;
        }

        @Override
        public Promise<Resource, ResourceException> readAsync(final Context context,
                final ReadRequest request) {
            final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
            requestHandler.handleRead(getServerContext(context), request, adapt(request, promise));
            return promise;
        }

        @Override
        public Promise<Resource, ResourceException> updateAsync(final Context context,
                final UpdateRequest request) {
            final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
            requestHandler
                    .handleUpdate(getServerContext(context), request, adapt(request, promise));
            return promise;
        }

        private ServerContext getServerContext(final Context context) {
            if (context instanceof ServerContext) {
                return (ServerContext) context;
            } else {
                return new ServerContext(context);
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
        public void close() {
            // Do nothing.
        }

        @Override
        public Connection getConnection() {
            return newInternalConnection(handler);
        }

        public Promise<Connection, ResourceException> getConnectionAsync() {
            return newSuccessfulPromise(getConnection());
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
     * Adapts the provided {@link SynchronousRequestHandler} as a
     * {@link RequestHandler}.
     *
     * @param syncHandler
     *            The synchronous request handler to be adapted.
     * @return The adapted synchronous request handler.
     */
    public static RequestHandler asRequestHandler(final SynchronousRequestHandler syncHandler) {
        return new SynchronousRequestHandlerAdapter(syncHandler);
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
            for (final JsonPointer field : fields) {
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
        router.addRoute(EQUALS, "", collectionHandler);

        // Create a route for the instances within the collection.
        final RequestHandler instanceHandler = new CollectionInstance(provider);
        router.addRoute(EQUALS, "{id}", instanceHandler);

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

    /**
     * Returns an uncloseable view of the provided connection. Attempts to call
     * {@link Connection#close()} will be ignored.
     *
     * @param connection
     *            The connection whose {@code close} method is to be disabled.
     * @return An uncloseable view of the provided connection.
     */
    public static Connection uncloseable(final Connection connection) {
        return new AbstractConnectionWrapper<Connection>(connection) {
            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    /**
     * Returns an uncloseable view of the provided connection factory. Attempts
     * to call {@link ConnectionFactory#close()} will be ignored.
     *
     * @param factory
     *            The connection factory whose {@code close} method is to be
     *            disabled.
     * @return An uncloseable view of the provided connection factory.
     */
    public static ConnectionFactory uncloseable(final ConnectionFactory factory) {
        return new ConnectionFactory() {

            @Override
            public Promise<Connection, ResourceException> getConnectionAsync() {
                return factory.getConnectionAsync();
            }

            @Override
            public Connection getConnection() throws ResourceException {
                return factory.getConnection();
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }

    private static String idOf(final ServerContext context) {
        return context.asContext(RouterContext.class).getUriTemplateVariables().get("id");
    }

    private static ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

    private static final <V> Promise<V, ResourceException> newSuccessfulPromise(V result) {
        return Promises.<V, ResourceException> newSuccessfulPromise(result);
    }

    // Strips off the unwanted leaf routing context which was added when routing
    // requests to a collection.
    private static ServerContext parentOf(final ServerContext context) {
        assert context instanceof RouterContext;
        return (ServerContext) context.getParent();
    }

    // Prevent instantiation.
    private Resources() {
        // Nothing to do.
    }

}
