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
package org.forgerock.json.resource;

import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

/**
 * Example class illustrating usage of API. This class is implemented as a unit
 * test so that we are sure that it compiles and runs. The eventual idea is that
 * its content will be cited in the generated site documentation, but for now
 * its code has to be manually copied.
 */
@SuppressWarnings("javadoc")
public final class ExampleTest {

    @Test
    public void smokeTest() throws ResourceException {
        // Create a new in memory backend which will store user resources.
        InMemoryBackend users = new InMemoryBackend();

        // Create a router request handler and route requests for user
        // resources to the in memory backend.
        Router router = new Router();
        router.addRoute(EQUALS, "/users", users);

        // Obtain an internal connection to the router.
        Connection connection = newInternalConnection(router);

        // Create two users.
        JsonValue alice = new JsonValue(new LinkedHashMap<String, Object>());
        alice.put("name", "Alice");
        alice.put("age", 21);
        alice.put("role", "administrator");
        Resource r1 = connection.create(new RootContext(), newCreateRequest("/users", alice));

        JsonValue bob = new JsonValue(new LinkedHashMap<String, Object>());
        bob.put("name", "Bob");
        bob.put("age", 40);
        bob.put("role", "sales");
        Resource r2 = connection.create(new RootContext(), newCreateRequest("/users", bob));

        // Read a single user.
        Resource r3 = connection.read(new RootContext(), newReadRequest("/users", r1.getId()));
        assertEquals(r1, r3);

        // Update a single user.
        bob.put("role", "marketing");
        Resource r4 = connection.update(new RootContext(), newUpdateRequest("/users", r2.getId(),
                bob));

        // Retrieve the list of users.
        Set<Resource> results = new HashSet<Resource>();
        connection.query(new RootContext(), newQueryRequest("/users"), results);
        assertEquals(results, new HashSet<Resource>(Arrays.asList(r3, r4)));
    }
}
