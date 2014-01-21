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

import java.util.Collections;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;

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
        c.read(new RootContext(), Requests.newReadRequest("users/alice"));

        // Realm = [], Collection = groups, Resource = administrators
        c.read(new RootContext(), Requests.newReadRequest("groups/administrators"));

        // Realm = [a], Collection = users, Resource = alice
        c.read(new RootContext(), Requests.newReadRequest("a/users/alice"));

        // Realm = [a, b], Collection = users, Resource = alice
        c.read(new RootContext(), Requests.newReadRequest("a/b/users/alice"));
    }

    /**
     * Returns a request handler which will handle requests to a sub-realm.
     *
     * @return A request handler which will handle requests to a sub-realm.
     */
    private static RequestHandler simpleRouter() {
        return new AbstractRequestHandler() {
            @Override
            public void handleRead(final ServerContext context, final ReadRequest request,
                    final ResultHandler<Resource> handler) {
                final ResourceName name = request.getResourceNameObject();
                final int size = name.size();
                if (size == 0) {
                    System.out.println("Reading root");
                } else if (name.leaf().equals("users")) {
                    System.out.println("Reading users container in "
                            + name.subSequence(0, size - 1));
                } else if (name.leaf().equals("groups")) {
                    System.out.println("Reading groups container in "
                            + name.subSequence(0, size - 1));
                } else if (size > 1) {
                    if (name.get(size - 2).equals("users")) {
                        read("user", name);
                    } else if (name.get(size - 2).equals("groups")) {
                        read("group", name);
                    } else {
                        System.out.println("Reading realm " + name);
                    }
                } else {
                    System.out.println("Reading realm " + name);
                }

                final JsonValue content =
                        new JsonValue(Collections.singletonMap("id", (Object) name.leaf()));
                handler.handleResult(new Resource(name.leaf(), "1", content));
            }
        };
    }

    private static void read(final String type, final ResourceName path) {
        System.out.println("Reading " + type);
        System.out.println("    resource ID : " + path.leaf());
        System.out.println("    realm path  : " + path.subSequence(0, path.size() - 2));
    }

    private SimpleRealmDemo() {
        // Prevent instantiation.
    }
}
