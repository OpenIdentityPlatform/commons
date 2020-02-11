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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.examples;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.json.resource.examples.DemoUtils.ctx;
import static org.forgerock.json.resource.examples.DemoUtils.log;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

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
            public Promise<ActionResponse, ResourceException> actionCollection(final Context context,
                    final ActionRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<ActionResponse, ResourceException> actionInstance(final Context context,
                    final String resourceId, final ActionRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
                    final CreateRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context,
                    final String resourceId, final DeleteRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> patchInstance(final Context context,
                    final String resourceId, final PatchRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<QueryResponse, ResourceException> queryCollection(final Context context,
                    final QueryRequest request, final QueryResourceHandler handler) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> readInstance(final Context context,
                    final String resourceId, final ReadRequest request) {
                log("Reading " + name);
                log("    resource ID : " + resourceId);
                log("    realm path  : " + path);
                final JsonValue content =
                        new JsonValue(Collections.singletonMap("id", (Object) resourceId));
                return newResultPromise(newResourceResponse(resourceId, "1", content));
            }

            @Override
            public Promise<ResourceResponse, ResourceException> updateInstance(final Context context,
                    final String resourceId, final UpdateRequest request) {
                ResourceException e = new NotSupportedException();
                return newExceptionPromise(e);
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
        final Router router = new Router();
        router.addRoute(uriTemplate("/users"), collection(path, "user"));
        router.addRoute(uriTemplate("/groups"), collection(path, "group"));
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/{realm}"), subrealms(path));
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
            public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                    final ReadRequest request) {
                return subrealm(parentPath, context).handleRead(context, request);
            }

            private RequestHandler subrealm(final List<String> parentPath, final Context context) {
                final String realm = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("realm");
                final List<String> path = new LinkedList<>(parentPath);
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
