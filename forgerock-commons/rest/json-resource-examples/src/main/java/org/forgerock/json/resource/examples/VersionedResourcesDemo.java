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

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.RouteMatchers.requestResourceApiVersionMatcher;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.json.resource.examples.DemoUtils.*;

import org.forgerock.services.context.Context;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.MemoryBackend;
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
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * An example client application which performs asynchronous reads against different versions of the same resource.
 *
 * @since 2.4.0
 */
public final class VersionedResourcesDemo {

    private VersionedResourcesDemo() {
        // No implementation.
    }

    /**
     * Main application.
     *
     * @param args
     *            CLI arguments.
     * @throws ResourceException
     *             If an unexpected error occurs.
     */
    public static void main(String... args) throws ResourceException {
        try (ConnectionFactory server = getConnectionFactory();
             Connection connection = server.getConnection()) {
            log("Reading version 1.0 of resource");
            ResourceResponse response = connection.read(ctx(), Requests.newReadRequest("users/1")
                    .setResourceVersion(version(1)));
            log("Retrieved resource with revision: " + response.getRevision());


            log("Reading version 1.5 of resource");
            response = connection.read(ctx(), Requests.newReadRequest("users/1")
                    .setResourceVersion(version(1, 5)));
            log("Retrieved resource with revision: " + response.getRevision());

            log("Reading version 2.0 of resource");
            response = connection.read(ctx(), Requests.newReadRequest("users/1")
                    .setResourceVersion(version(2)));
            log("Retrieved resource with revision: " + response.getRevision());
        }
    }

    private static ConnectionFactory getConnectionFactory() throws ResourceException {
        MemoryBackend usersV1Dot0 = new MemoryBackend();
        MemoryBackend usersV1Dot5 = new MemoryBackend();
        MemoryBackend usersV2Dot0 = new MemoryBackend();

        RequestHandler rolesV1Dot0 = handler(new MemoryBackend());
        RequestHandler rolesV1Dot5 = handler(new MemoryBackend());
        RequestHandler rolesV2Dot0 = handler(new MemoryBackend());

        SingletonResourceProvider configV1Dot0 = singleton(new MemoryBackend());
        SingletonResourceProvider configV1Dot5 = singleton(new MemoryBackend());
        SingletonResourceProvider configV2Dot0 = singleton(new MemoryBackend());

        MemoryBackend groups = new MemoryBackend();

        Router router = new Router();
        Router usersRouter = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/users"), usersRouter);
        usersRouter.addRoute(version(1), usersV1Dot0);
        usersRouter.addRoute(version(1, 5), usersV1Dot5);
        usersRouter.addRoute(version(2), usersV2Dot0);

        Router rolesRouter = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.EQUALS, "/roles"), rolesRouter);
        rolesRouter.addRoute(requestResourceApiVersionMatcher(version(1)), rolesV1Dot0);
        rolesRouter.addRoute(requestResourceApiVersionMatcher(version(1, 5)), rolesV1Dot5);
        rolesRouter.addRoute(requestResourceApiVersionMatcher(version(2)), rolesV2Dot0);

        Router configRouter = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/config"), configRouter);
        configRouter.addRoute(version(1), configV1Dot0);
        configRouter.addRoute(version(1, 5), configV1Dot5);
        configRouter.addRoute(version(2), configV2Dot0);

        // Ignores any version information.
        router.addRoute(uriTemplate("groups"), groups);

        final Connection connection = newInternalConnection(router);
        connection.create(ctx(), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 0))
                .setResourceVersion(version(1)));
        connection.create(ctx(), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 1))
                .setResourceVersion(version(1, 5)));
        connection.create(ctx(), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 2))
                .setResourceVersion(version(2)));

        return Resources.newInternalConnectionFactory(router);
    }

    private static SingletonResourceProvider singleton(final MemoryBackend backend) {
        return new SingletonResourceProvider() {

            @Override
            public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
                return backend.actionInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
                return backend.patchInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
                return backend.readInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
                return backend.updateInstance(context, "INSTANCE", request);
            }
        };
    }

    private static RequestHandler handler(final MemoryBackend backend) {
        return new RequestHandler() {
            @Override
            public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
                return backend.actionInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
                return backend.createInstance(context, request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
                return backend.deleteInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
                return backend.patchInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
                    QueryResourceHandler handler) {
                return backend.queryCollection(context, request, handler);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
                return backend.readInstance(context, "INSTANCE", request);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
                return backend.updateInstance(context, "INSTANCE", request);
            }
        };
    }
}
