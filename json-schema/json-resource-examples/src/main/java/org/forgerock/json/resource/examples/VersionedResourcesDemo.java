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

import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.examples.DemoUtils.*;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AcceptAPIVersion;
import org.forgerock.json.resource.AcceptAPIVersionContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.MemoryBackend;
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
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.UriRouter;
import org.forgerock.json.resource.VersionRouter;
import org.forgerock.resource.core.Context;

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
        ConnectionFactory server = getConnectionFactory();
        Connection connection = server.getConnection();
        try {
            log("Reading version 1.0 of resource");
            Resource resource = connection.read(apiCtx("1.0"), Requests.newReadRequest("users/1"));
            log("Retrieved resource with revision: " + resource.getRevision());


            log("Reading version 1.5 of resource");
            resource = connection.read(apiCtx("1.5"), Requests.newReadRequest("users/1"));
            log("Retrieved resource with revision: " + resource.getRevision());

            log("Reading version 2.0 of resource");
            resource = connection.read(apiCtx("2.0"), Requests.newReadRequest("users/1"));
            log("Retrieved resource with revision: " + resource.getRevision());
        } finally {
            log("Closing connection");
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static Context apiCtx(String resourceVersion) {
        return new AcceptAPIVersionContext(ctx(), "PROTOCOL_NAME",
                AcceptAPIVersion.newBuilder().withDefaultProtocolVersion("1.0").
                        withDefaultResourceVersion(resourceVersion).build());
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

        UriRouter router = new UriRouter();
        router.addRoute(RoutingMode.STARTS_WITH, "/users", new VersionRouter()
                .addVersion("1", usersV1Dot0)
                .addVersion("1.5", usersV1Dot5)
                .addVersion("2.0", usersV2Dot0));

        router.addRoute(RoutingMode.EQUALS, "/roles", new VersionRouter()
                .addVersion("1.0", rolesV1Dot0)
                .addVersion("1.5", rolesV1Dot5)
                .addVersion("2.0", rolesV2Dot0));

        router.addRoute(RoutingMode.STARTS_WITH, "/config", new VersionRouter()
                .addVersion("1.0", configV1Dot0)
                .addVersion("1.5", configV1Dot5)
                .addVersion("2.0", configV2Dot0));

        // Ignores any version information.
        router.addRoute("groups", groups);

        final Connection connection = newInternalConnection(router);
        connection.create(apiCtx("1.0"), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 0)));
        connection.create(apiCtx("1.5"), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 1)));
        connection.create(apiCtx("2.0"), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 2)));

        return Resources.newInternalConnectionFactory(router);
    }

    private static SingletonResourceProvider singleton(final MemoryBackend backend) {
        return new SingletonResourceProvider() {

            @Override
            public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
                backend.actionInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
                backend.patchInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
                backend.readInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
                backend.updateInstance(context, "INSTANCE", request, handler);
            }
        };
    }

    private static RequestHandler handler(final MemoryBackend backend) {
        return new RequestHandler() {
            @Override
            public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
                backend.actionInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
                backend.createInstance(context, request, handler);
            }

            @Override
            public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
                backend.deleteInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
                backend.patchInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
                backend.queryCollection(context, request, handler);
            }

            @Override
            public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
                backend.readInstance(context, "INSTANCE", request, handler);
            }

            @Override
            public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
                backend.updateInstance(context, "INSTANCE", request, handler);
            }
        };
    }
}