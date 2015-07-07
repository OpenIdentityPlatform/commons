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

package org.forgerock.json.resource;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.http.context.ServerContext;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

/**
 * This class contains methods for creating and manipulating connection
 * factories and connections.
 */
public final class Resources {

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
            final Map<String, Object> filtered = new LinkedHashMap<>(fields.size());
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
     *            The collection resource provider. Either an implementation of {@link CollectionResourceProvider} or
     *            a POJO annotated with annotations from {@link org.forgerock.json.resource.annotations}.
     * @return A new request handler which will forward requests on to the
     *         provided collection resource provider.
     */
    public static RequestHandler newCollection(final Object provider) {
        boolean fromInterface = provider instanceof CollectionResourceProvider;
        // Route requests to the collection/instance using a router.
        final Router router = new Router();

        // Create a route for the collection.
        final RequestHandler collectionHandler = fromInterface ?
                new InterfaceCollectionHandler((CollectionResourceProvider) provider) :
                new AnnotatedCollectionHandler(provider);
        router.addRoute(requestUriMatcher(EQUALS, ""), collectionHandler);

        // Create a route for the instances within the collection.
        final RequestHandler instanceHandler = fromInterface ?
                new InterfaceCollectionInstance((CollectionResourceProvider) provider) :
                new AnnotationCollectionInstance(provider);
        router.addRoute(requestUriMatcher(EQUALS, "{id}"), instanceHandler);

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
     *            The singleton resource provider. Either an implementation of {@link SingletonResourceProvider} or
     *            a POJO annotated with annotations from {@link org.forgerock.json.resource.annotations}.
     * @return A new request handler which will forward requests on to the
     *         provided singleton resource provider.
     */
    public static RequestHandler newSingleton(final Object provider) {
        if (provider instanceof SingletonResourceProvider) {
            return new InterfaceSingletonHandler((SingletonResourceProvider) provider);
        } else {
            return new AnnotatedSingletonHandler(provider);
        }
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

    static String idOf(final ServerContext context) {
        return context.asContext(RouterContext.class).getUriTemplateVariables().get("id");
    }

    static ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

    private static <V> Promise<V, ResourceException> newSuccessfulPromise(V result) {
        return newResultPromise(result);
    }

    // Strips off the unwanted leaf routing context which was added when routing
    // requests to a collection.
    static ServerContext parentOf(final ServerContext context) {
        assert context instanceof RouterContext;
        return (ServerContext) context.getParent();
    }

    // Prevent instantiation.
    private Resources() {
        // Nothing to do.
    }

}
