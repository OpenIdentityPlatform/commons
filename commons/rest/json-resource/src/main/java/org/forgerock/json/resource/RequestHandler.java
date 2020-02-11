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

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Represents the contract with a set of resources.
 * <p>
 * The structure and depth of the (potentially nested) resource set it deals
 * with is up to the implementation. It can choose to just deal with one level,
 * and hand off to another resource implementation, or it can choose to handle
 * multiple levels.
 * <p>
 * As an example, taking an id of level1/level2/leaf, and assuming that the
 * resource implementation registers to handle level1 with the router; the
 * implementation could choose to hand off processing to other implementations
 * for level2 (or leaf) or it could handle all operations down to leaf.
 * <p>
 * Supports either synchronous or asynchronous internal processing, i.e. it does
 * not have to block the request thread until a response becomes available.
 * <p>
 * For synchronous internal processing, immediately return a completed
 * {@code Promise}, e.g.
 *
 * <pre>
 * return Promises.newResultPromise(result);
 * </pre>
 *
 * <p>
 * <b>NOTE:</b> field filtering alters the structure of a JSON resource and MUST
 * only be performed once while processing a request. It is therefore the
 * responsibility of front-end implementations (e.g. HTTP listeners, Servlets,
 * etc) to perform field filtering. Request handler and resource provider
 * implementations SHOULD NOT filter fields, but MAY choose to optimise their
 * processing in order to return a resource containing only the fields targeted
 * by the field filters.
 */
public interface RequestHandler {

    /**
     * Handles performing an action on a resource, and optionally returns an
     * associated result. The execution of an action is allowed to incur side
     * effects.
     * <p>
     * Actions are parametric; a set of named parameters is provided as input to
     * the action. The action result is a JSON object structure composed of
     * basic Java types; its overall structure is defined by a specific
     * implementation.
     * <p>
     * On completion, the action result (or null) must be used to complete the
     * returned {@code Promise}. On failure, the returned {@code Promise} must
     * be completed with the exception.
     * <p>
     * Action expects failure exceptions as follows: {@code ForbiddenException}
     * if access to the resource is forbidden. {@code NotSupportedException} if
     * the requested functionality is not implemented/supported
     * {@code BadRequestException} if the passed identifier, parameters or
     * filter is invalid {@code NotFoundException} if the specified resource
     * could not be found.
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request);

    /**
     * Adds a new JSON resource, returning a {@code Promise} that will be
     * completed when the resource has been added.
     * <p>
     * Create expects failure exceptions as follows:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden.
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code PreconditionFailedException} if a resource with the same ID
     * already exists
     * <li>{@code BadRequestException} if the passed identifier or value is
     * invalid
     * <li>{@code NotFoundException} if the specified id could not be resolved,
     * for example when an intermediate resource in the hierarchy does not
     * exist.
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The create request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request);

    /**
     * Deletes a JSON resource, returning a {@code Promise} that will be
     * completed when the resource has been deleted.
     * <p>
     * Delete expects failure exceptions as follows:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code BadRequestException} if the passed identifier is invalid
     * <li>{@code NotFoundException} if the specified resource could not be
     * found
     * <li>{@code PreconditionRequiredException} if version is required, but is
     * {@code null}
     * <li>{@code PreconditionFailedException} if version did not match the
     * existing resource.
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The delete request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request);

    /**
     * Updates a JSON resource by applying a set of changes to its existing
     * content, returning a {@code Promise} that will be completed when the
     * resource has been updated.
     * <p>
     * Patch expects failure exceptions as follows:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code PreconditionRequiredException} if version is required, but is
     * {@code null}
     * <li>{@code PreconditionFailedException} if version did not match the
     * existing resource
     * <li>{@code BadRequestException} if the passed identifier or filter is
     * invalid
     * <li>{@code NotFoundException} if the specified resource could not be
     * found
     * <li>{@code ConflictException} if patch could not be applied for the given
     * resource state.
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The patch request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request);

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, returning a {@code Promise} that will be completed when the
     * search has completed.
     * <p>
     * Implementations must invoke
     * {@link QueryResourceHandler#handleResource(ResourceResponse)} for each resource
     * which matches the query criteria. Once all matching resources have been
     * returned implementations are required to return either a
     * {@link QueryResponse} if the query has completed successfully, or
     * {@link ResourceException} if the query did not complete successfully
     * (even if some matching resources were returned).
     * <p>
     * Query expects failure exceptions as follows:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code BadRequestException} if the passed identifier, parameters or
     * filter is invalid
     * <li>{@code NotFoundException} if the specified resource could not be
     * found
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The query request.
     * @param handler
     *            The query resource handler to be notified for each matching
     *            resource.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler);

    /**
     * Reads a JSON resource, returning a {@code Promise} that will be
     * completed when the resource has been read.
     * <p>
     * Read expects failure exceptions as follows:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden.
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code BadRequestException} if the passed identifier or filter is
     * invalid
     * <li>{@code NotFoundException} if the specified resource could not be
     * found.
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The read request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request);

    /**
     * Updates a JSON resource by replacing its existing content with new
     * content, returning a {@code Promise} that will be completed when the
     * resource has been updated.
     * <p>
     * Update expects failure the following failure exceptions:
     * <ul>
     * <li>{@code ForbiddenException} if access to the resource is forbidden
     * <li>{@code NotSupportedException} if the requested functionality is not
     * implemented/supported
     * <li>{@code PreconditionRequiredException} if version is required, but is
     * {@code null}
     * <li>{@code PreconditionFailedException} if version did not match the
     * existing resource
     * <li>{@code BadRequestException} if the passed identifier or filter is
     * invalid
     * <li>{@code NotFoundException} if the specified resource could not be
     * found.
     * </ul>
     *
     * @param context
     *            The request server context, such as associated principal.
     * @param request
     *            The update request.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request);
}
