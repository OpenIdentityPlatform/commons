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
import static org.forgerock.json.resource.examples.DemoUtils.ctx;
import static org.forgerock.json.resource.examples.DemoUtils.log;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collections;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.util.promise.Promise;

/**
 * An example illustrating how you can route realms / sub-realm requests using
 * simple hand-crafted routing. Resource URLs are of the form
 * {@code realm0/realm1/.../realmx/users/id}. For simple resource hierarchies
 * this approach is simpler than using dynamic routing.
 */
public final class SimpleRealmDemo {
    /**
     * Main application.
     *
     * @param args
     *            No arguments required.
     * @throws ResourceException
     *             If an unexpected error occurred.
     */
    public static void main(final String... args) throws ResourceException {
        final RequestHandler rootRealm = simpleRouter();
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
     * Returns a request handler which will handle requests to a sub-realm.
     *
     * @return A request handler which will handle requests to a sub-realm.
     */
    private static RequestHandler simpleRouter() {
        return new AbstractRequestHandler() {
            @Override
            public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                    final ReadRequest request) {
                final ResourcePath name = request.getResourcePathObject();
                final int size = name.size();
                if (size == 0) {
                    log("Reading root");
                } else if (name.leaf().equals("users")) {
                    log("Reading users container in " + name.subSequence(0, size - 1));
                } else if (name.leaf().equals("groups")) {
                    log("Reading groups container in " + name.subSequence(0, size - 1));
                } else if (size > 1) {
                    if (name.get(size - 2).equals("users")) {
                        read("user", name);
                    } else if (name.get(size - 2).equals("groups")) {
                        read("group", name);
                    } else {
                        log("Reading realm " + name);
                    }
                } else {
                    log("Reading realm " + name);
                }

                final JsonValue content =
                        new JsonValue(Collections.singletonMap("id", (Object) name.leaf()));
                return newResultPromise(newResourceResponse(name.leaf(), "1", content));
            }
        };
    }

    private static void read(final String type, final ResourcePath path) {
        log("Reading " + type);
        log("    resource ID : " + path.leaf());
        log("    realm path  : " + path.subSequence(0, path.size() - 2));
    }

    private SimpleRealmDemo() {
        // Prevent instantiation.
    }
}
