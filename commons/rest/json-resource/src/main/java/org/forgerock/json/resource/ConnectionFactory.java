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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.io.Closeable;

import org.forgerock.util.promise.Promise;

/**
 * A connection factory provides an interface for obtaining a connection to a
 * JSON resource provider. Connection factories can be used to wrap other
 * connection factories in order to provide enhanced capabilities in a manner
 * which is transparent to the application. For example:
 * <ul>
 * <li>Connection pooling
 * <li>Load balancing
 * <li>Keep alive
 * <li>Logging connections
 * <li>Read-only connections
 * </ul>
 * An application typically obtains a connection from a connection factory,
 * performs one or more operations, and then closes the connection. Applications
 * should aim to close connections as soon as possible in order to avoid
 * resource contention.
 */
public interface ConnectionFactory extends Closeable {

    /**
     * Releases any resources associated with this connection factory. Depending
     * on the implementation a factory may:
     * <ul>
     * <li>do nothing
     * <li>close underlying connection factories (e.g. load-balancers)
     * <li>close pooled connections (e.g. connection pools)
     * <li>shutdown IO event service and related thread pools (e.g. Grizzly).
     * </ul>
     * Calling {@code close} on a connection factory which is already closed has
     * no effect.
     * <p>
     * Applications should avoid closing connection factories while there are
     * remaining active connections in use or connection attempts in progress.
     *
     * @see Resources#uncloseable(ConnectionFactory)
     */
    @Override
    void close();

    /**
     * Returns a connection to the JSON resource provider associated with this
     * connection factory. The connection returned by this method can be used
     * immediately.
     *
     * @return A connection to the JSON resource provider associated with this
     *         connection factory.
     * @throws ResourceException
     *             If the connection request failed for some reason.
     */
    Connection getConnection() throws ResourceException;

    /**
     * Asynchronously obtains a connection to the JSON resource provider
     * associated with this connection factory. The returned
     * {@code FutureResult} can be used to retrieve the completed connection.
     *
     * @return A future which can be used to retrieve the connection.
     */
    Promise<Connection, ResourceException> getConnectionAsync();
}
