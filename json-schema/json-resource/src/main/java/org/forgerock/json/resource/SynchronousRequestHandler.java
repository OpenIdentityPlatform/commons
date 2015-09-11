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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.Collection;

import org.forgerock.services.context.Context;

/**
 * An interface for implementing synchronous {@link RequestHandler}s. A
 * synchronous request handler will block the caller until an operation has
 * completed which may impact scalability in many environments, such as
 * application servers. Therefore it is recommended that request handlers which
 * are intended for use in production environments implement the asynchronous
 * {@link RequestHandler} interface. This interface can be easily "mocked" and
 * is therefore suitable for use in unit tests.
 * <p>
 * A synchronous request handler can be adapted as a {@link RequestHandler}
 * using the {@link Resources#asRequestHandler(SynchronousRequestHandler)}
 * method.
 * <p>
 * For more documentation about the individual operations see
 * {@link RequestHandler}.
 *
 * @see RequestHandler
 * @see Resources#asRequestHandler(SynchronousRequestHandler)
 */
public interface SynchronousRequestHandler {

    /**
     * Handles performing an action on a resource, and optionally returns an
     * associated result. The execution of an action is allowed to incur side
     * effects.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The action request.
     * @return The possibly {@code null} result of the action.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleAction(Context, ActionRequest)
     */
    ActionResponse handleAction(Context context, ActionRequest request) throws ResourceException;

    /**
     * Adds a new JSON resource.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The create request.
     * @return The new resource.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleCreate(Context, CreateRequest)
     */
    ResourceResponse handleCreate(Context context, CreateRequest request) throws ResourceException;

    /**
     * Deletes a JSON resource.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The delete request.
     * @return The deleted resource.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleDelete(Context, DeleteRequest)
     */
    ResourceResponse handleDelete(Context context, DeleteRequest request) throws ResourceException;

    /**
     * Updates a JSON resource by applying a set of changes to its existing
     * content.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The patch request.
     * @return The patched resource.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handlePatch(Context, PatchRequest)
     */
    ResourceResponse handlePatch(Context context, PatchRequest request) throws ResourceException;

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The query request.
     * @param resources
     *            A non-{@code null} collection into which matching resources
     *            should be put.
     * @return The query result.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleQuery(Context, QueryRequest, QueryResourceHandler)
     */
    QueryResponse handleQuery(Context context, QueryRequest request,
            Collection<ResourceResponse> resources) throws ResourceException;

    /**
     * Reads a JSON resource.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The read request.
     * @return The resource.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleRead(Context, ReadRequest)
     */
    ResourceResponse handleRead(Context context, ReadRequest request) throws ResourceException;

    /**
     * Updates a JSON resource by replacing its existing content with new
     * content.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The update request.
     * @return The updated resource.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @see RequestHandler#handleUpdate(Context, UpdateRequest)
     */
    ResourceResponse handleUpdate(Context context, UpdateRequest request) throws ResourceException;
}
