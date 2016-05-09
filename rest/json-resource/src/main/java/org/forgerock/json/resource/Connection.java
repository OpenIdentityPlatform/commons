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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.io.Closeable;
import java.util.Collection;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A client connection to a JSON resource provider over which read and update
 * requests may be performed.
 */
public interface Connection extends Closeable {

    /**
     * Performs an action against a specific resource, or set of resources. Bulk
     * updates are an example of an action request.
     *
     * @param context
     *            The request context, such as associated principal.
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
    ActionResponse action(Context context, ActionRequest request) throws ResourceException;

    /**
     * Asynchronously performs an action against a specific resource, or set of
     * resources. Bulk updates are an example of an action request.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The action request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support action requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ActionResponse, ResourceException> actionAsync(Context context, ActionRequest request);

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
     * @param context
     *            The request context, such as associated principal.
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
    ResourceResponse create(Context context, CreateRequest request) throws ResourceException;

    /**
     * Asynchronously adds a new JSON resource.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The create request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support create requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ResourceResponse, ResourceException> createAsync(Context context, CreateRequest request);

    /**
     * Deletes a JSON resource.
     *
     * @param context
     *            The request context, such as associated principal.
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
    ResourceResponse delete(Context context, DeleteRequest request) throws ResourceException;

    /**
     * Asynchronously deletes a JSON resource.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The delete request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support delete requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ResourceResponse, ResourceException> deleteAsync(Context context, DeleteRequest request);

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
     * @param context
     *            The request context, such as associated principal.
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
    ResourceResponse patch(Context context, PatchRequest request) throws ResourceException;

    /**
     * Asynchronously updates a JSON resource by applying a set of changes to
     * its existing content.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The patch request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support patch requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ResourceResponse, ResourceException> patchAsync(Context context, PatchRequest request);

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, and returns a {@code Promise} that will be completed with the
     * results of the search.
     * <p>
     * Result processing <b><i>happens-before</i></b> this method returns to the
     * caller.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The query request.
     * @param handler
     *            A query resource handler which can be used to process
     *            matching resources as they are received.
     * @return The query result.
     * @throws ResourceException
     *             If the query could not be performed.
     * @throws UnsupportedOperationException
     *             If this connection does not support query requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    QueryResponse query(Context context, QueryRequest request, QueryResourceHandler handler)
            throws ResourceException;

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, and places the results in the provided collection.
     *
     * @param context
     *            The request context, such as associated principal.
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
    QueryResponse query(Context context, QueryRequest request, Collection<? super ResourceResponse> results)
            throws ResourceException;

    /**
     * Asynchronously searches for all JSON resources matching a user specified
     * set of criteria, and returns a {@code Promise} that will be completed with the
     * results of the search.
     * <p>
     * Result processing <b><i>happens-before</i></b> the returned future
     * completes.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The create request.
     * @param handler
     *            A non-{@code null} query resource handler which should be
     *            used to process matching resources as they are received.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support query requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<QueryResponse, ResourceException> queryAsync(Context context, QueryRequest request,
            QueryResourceHandler handler);

    /**
     * Reads a JSON resource.
     *
     * @param context
     *            The request context, such as associated principal.
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
    ResourceResponse read(Context context, ReadRequest request) throws ResourceException;

    /**
     * Asynchronously reads a JSON resource.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The read request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support read requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ResourceResponse, ResourceException> readAsync(Context context, ReadRequest request);

    /**
     * Updates a JSON resource by replacing its existing content with new
     * content.
     *
     * @param context
     *            The request context, such as associated principal.
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
    ResourceResponse update(Context context, UpdateRequest request) throws ResourceException;

    /**
     * Asynchronously updates a JSON resource by replacing its existing content
     * with new content.
     *
     * @param context
     *            The request context, such as associated principal.
     * @param request
     *            The update request.
     * @return A future representing the result of the request.
     * @throws UnsupportedOperationException
     *             If this connection does not support update requests.
     * @throws IllegalStateException
     *             If this connection has already been closed, i.e. if
     *             {@code isClosed() == true}.
     */
    Promise<ResourceResponse, ResourceException> updateAsync(Context context, UpdateRequest request);
}
