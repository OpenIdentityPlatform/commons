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

package org.forgerock.json.resource.provider;

import static org.forgerock.json.resource.provider.RoutingMode.EQUALS;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.ResourceException;

/**
 *
 */
public final class RequestHandlers {

    private static final class Collection implements RequestHandler {
        private final CollectionResourceProvider provider;

        private Collection(final CollectionResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final Context context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionCollection(context, request, handler);
        }

        @Override
        public void handleCreate(final Context context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            provider.createInstance(context, request, handler);
        }

        @Override
        public void handleDelete(final Context context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource collection %s cannot be deleted", request.getResourceName()));
        }

        @Override
        public void handlePatch(final Context context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource collection %s cannot be patched", request.getResourceName()));
        }

        @Override
        public void handleQuery(final Context context, final QueryRequest request,
                final QueryResultHandler handler) {
            provider.queryCollection(context, request, handler);
        }

        @Override
        public void handleRead(final Context context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException("The resource collection %s cannot be read",
                    request.getResourceName()));
        }

        @Override
        public void handleUpdate(final Context context, final UpdateRequest request,
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
        public void handleAction(final Context context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionInstance(context, id(context), request, handler);
        }

        @Override
        public void handleCreate(final Context context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource instance %s cannot be created", request.getResourceName()));
        }

        @Override
        public void handleDelete(final Context context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            provider.deleteInstance(context, id(context), request, handler);
        }

        @Override
        public void handlePatch(final Context context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            provider.patchInstance(context, id(context), request, handler);
        }

        @Override
        public void handleQuery(final Context context, final QueryRequest request,
                final QueryResultHandler handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The resource instance %s cannot be queried", request.getResourceName()));
        }

        @Override
        public void handleRead(final Context context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            provider.readInstance(context, id(context), request, handler);
        }

        @Override
        public void handleUpdate(final Context context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            provider.updateInstance(context, id(context), request, handler);
        }

        private String id(final Context context) {
            return Router.URI_TEMPLATE_VARIABLES.get(context).get("id");
        }
    }

    private static final class Singleton implements RequestHandler {
        private final SingletonResourceProvider provider;

        private Singleton(final SingletonResourceProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handleAction(final Context context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            provider.actionInstance(context, request, handler);
        }

        @Override
        public void handleCreate(final Context context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be created", request.getResourceName()));
        }

        @Override
        public void handleDelete(final Context context, final DeleteRequest request,
                final ResultHandler<Resource> handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be deleted", request.getResourceName()));
        }

        @Override
        public void handlePatch(final Context context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            provider.patchInstance(context, request, handler);
        }

        @Override
        public void handleQuery(final Context context, final QueryRequest request,
                final QueryResultHandler handler) {
            // TODO: i18n
            handler.handleError(newBadRequestException(
                    "The singleton resource %s cannot be queried", request.getResourceName()));
        }

        @Override
        public void handleRead(final Context context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            provider.readInstance(context, request, handler);
        }

        @Override
        public void handleUpdate(final Context context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            provider.updateInstance(context, request, handler);
        }
    }

    /**
     * Returns a new request handler which will forward requests on to the
     * provided collection resource provider. Incoming requests which are not
     * appropriate for a resource collection or resource instance will result in
     * a bad request error being returned to the client.
     * <p>
     * The provided URI template must match the resource collection itself, not
     * resource instances. In addition, the URI template must not contain a
     * {@code id} template variable since this will be implicitly added to the
     * template in order for matching against resource instances. For example:
     * 
     * <pre>
     * CollectionResourceProvider users = ...;
     * 
     * // This is valid usage: the template matches the resource collection.
     * RequestHandler handler = newCollection(EQUALS, "/users", users);
     * 
     * // This is invalid usage: the template matches resource instances.
     * RequestHandler handler = newCollection(EQUALS, "/users/{userId}", users);
     * </pre>
     * 
     * @param mode
     *            Indicates how the URI template should be matched against
     *            resource instance names.
     * @param uriTemplate
     *            The URI template which should be used for matching against the
     *            resource collection.
     * @param provider
     *            The collection resource provider.
     * @return A new request handler which will forward requests on to the
     *         provided collection resource provider.
     * @throws IllegalArgumentException
     *             If {@code uriTemplate} contained a template variable called
     *             {@code id}.
     */
    public static RequestHandler newCollection(final RoutingMode mode, final String uriTemplate,
            final CollectionResourceProvider provider) {
        // Route requests to the collection/instance using a router.
        final Router router = new Router();
        addCollectionRoutes(router, mode, uriTemplate, provider);
        return router;
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
        return new Singleton(provider);
    }

    static Route addCollectionRoutes(final Router router, final RoutingMode mode,
            final String uriTemplate, final CollectionResourceProvider provider) {
        if (uriTemplate.contains("{id}")) {
            throw new IllegalArgumentException("uriTemplate contains the variable {id}");
        }

        // Create a route for the instances within the collection.
        final StringBuilder builder = new StringBuilder();
        builder.append(uriTemplate);
        if (!uriTemplate.endsWith("/")) {
            builder.append('/');
        }
        builder.append("{id}");
        final RequestHandler instanceHandler = new CollectionInstance(provider);
        final Route instanceRoute = new Route(mode, builder.toString(), instanceHandler, null);

        // Create a route for the collection.
        final RequestHandler collectionHandler = new Collection(provider);
        final Route collectionRoute = new Route(EQUALS, uriTemplate, collectionHandler,
                instanceRoute);

        // Register the two routes - the instance route is a subroute of the
        // collection so it will be removed when the collection is removed.
        router.addRoute(collectionRoute);
        router.addRoute(instanceRoute);
        return collectionRoute;
    }

    private static ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

    private RequestHandlers() {
        // Prevent instantiation.
    }
}
