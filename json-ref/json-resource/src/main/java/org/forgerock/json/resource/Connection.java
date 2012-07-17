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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.io.Closeable;
import java.util.Collection;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A client connection to a JSON resource provider over which read and update
 * requests may be performed.
 */
public interface Connection extends Closeable {

    // TODO: do we need to expose a method for obtaining the endpoints/schema?

    /**
     * Performs an action against a specific resource, or set of resources. Bulk
     * updates are an example of an action request.
     *
     * @param request
     *            The action request.
     * @return A JSON object containing the result of the action, the content of
     *         which is specified by the action.
     * @throws ResourceException
     *             If the action could not be performed.
     * @throws UnsupportedOperationException
     *             If this connection does not support action requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    JsonValue action(ActionRequest request) throws ResourceException;

    /**
     * Asynchronously performs an action against a specific resource, or set of
     * resources. Bulk updates are an example of an action request.
     *
     * @param request
     *            The action request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support action requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<JsonValue> actionAsync(ActionRequest request, ResultHandler<JsonValue> handler);

    /**
     * Releases any resources associated with this connection. For physical
     * connections to a server this will mean that the underlying socket is
     * closed.
     * <p>
     * Other connection implementations may behave differently. For example, a
     * pooled connection will be released and returned to its connection pool.
     * <p>
     * Calling {@code close} on a connection that is already closed has no
     * effect.
     */
    void close();

    /**
     * Adds a new JSON resource.
     *
     * @param request
     *            The create request.
     * @return The newly created JSON resource.
     * @throws ResourceException
     *             If the JSON resource could not be created.
     * @throws UnsupportedOperationException
     *             If this connection does not support create requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Resource create(CreateRequest request) throws ResourceException;

    /**
     * Asynchronously adds a new JSON resource.
     *
     * @param request
     *            The create request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support create requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<Resource> createAsync(CreateRequest request, ResultHandler<Resource> handler);

    /**
     * Deletes a JSON resource.
     *
     * @param request
     *            The delete request.
     * @return The deleted JSON resource.
     * @throws ResourceException
     *             If the JSON resource could not be deleted.
     * @throws UnsupportedOperationException
     *             If this connection does not support delete requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Resource delete(DeleteRequest request) throws ResourceException;

    /**
     * Asynchronously deletes a JSON resource.
     *
     * @param request
     *            The delete request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support delete requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<Resource> deleteAsync(DeleteRequest request, ResultHandler<Resource> handler);

    /**
     * Indicates whether or not this connection has been explicitly closed by
     * calling {@code close}. This method will not return {@code true} if a
     * fatal error has occurred on the connection unless {@code close} has been
     * called.
     *
     * @return {@code true} if this connection has been explicitly closed by
     *         calling {@code close}, or {@code false} otherwise.
     */
    boolean isClosed();

    /**
     * Returns {@code true} if this connection has not been closed and no fatal
     * errors have been detected. This method is guaranteed to return
     * {@code false} only when it is called after the method {@code close} has
     * been called.
     *
     * @return {@code true} if this connection is valid, {@code false}
     *         otherwise.
     */
    boolean isValid();

    /**
     * Updates a JSON resource by applying a set of changes to its existing
     * content.
     *
     * @param request
     *            The update request.
     * @return The updated JSON resource.
     * @throws ResourceException
     *             If the JSON resource could not be updated.
     * @throws UnsupportedOperationException
     *             If this connection does not support patch requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Resource patch(PatchRequest request) throws ResourceException;

    /**
     * Asynchronously updates a JSON resource by applying a set of changes to
     * its existing content.
     *
     * @param request
     *            The patch request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support patch requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<Resource> patchAsync(PatchRequest request, ResultHandler<Resource> handler);

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, and passes the results to the provided result handler.
     * <p>
     * Result processing <b><i>happens-before</i></b> this method returns to the
     * caller.
     *
     * @param request
     *            The query request.
     * @param handler
     *            A result handler which can be used to process matching
     *            resources as they are received.
     * @return The query result.
     * @throws ResourceException
     *             If the query could not be performed.
     * @throws UnsupportedOperationException
     *             If this connection does not support query requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    QueryResult query(QueryRequest request, QueryResultHandler handler) throws ResourceException;

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, and places the results in the provided collection.
     *
     * @param <T>
     *            The type of the collection in which the results should be
     *            placed.
     * @param request
     *            The query request.
     * @param results
     *            A collection into which matching resources will be added as
     *            they are received.
     * @return The query result.
     * @throws ResourceException
     *             If the query could not be performed.
     * @throws UnsupportedOperationException
     *             If this connection does not support query requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    <T extends Collection<? super Resource>> QueryResult query(QueryRequest request, T results)
            throws ResourceException;

    /**
     * Asynchronously searches for all JSON resources matching a user specified
     * set of criteria, and passes the results to the provided result handler.
     * <p>
     * Result processing <b><i>happens-before</i></b> the returned future
     * completes.
     *
     * @param request
     *            The create request.
     * @param handler
     *            A result handler which can be used to process matching
     *            resources as they are received.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support query requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<QueryResult> queryAsync(QueryRequest request, QueryResultHandler handler);

    /**
     * Reads a JSON resource.
     *
     * @param request
     *            The read request.
     * @return The JSON resource.
     * @throws ResourceException
     *             If the JSON resource could not be read.
     * @throws UnsupportedOperationException
     *             If this connection does not support read requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Resource read(ReadRequest request) throws ResourceException;

    /**
     * Asynchronously reads a JSON resource.
     *
     * @param request
     *            The read request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support read requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<Resource> readAsync(ReadRequest request, ResultHandler<Resource> handler);

    /**
     * Updates a JSON resource by replacing its existing content with new
     * content.
     *
     * @param request
     *            The update request.
     * @return The updated JSON resource.
     * @throws ResourceException
     *             If the JSON resource could not be updated.
     * @throws UnsupportedOperationException
     *             If this connection does not support update requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Resource update(UpdateRequest request) throws ResourceException;

    /**
     * Asynchronously updates a JSON resource by replacing its existing content
     * with new content.
     *
     * @param request
     *            The update request.
     * @param handler
     *            A result handler which can be used to asynchronously process
     *            the operation result when it is received, may be {@code null}.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support updaterequests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    FutureResult<Resource> updateAsync(UpdateRequest request, ResultHandler<Resource> handler);
}
