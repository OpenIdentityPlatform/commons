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
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Resources {

    private static final class SynchronousRequestHandlerAdapter implements RequestHandler {
        private final SynchronousRequestHandler syncHandler;

        /**
         * @param syncHandler
         */
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

    /**
     * An abstract future result which acts as a result handler.
     */
    private static abstract class AbstractFutureResultHandler<V, H extends ResultHandler<? super V>>
            implements FutureResult<V>, ResultHandler<V> {
        private ResourceException error = null;
        private final H innerHandler;
        private final CountDownLatch latch = new CountDownLatch(1);
        private V result = null;

        /**
         * Creates a new future.
         */
        private AbstractFutureResultHandler(final H innerHandler) {
            this.innerHandler = innerHandler;
        }

        @Override
        public final boolean cancel(final boolean mayInterruptIfRunning) {
            // Cancellation not supported.
            return false;
        }

        @Override
        public final V get() throws ResourceException, InterruptedException {
            latch.await();
            return get0();
        }

        @Override
        public final V get(final long timeout, final TimeUnit unit) throws ResourceException,
                TimeoutException, InterruptedException {
            if (latch.await(timeout, unit)) {
                return get0();
            } else {
                throw new TimeoutException();
            }
        }

        @Override
        public final void handleError(final ResourceException error) {
            try {
                if (innerHandler != null) {
                    innerHandler.handleError(error);
                }
            } finally {
                this.error = error;
                latch.countDown();
            }
        }

        @Override
        public final void handleResult(final V result) {
            final V transformedResult = transform(result);
            try {
                if (innerHandler != null) {
                    innerHandler.handleResult(transformedResult);
                }
            } finally {
                this.result = transformedResult;
                latch.countDown();
            }
        }

        @Override
        public final boolean isCancelled() {
            // Cancellation not supported.
            return false;
        }

        @Override
        public final boolean isDone() {
            return latch.getCount() == 0;
        }

        final H getInnerHandler() {
            return innerHandler;
        }

        V transform(final V result) {
            return result;
        }

        private V get0() throws ResourceException {
            if (error == null) {
                return result;
            } else {
                throw error;
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

    /**
     * A future JsonValue which acts as a result handler.
     */
    private static final class FutureJsonValueHandler extends
            AbstractFutureResultHandler<JsonValue, ResultHandler<? super JsonValue>> {
        private FutureJsonValueHandler(final ResultHandler<? super JsonValue> handler) {
            super(handler);
        }
    }

    /**
     * A future query result which acts as a query result handler.
     */
    private static final class FutureQueryResultHandler extends
            AbstractFutureResultHandler<QueryResult, QueryResultHandler> implements
            QueryResultHandler {
        private final QueryRequest request;

        private FutureQueryResultHandler(final QueryRequest request,
                final QueryResultHandler handler) {
            super(handler);
            this.request = request;
        }

        @Override
        public boolean handleResource(final Resource resource) {
            final QueryResultHandler handler = getInnerHandler();
            if (handler != null) {
                return handler.handleResource(filterResource(resource, request.getFields()));
            } else {
                return true;
            }
        }
    }

    /**
     * A future resource which acts as a result handler.
     */
    private static final class FutureResourceHandler extends
            AbstractFutureResultHandler<Resource, ResultHandler<? super Resource>> {
        private final Request request;

        private FutureResourceHandler(final Request request,
                final ResultHandler<? super Resource> handler) {
            super(handler);
            this.request = request;
        }

        @Override
        protected Resource transform(final Resource result) {
            return filterResource(result, request.getFields());
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
                final ActionRequest request, final ResultHandler<? super JsonValue> handler) {
            final FutureJsonValueHandler future = new FutureJsonValueHandler(handler);
            requestHandler.handleAction(getServerContext(context), request, future);
            return future;
        }

        @Override
        public void close() {
            // Do nothing.
        }

        @Override
        public FutureResult<Resource> createAsync(final Context context,
                final CreateRequest request, final ResultHandler<? super Resource> handler) {
            final FutureResourceHandler future = new FutureResourceHandler(request, handler);
            requestHandler.handleCreate(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> deleteAsync(final Context context,
                final DeleteRequest request, final ResultHandler<? super Resource> handler) {
            final FutureResourceHandler future = new FutureResourceHandler(request, handler);
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
                final ResultHandler<? super Resource> handler) {
            final FutureResourceHandler future = new FutureResourceHandler(request, handler);
            requestHandler.handlePatch(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<QueryResult> queryAsync(final Context context,
                final QueryRequest request, final QueryResultHandler handler) {
            final FutureQueryResultHandler future = new FutureQueryResultHandler(request, handler);
            requestHandler.handleQuery(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> readAsync(final Context context, final ReadRequest request,
                final ResultHandler<? super Resource> handler) {
            final FutureResourceHandler future = new FutureResourceHandler(request, handler);
            requestHandler.handleRead(getServerContext(context), request, future);
            return future;
        }

        @Override
        public FutureResult<Resource> updateAsync(final Context context,
                final UpdateRequest request, final ResultHandler<? super Resource> handler) {
            final FutureResourceHandler future = new FutureResourceHandler(request, handler);
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
        public void close() {
            // Do nothing.
        }

        @Override
        public Connection getConnection() {
            return newInternalConnection(handler);
        }

        @Override
        public FutureResult<Connection> getConnectionAsync(
                final ResultHandler<? super Connection> handler) {
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
            public FutureResult<Connection> getConnectionAsync(
                    final ResultHandler<? super Connection> handler) {
                return factory.getConnectionAsync(handler);
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

    static <T> T checkNotNull(final T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    // Ensures that the resource name does not begin or end with forward slashes.
    static String normalizeResourceName(final String name) {
        String tmp = name;
        if (tmp.startsWith("/")) {
            tmp = tmp.substring(1);
        }
        if (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        return tmp;
    }

    private static String idOf(final ServerContext context) {
        return context.asContext(RouterContext.class).getUriTemplateVariables().get("id");
    }

    private static ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
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
