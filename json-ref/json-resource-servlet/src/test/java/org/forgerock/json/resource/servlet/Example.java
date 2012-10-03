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

package org.forgerock.json.resource.servlet;

import static org.forgerock.json.resource.Context.newRootContext;
import static org.forgerock.json.resource.provider.RoutingMode.EQUALS;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Connections;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.exception.ResourceException;
import org.forgerock.json.resource.provider.RequestHandler;
import org.forgerock.json.resource.provider.Router;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.testng.annotations.Test;

/**
 * An example server which can be run as a unit test.
 */
public class Example {
    private static final Logger LOGGER = Logger.getLogger(Example.class.getName());
    private static final int PORT = 18890;

    /**
     * Creates an example server having the following two containers:
     * <ul>
     * <li>/example/managed/users
     * <li>/example/managed/groups
     * </ul>
     *
     * @throws Exception
     *             If an unexpected IO error occurred.
     */
    @Test
    public void example() throws Exception {
        final RequestHandler handler = createRequestHandler();
        final ConnectionFactory factory = Connections.newInternalConnectionFactory(handler);
        final HttpServer httpServer = HttpServer.createSimpleServer("./", PORT);
        try {
            final WebappContext ctx = new WebappContext("example", "/example");
            final ServletRegistration reg = ctx.addServlet("managed", new HttpServlet(factory));
            reg.addMapping("/managed/*");
            ctx.deploy(httpServer);

            LOGGER.info("Starting server...");
            httpServer.start();
            LOGGER.info("Server started");
            LOGGER.info("Press any key to stop the server...");
            System.in.read();
        } finally {
            LOGGER.info("Stopping server...");
            httpServer.stop();
            LOGGER.info("Server stopped");
        }
    }

    private RequestHandler createRequestHandler() throws ResourceException {
        final Router router = new Router();
        router.addRoute(EQUALS, "/users", new MapBackend());
        router.addRoute(EQUALS, "/groups", new MapBackend());

        // Populate with some test users and groups.
        final Connection connection = Connections.newInternalConnection(router);

        final JsonValue user1 = new JsonValue(new LinkedHashMap<String, Object>());
        user1.add("userName", "alice");
        user1.add("employeeNumber", 1234);
        user1.add("email", "alice@example.com");

        final JsonValue user2 = new JsonValue(new LinkedHashMap<String, Object>());
        user2.add("userName", "bob");
        user2.add("employeeNumber", 2468);
        user2.add("email", "bob@example.com");

        for (final JsonValue user : Arrays.asList(user1, user2)) {
            final CreateRequest request = Requests.newCreateRequest("/users", user);
            connection.create(newRootContext(), request);
        }

        final JsonValue group1 = new JsonValue(new LinkedHashMap<String, Object>());
        group1.add("groupName", "users");
        group1.add("members", Arrays.asList("alice", "bob"));

        final JsonValue group2 = new JsonValue(new LinkedHashMap<String, Object>());
        group2.add("groupName", "administrators");
        group2.add("members", Arrays.asList("alice"));

        for (final JsonValue user : Arrays.asList(group1, group2)) {
            final CreateRequest request = Requests.newCreateRequest("/groups", user);
            connection.create(newRootContext(), request);
        }

        return router;
    }
}
