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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.api.models.Parameter.*;
import static org.forgerock.api.models.Resource.AnnotatedTypeVariant.*;
import static org.forgerock.api.models.SubResources.*;
import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.json.resource.RouteMatchers.*;
import static org.forgerock.util.promise.Promises.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Path;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.SubResources;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Reject;
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
        return syncHandler instanceof Describable
                ? new DescribedSyncRequestHandlerAdapter(syncHandler)
                : new SynchronousRequestHandlerAdapter(syncHandler);
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
    public static ResourceResponse filterResource(final ResourceResponse resource,
            final Collection<JsonPointer> fields) {
        final JsonValue unfiltered = resource.getContent();
        final Collection<JsonPointer> filterFields = resource.hasFields()
                ? resource.getFields()
                : fields;
        final JsonValue filtered = filterResource(unfiltered, filterFields);
        if (filtered == unfiltered) {
            return resource; // Unchanged.
        } else {
            return newResourceResponse(resource.getId(), resource.getRevision(), filtered);
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
        return new InternalConnectionFactory(handler);
    }

    /**
     * Creates a new {@link RequestHandler} backed by the supplied provider. The provider can be an instance of an
     * interface resource handler, and annotated resource handler or an annotated request handler. The type of the
     * provider will be determined from the {@code org.forgerock.api.annotations.RequestHandler#variant}
     * annotation property, or from the type of interface implemented.
     * <p>
     * Sub-paths that are declared using the {@code org.forgerock.api.annotations.Path} annotation will also be routed
     * through the returned handler.
     * <p>
     * This method uses the same logic as {@link #newCollection(Object)}, {@link #newSingleton(Object)} and
     * {@link #newAnnotatedRequestHandler(Object)} to create the underlying {@link RequestHandler}s.
     * @param provider The provider instance.
     * @return The constructed handler.
     */
    public static RequestHandler newHandler(Object provider) {
        Router router;
        if (provider instanceof Describable) {
            router = new Router();
            addHandlers(provider, router, "", null);
        } else {
            final DescribableResourceHandler descriptorProvider = new DescribableResourceHandler();
            router = new Router() {
                @Override
                protected ApiDescription buildApi(ApiProducer<ApiDescription> producer) {
                    return descriptorProvider.api(producer);
                }
            };
            descriptorProvider.describes(addHandlers(provider, router, "",
                    descriptorProvider.getDefinitionDescriptions()));
        }
        Path path = provider.getClass().getAnnotation(Path.class);
        if (path != null) {
            Router pathRouter = new Router();
            pathRouter.addRoute(requestUriMatcher(STARTS_WITH, path.value()), router);
            router = pathRouter;
        }
        return router;
    }

    private static Resource addHandlers(Object provider, Router router, String basePath,
            ApiDescription definitions, Parameter... pathParameters) {
        HandlerVariant variant = deduceHandlerVariant(provider);
        Parameter[] nextPathParameters = pathParameters;
        switch (variant) {
        case SINGLETON_RESOURCE:
            addSingletonHandlerToRouter(provider, router, basePath);
            break;
        case COLLECTION_RESOURCE:
            nextPathParameters = new Parameter[pathParameters.length + 1];
            System.arraycopy(pathParameters, 0, nextPathParameters, 0, pathParameters.length);
            nextPathParameters[pathParameters.length] = addCollectionHandlersToRouter(provider, router, basePath);
            String pathParameter = nextPathParameters[pathParameters.length].getName();
            basePath = (basePath.isEmpty() ? "" : basePath + "/") + "{" + pathParameter + "}";
            break;
        case REQUEST_HANDLER:
            addRequestHandlerToRouter(provider, router, basePath);
            break;
        default:
            return null;
        }

        SubResources.Builder subResourcesBuilder = null;
        for (Method m : provider.getClass().getMethods()) {
            Path subpathAnnotation = m.getAnnotation(Path.class);
            if (subpathAnnotation != null) {
                if (subResourcesBuilder == null) {
                    subResourcesBuilder = subresources();
                }
                String subpath = subpathAnnotation.value().replaceAll("^/", "");
                try {
                    Resource subResource = addHandlers(m.invoke(provider), router,
                            basePath.isEmpty() ? subpath : basePath + "/" + subpath, definitions, nextPathParameters);
                    if (subResource != null) {
                        subResourcesBuilder.put(subpath, subResource);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException("Could not construct handler tree", e);
                }
            }
        }
        SubResources subResources = subResourcesBuilder == null ? null : subResourcesBuilder.build();
        return makeDescriptor(provider, variant, subResources, definitions, pathParameters);
    }

    private static HandlerVariant deduceHandlerVariant(Object provider) {
        Class<?> type = provider.getClass();
        org.forgerock.api.annotations.RequestHandler handler =
                provider.getClass().getAnnotation(org.forgerock.api.annotations.RequestHandler.class);
        if (handler != null || provider instanceof RequestHandler) {
            return HandlerVariant.REQUEST_HANDLER;
        } else if (type.getAnnotation(SingletonProvider.class) != null
                || provider instanceof SingletonResourceProvider) {
            return HandlerVariant.SINGLETON_RESOURCE;
        } else if (type.getAnnotation(CollectionProvider.class) != null
                || provider instanceof CollectionResourceProvider) {
            return HandlerVariant.COLLECTION_RESOURCE;
        }
        throw new IllegalArgumentException("Cannot deduce provider variant" + provider.getClass());
    }

    private static void addRequestHandlerToRouter(Object provider, Router router, String basePath) {
        router.addRoute(requestUriMatcher(STARTS_WITH, basePath), new AnnotatedRequestHandler(provider));
    }

    private static Parameter addCollectionHandlersToRouter(Object provider, Router router, String basePath) {
        boolean fromInterface = provider instanceof CollectionResourceProvider;
        // Create a route for the collection.
        final RequestHandler collectionHandler = fromInterface
                ? new InterfaceCollectionHandler((CollectionResourceProvider) provider)
                : new AnnotatedCollectionHandler(provider);
        router.addRoute(requestUriMatcher(EQUALS, basePath), collectionHandler);

        // Create a route for the instances within the collection.
        RequestHandler instanceHandler = fromInterface
                ? new InterfaceCollectionInstance((CollectionResourceProvider) provider)
                : new AnnotationCollectionInstance(provider);
        Class<?> providerClass = provider.getClass();
        CollectionProvider providerAnnotation = providerClass.getAnnotation(CollectionProvider.class);
        Parameter pathParameter;
        if (providerAnnotation != null) {
            pathParameter = Parameter.fromAnnotation(providerClass, providerAnnotation.pathParam());
        } else {
            pathParameter = parameter().name("id").type("string").source(ParameterSource.PATH).required(true).build();
        }
        String pathParam = pathParameter.getName();
        instanceHandler = new FilterChain(instanceHandler, new CollectionInstanceIdContextFilter(pathParam));
        router.addRoute(requestUriMatcher(EQUALS, (basePath.isEmpty() ? "" : basePath + "/") + "{" + pathParam + "}"),
                instanceHandler);
        return pathParameter;
    }

    private static void addSingletonHandlerToRouter(Object provider, Router router, String basePath) {
        router.addRoute(requestUriMatcher(EQUALS, basePath),
                provider instanceof SingletonResourceProvider
                        ? new InterfaceSingletonHandler((SingletonResourceProvider) provider)
                        : new AnnotatedSingletonHandler(provider));
    }

    private static Resource makeDescriptor(Object provider, HandlerVariant variant, SubResources subResources,
            ApiDescription definitions, Parameter[] pathParameters) {
        if (provider instanceof Describable) {
            return null;
        }
        Class<?> type = provider.getClass();
        switch (variant) {
        case SINGLETON_RESOURCE:
            return Resource.fromAnnotatedType(type, SINGLETON_RESOURCE, subResources, definitions, pathParameters);
        case COLLECTION_RESOURCE:
            final Items items = Items.fromAnnotatedType(type, definitions, subResources);
            return Resource.fromAnnotatedType(type, COLLECTION_RESOURCE_COLLECTION, items,
                    definitions, pathParameters);
        case REQUEST_HANDLER:
            return Resource.fromAnnotatedType(type, REQUEST_HANDLER, subResources, definitions, pathParameters);
        default:
            return null;
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
     * @deprecated Use {@link #newHandler(Object)} instead.
     */
    @Deprecated
    public static RequestHandler newCollection(final Object provider) {
        return newHandler(provider);
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
     * @deprecated Use {@link #newHandler(Object)} instead.
     */
    @Deprecated
    public static RequestHandler newSingleton(final Object provider) {
        return newHandler(provider);
    }

    /**
     * Returns a new request handler which will forward requests on to the
     * provided annotated request handler.
     *
     * @param provider
     *            The POJO annotated with annotations from {@link org.forgerock.json.resource.annotations}.
     * @return A new request handler which will forward requests on to the provided annotated POJO.
     * @deprecated Use {@link #newHandler(Object)} instead.
     */
    @Deprecated
    public static RequestHandler newAnnotatedRequestHandler(final Object provider) {
        Reject.ifTrue(provider instanceof RequestHandler,
                "Refusing to create an annotated request handler using a provider that implements RequestHandler. "
                        + "Use the RequestHandler implementation directly instead");
        return newHandler(provider);
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

    static String idOf(final Context context) {
        String idFieldName = context.asContext(IdFieldContext.class).getIdFieldName();
        return context.asContext(UriRouterContext.class).getUriTemplateVariables().get(idFieldName);
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
    static Context parentOf(Context context) {
        if (context instanceof IdFieldContext) {
            context = context.getParent();
        }
        assert context instanceof UriRouterContext;
        return context.getParent();
    }

    private static class IdFieldContext extends AbstractContext {

        private static final String ID_FIELD_NAME = "IdFieldName";

        protected IdFieldContext(Context parent, String idFieldName) {
            super(parent, "IdField");
            this.data.put(ID_FIELD_NAME, idFieldName);
        }

        protected IdFieldContext(JsonValue data, ClassLoader loader) {
            super(data, loader);
        }

        String getIdFieldName() {
            return this.data.get(ID_FIELD_NAME).asString();
        }
    }

    private static final class CollectionInstanceIdContextFilter implements Filter {

        private final String idFieldName;

        private CollectionInstanceIdContextFilter(String idFieldName) {
            this.idFieldName = idFieldName;
        }

        @Override
        public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
                RequestHandler next) {
            return next.handleAction(new IdFieldContext(context, idFieldName), request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
                RequestHandler next) {
            return next.handleCreate(new IdFieldContext(context, idFieldName), request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
                RequestHandler next) {
            return next.handleDelete(new IdFieldContext(context, idFieldName), request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
                RequestHandler next) {
            return next.handlePatch(new IdFieldContext(context, idFieldName), request);
        }

        @Override
        public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
                QueryResourceHandler handler, RequestHandler next) {
            return next.handleQuery(new IdFieldContext(context, idFieldName), request, handler);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
                RequestHandler next) {
            return next.handleRead(new IdFieldContext(context, idFieldName), request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
                RequestHandler next) {
            return next.handleUpdate(new IdFieldContext(context, idFieldName), request);
        }
    }

    /**
     * Enumeration of the possible CREST handler variants.
     */
    enum HandlerVariant {
        /** A singleton resource handler. */
        SINGLETON_RESOURCE,
        /** A collection resource handler. */
        COLLECTION_RESOURCE,
        /** A plain request handler. */
        REQUEST_HANDLER
    }

    // Prevent instantiation.
    private Resources() {
        // Nothing to do.
    }
}
