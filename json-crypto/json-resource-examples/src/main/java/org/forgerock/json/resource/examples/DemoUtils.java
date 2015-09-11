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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.Router.uriTemplate;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;

/**
 * Utility methods for demo apps.
 */
final class DemoUtils {
    static boolean isUnitTest = false;

    private DemoUtils() {
        // No implementation.
    }

    static Context ctx() {
        return new RootContext();
    }

    static void log(final String message) {
        if (!isUnitTest) {
            System.out.println(message);
        }
    }

    static JsonValue userAliceWithIdAndRev(final int id, final int rev) {
        return json(object(field("name", "alice"), field("age", 20), field("role", "sales"), field(
                "_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

    static ConnectionFactory getConnectionFactory() throws ResourceException {
        final MemoryBackend users = new MemoryBackend();
        final Router router = new Router();
        router.addRoute(uriTemplate("users"), users);
        final Connection connection = newInternalConnection(router);
        connection.create(ctx(), newCreateRequest("users", "1", userAliceWithIdAndRev(1, 0)));
        return Resources.newInternalConnectionFactory(router);
    }
}
