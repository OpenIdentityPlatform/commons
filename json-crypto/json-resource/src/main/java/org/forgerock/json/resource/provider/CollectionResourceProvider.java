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

package org.forgerock.json.resource.provider;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;

/**
 * An implementation interface for resource providers which exposes a collection
 * of resource instances. The resource collection supports the following
 * operations:
 * <ul>
 * <li>action
 * <li>create - with an optional client provided resource ID as part of the
 * request itself
 * <li>query
 * </ul>
 * Whereas resource instances within the collection support the following
 * operations:
 * <ul>
 * <li>action
 * <li>delete
 * <li>patch
 * <li>read
 * <li>update
 * </ul>
 */
public interface CollectionResourceProvider {

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(ServerContext, ActionRequest, ResultHandler)
     * action} against the resource collection.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleAction(ServerContext, ActionRequest,
     *      ResultHandler)
     */
    void actionCollection(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler);

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(ServerContext, ActionRequest, ResultHandler)
     * action} against a resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The action request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleAction(ServerContext, ActionRequest,
     *      ResultHandler)
     */
    void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler);

    /**
     * {@link RequestHandler#handleCreate(ServerContext, CreateRequest, ResultHandler)
     * Adds} a new resource instance to the collection.
     * <p>
     * Create requests are targeted at the collection itself and may include a
     * user-provided resource ID for the new resource as part of the request
     * itself. The user-provider resource ID may be accessed using the method
     * {@link CreateRequest#getNewResourceId()}.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The create request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleCreate(ServerContext, CreateRequest,
     *      ResultHandler)
     * @see CreateRequest#getNewResourceId()
     */
    void createInstance(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handleDelete(ServerContext, DeleteRequest, ResultHandler)
     * Removes} a resource instance from the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The delete request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleDelete(ServerContext, DeleteRequest,
     *      ResultHandler)
     */
    void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handlePatch(ServerContext, PatchRequest, ResultHandler)
     * Patches} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The patch request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handlePatch(ServerContext, PatchRequest,
     *      ResultHandler)
     */
    void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handleQuery(ServerContext, QueryRequest, QueryResultHandler)
     * Searches} the collection for all resources which match the query request
     * criteria.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The query request.
     * @param handler
     *            The query result handler to be notified on completion.
     * @see RequestHandler#handleQuery(ServerContext, QueryRequest,
     *      QueryResultHandler)
     */
    void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler);

    /**
     * {@link RequestHandler#handleRead(ServerContext, ReadRequest, ResultHandler)
     * Reads} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The read request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleRead(ServerContext, ReadRequest, ResultHandler)
     */
    void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler);

    /**
     * {@link RequestHandler#handleUpdate(ServerContext, UpdateRequest, ResultHandler)
     * Updates} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The update request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see RequestHandler#handleUpdate(ServerContext, UpdateRequest,
     *      ResultHandler)
     */
    void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler);

}
