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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.json.resource.examples;

import static org.forgerock.json.resource.examples.DemoUtils.ctx;
import static org.forgerock.json.resource.examples.DemoUtils.log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.UriRouter;
import org.forgerock.resource.core.ServerContext;

/**
 * An example illustrating how you can route realms / sub-realm requests using
 * dynamic routing. Resource URLs are of the form
 * {@code realm0/realm1/.../realmx/users/id}. During dynamic routing temporary
 * routers and request handlers are created on demand in order to maintain state
 * while parsing the request. Dynamic routing is probably a bit overkill for a
 * simple routing model like this.
 */
public final class DynamicRealmDemo {

    /**
     * Main application.
     *
     * @param args
     *            No arguments required.
     * @throws ResourceException
     *             If an unexpected error occurred.
     */
    public static void main(final String... args) throws ResourceException {
        final RequestHandler rootRealm = realm(Collections.<String> emptyList());
        final Connection c = Resources.newInternalConnection(rootRealm);

        // Realm = [], Collection = users, Resource = alice
        c.read(ctx(), Requests.newReadRequest("users/alice"));

        // Realm = [], Collection = groups, Resource = administrators
        c.read(ctx(), Requests.newReadRequest("groups/administrators"));

        // Realm = [a], Collection = users, Resource = alice
        c.read(ctx(), Requests.newReadRequest("a/users/alice"));

        // Realm = [a, b], Collection = users, Resource = alice
        c.read(ctx(), Requests.newReadRequest("a/b/users/alice"));
    }

    /**
     * Returns a collection for handling users or groups.
     *
     * @param path
     *            The realm containing the users or groups.
     * @param name
     *            The type of collection, e.g. users or groups.
     * @return A collection for handling users or groups.
     */
    private static CollectionResourceProvider collection(final List<String> path, final String name) {
        return new CollectionResourceProvider() {

            @Override
            public void actionCollection(final ServerContext context, final ActionRequest request,
                    final ResultHandler<JsonValue> handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void actionInstance(final ServerContext context, final String resourceId,
                    final ActionRequest request, final ResultHandler<JsonValue> handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void createInstance(final ServerContext context, final CreateRequest request,
                    final ResultHandler<Resource> handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void deleteInstance(final ServerContext context, final String resourceId,
                    final DeleteRequest request, final ResultHandler<Resource> handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void patchInstance(final ServerContext context, final String resourceId,
                    final PatchRequest request, final ResultHandler<Resource> handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void queryCollection(final ServerContext context, final QueryRequest request,
                    final QueryResultHandler handler) {
                handler.handleError(new NotSupportedException());
            }

            @Override
            public void readInstance(final ServerContext context, final String resourceId,
                    final ReadRequest request, final ResultHandler<Resource> handler) {
                log("Reading " + name);
                log("    resource ID : " + resourceId);
                log("    realm path  : " + path);
                final JsonValue content =
                        new JsonValue(Collections.singletonMap("id", (Object) resourceId));
                handler.handleResult(new Resource(resourceId, "1", content));
            }

            @Override
            public void updateInstance(final ServerContext context, final String resourceId,
                    final UpdateRequest request, final ResultHandler<Resource> handler) {
                handler.handleError(new NotSupportedException());
            }
        };
    }

    /**
     * Returns a request handler which will handle all requests to a realm,
     * including sub-realms, users, and groups.
     *
     * @param path
     *            The realm.
     * @return A request handler which will handle all requests to a realm,
     *         including sub-realms, users, and groups.
     */
    private static RequestHandler realm(final List<String> path) {
        final UriRouter router = new UriRouter();
        router.addRoute("/users", collection(path, "user"));
        router.addRoute("/groups", collection(path, "group"));
        router.addRoute(RoutingMode.STARTS_WITH, "/{realm}", subrealms(path));
        return router;
    }

    /**
     * Returns a request handler which will handle requests to a sub-realm.
     *
     * @param parentPath
     *            The parent realm.
     * @return A request handler which will handle requests to a sub-realm.
     */
    private static RequestHandler subrealms(final List<String> parentPath) {
        return new AbstractRequestHandler() {
            @Override
            public void handleRead(final ServerContext context, final ReadRequest request,
                    final ResultHandler<Resource> handler) {
                subrealm(parentPath, context).handleRead(context, request, handler);
            }

            private RequestHandler subrealm(final List<String> parentPath,
                    final ServerContext context) {
                final String realm =
                        context.asContext(RouterContext.class).getUriTemplateVariables().get(
                                "realm");
                final List<String> path = new LinkedList<String>(parentPath);
                path.add(realm);

                // TODO: check that the path references an existing realm?
                return realm(path);
            }
        };
    }

    private DynamicRealmDemo() {
        // Prevent instantiation.
    }
}
