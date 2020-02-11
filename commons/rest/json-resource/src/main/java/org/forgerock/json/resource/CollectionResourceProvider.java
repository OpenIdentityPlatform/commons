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

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

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
 * <p>
 * <b>NOTE:</b> field filtering alters the structure of a JSON resource and MUST
 * only be performed once while processing a request. It is therefore the
 * responsibility of front-end implementations (e.g. HTTP listeners, Servlets,
 * etc) to perform field filtering. Request handler and resource provider
 * implementations SHOULD NOT filter fields, but MAY choose to optimise their
 * processing in order to return a resource containing only the fields targeted
 * by the field filters.
 */
public interface CollectionResourceProvider {

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(Context, ActionRequest) action}
     * against the resource collection.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleAction(Context, ActionRequest)
     */
    Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request);

    /**
     * Performs the provided
     * {@link RequestHandler#handleAction(Context, ActionRequest)
     * action} against a resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleAction(Context, ActionRequest)
     */
    Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request);

    /**
     * {@link RequestHandler#handleCreate(Context, CreateRequest)
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
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleCreate(Context, CreateRequest)
     * @see CreateRequest#getNewResourceId()
     */
    Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request);

    /**
     * {@link RequestHandler#handleDelete(Context, DeleteRequest)
     * Removes} a resource instance from the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The delete request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleDelete(Context, DeleteRequest)
     */
    Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request);

    /**
     * {@link RequestHandler#handlePatch(Context, PatchRequest)
     * Patches} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The patch request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handlePatch(Context, PatchRequest)
     */
    Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request);

    /**
     * {@link RequestHandler#handleQuery(Context, QueryRequest, QueryResourceHandler)
     * Searches} the collection for all resources which match the query request
     * criteria.
     * <p>
     * Implementations must invoke
     * {@link QueryResourceHandler#handleResource(ResourceResponse)} for each resource
     * which matches the query criteria. Once all matching resources have been
     * returned implementations are required to return either a
     * {@link QueryResponse} if the query has completed successfully, or
     * {@link ResourceException} if the query did not complete successfully
     * (even if some matching resources were returned).
     *
     * @param context
     *            The request server context.
     * @param request
     *            The query request.
     * @param handler
     *            The query resource handler to be notified for each matching
     *            resource.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleQuery(Context, QueryRequest, QueryResourceHandler)
     */
    Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler);

    /**
     * {@link RequestHandler#handleRead(Context, ReadRequest)
     * Reads} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The read request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleRead(Context, ReadRequest)
     */
    Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request);

    /**
     * {@link RequestHandler#handleUpdate(Context, UpdateRequest)
     * Updates} an existing resource within the collection.
     *
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The update request.
     * @return A {@code Promise} containing the result of the operation.
     * @see RequestHandler#handleUpdate(Context, UpdateRequest)
     */
    Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request);
}
