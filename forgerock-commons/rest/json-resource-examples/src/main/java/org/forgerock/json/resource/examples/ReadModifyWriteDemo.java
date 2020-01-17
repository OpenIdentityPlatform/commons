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

import static org.forgerock.json.resource.examples.DemoUtils.*;

import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;

/**
 * An example client application which performs an asynchronous read, modify,
 * write update cycle for a resource in an in memory resource container. This
 * example does not require any command line arguments.
 */
public final class ReadModifyWriteDemo {

    private ReadModifyWriteDemo() {
        // No implementation.
    }

    /**
     * Main method.
     *
     * @param args
     *            The command line arguments: this example does not have any.
     * @throws ResourceException
     *             If an unexpected error occurred.
     */
    public static void main(final String[] args) throws ResourceException {
        try (final ConnectionFactory server = getConnectionFactory();
             final Connection connection = server.getConnection()) {
            log("Reading resource");
            final ResourceResponse before = connection.read(ctx(), Requests.newReadRequest("users/1"));
            log("Resource read and has revision " + before.getRevision());
            log("Updating resource");
            final ResourceResponse after =
                    connection.update(ctx(), Requests.newUpdateRequest("users/1",
                            userAliceWithIdAndRev(1, 1)));
            log("Updated resource now has revision " + after.getRevision());
        }
    }

}
