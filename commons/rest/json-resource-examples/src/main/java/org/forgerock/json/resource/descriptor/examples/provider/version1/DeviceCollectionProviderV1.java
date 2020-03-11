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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.resource.descriptor.examples.provider.version1;

import static org.forgerock.util.promise.Promises.*;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.QueryType;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UncategorizedException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.descriptor.examples.model.Device;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Example device collection provider with API descriptor annotations.
 */
@CollectionProvider(details = @Handler(
        id = "devices:1.0",
        title = "User devices",
        description = "Devices 1.0 example service has the CQ operations on the collection and CRUDPA operations "
                + "on the items where the Action is to mark the device as stolen. This service is the subresource "
                + "of the Users v1.0 items.",
        resourceSchema = @Schema(fromType = Device.class),
        mvccSupported = true,
        parameters = {
                @Parameter(name = "userId",
                        type = "string",
                        description = "The uid of the User record, the parent of the device")
        }), pathParam = @Parameter(name = "deviceId", type = "string", description = "The device ID from the path"))
public class DeviceCollectionProviderV1 {

    /**
     * The used provider base.
     */
    protected MemoryBackend memoryBackend;

    /**
     * Default constructor.
     * @param memoryBackend the in memory provider to be used
     */
    public DeviceCollectionProviderV1(MemoryBackend memoryBackend) {
        this.memoryBackend = memoryBackend;
    }

    /**
     * Performs a query operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @param handler
     *            The action handler.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(Context, ActionRequest)
     */
    @Query(operationDescription =
        @Operation(
            description = LocalizableString.TRANSLATION_KEY_PREFIX
                    + "api-dictionary-example#device_query_operation_description",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication")}),
            type = QueryType.FILTER,
            countPolicies = {CountPolicy.NONE},
            pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
            queryableFields = {"id", "name", "type", "rollOutDate", "stolen"})
    public Promise<QueryResponse, ResourceException> query(Context context, QueryRequest request,
                                                           QueryResourceHandler handler) {
        return memoryBackend.queryCollection(context, request, handler);
    }

    /**
     * Performs a create operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(Context, ActionRequest)
     */
    @Create(operationDescription =
        @Operation(
            description = "Create new device",
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication")}))
    public Promise<ResourceResponse, ResourceException> create(Context context, CreateRequest request) {
        return memoryBackend.createInstance(context, request);
    }

    /**
     * Performs a read operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param id
     *            The request id.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(Context, ActionRequest)
     */
    @Read(operationDescription =
        @Operation(
            description = "Read one device",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication"),
                @ApiError(
                    code = 404,
                    description = "Device not found")}))
    public Promise<ResourceResponse, ResourceException> read(Context context, String id, ReadRequest request) {
        return memoryBackend.readInstance(context, id, request);
    }

    /**
     * Performs an update operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param id
     *            The request id.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(Context, ActionRequest)
     */
    @Update(operationDescription =
        @Operation(
            description = "Update a device",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication")}))
    public Promise<ResourceResponse, ResourceException> update(Context context, String id, UpdateRequest request) {
        return memoryBackend.updateInstance(context, id, request);
    }

    /**
     * Performs a delete operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param id
     *            The action id.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleDelete(Context, DeleteRequest)
     */
    @Delete(operationDescription =
        @Operation(
            description = "Delete a device",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication")}))
    public Promise<ResourceResponse, ResourceException> delete(Context context, String id, DeleteRequest request) {
        return memoryBackend.deleteInstance(context, id, request);
    }

    /**
     * Performs a patch operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param id
     *            The request id.
     * @param request
     *            The patch request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handlePatch(Context, PatchRequest)
     */
    @Patch(operationDescription =
        @Operation(
            description = "Patch a device",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication")}))
    public Promise<ResourceResponse, ResourceException> patch(Context context, String id, PatchRequest request) {
        return memoryBackend.patchInstance(context, id, request);
    }

    /**
     * Performs a mark as stolen action operation on the resource.
     *
     * @param context
     *            The request server context.
     * @param id
     *            The action id.
     * @param request
     *            The action request.
     * @return A {@code Promise} containing the result of the operation.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(Context, ActionRequest)
     */
    @Action(operationDescription =
        @Operation(
            description = "Mark device as stolen",
            locales = {"en-GB", "en-US"},
            errors = {
                @ApiError(
                    id = "badRequest",
                    code = 400,
                    description = "Indicates that the request could not be understood by "
                            + "the resource due to malformed syntax."),
                @ApiError(
                    id = "unauthorized",
                    code = 401,
                    description = "Unauthorized - Missing or bad authentication"),
                @ApiError(
                    code = 501,
                    description = "Action `markAsStolen` reached. As it is an example "
                            + "service it has not been implemented.")}),
        name = "markAsStolen")
    public Promise<ActionResponse, ResourceException> markStolen(Context context, String id, ActionRequest request) {
        final ResourceException e =
                new UncategorizedException(501, "Action `markAsStolen` reached. As it is an example service it has "
                        + "not been implemented.", null);
        return newExceptionPromise(e);
    }

}
