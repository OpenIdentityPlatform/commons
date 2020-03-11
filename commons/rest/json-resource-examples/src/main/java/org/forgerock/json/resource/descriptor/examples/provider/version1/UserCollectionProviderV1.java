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
import org.forgerock.api.annotations.Path;
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
import org.forgerock.json.resource.descriptor.examples.model.User;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Example collection provider class with API descriptor annotations.
 */
@CollectionProvider(details = @Handler(
        id = "users:1.0",
        title = "Users",
        description = "This example version 1.0 user service represents a Users resource with CQ operations "
                + "on the users collection and CRUDPA operations available for the user item. "
                + "Action is to reset the password. "
                + "Items can have devices version 1.0 subresources. "
                + "This service is populated by the /users endpoint and the /admins endpoint",
        resourceSchema = @Schema(fromType = User.class),
        mvccSupported = true),
        pathParam = @Parameter(name = "userId", type = "string", description = "The user ID from the path"))
public class UserCollectionProviderV1 {

    /**
     * The user device collection provider.
     */
    private final DeviceCollectionProviderV1 deviceCollectionProvider;

    /**
     * The used provider base.
     */
    protected MemoryBackend memoryBackend;

    /**
     * Default constructor.
     * @param memoryBackend The collection provider base
     * @param deviceCollectionProvider The device collection provider
     */
    public UserCollectionProviderV1(MemoryBackend memoryBackend, DeviceCollectionProviderV1 deviceCollectionProvider) {
        this.memoryBackend = memoryBackend;
        this.deviceCollectionProvider = deviceCollectionProvider;
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
                    + "api-dictionary-example#user_query_operation_description",
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
        queryableFields = {"uid", "name", "password"})
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
            description = LocalizableString.TRANSLATION_KEY_PREFIX
                    + "api-dictionary-example#user_create_operation_description",
            locales = {"en-GB", "en-US", "fr"},
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
            description = "User read operation",
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
                    description = "User not found")}))
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
            description = "User update opreation",
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
            description = "User delete opreation",
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
            description = "User patch operation",
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
            description = "Reset user password",
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
                    description = "Action `resetPassword` reached. "
                            + "As it is an example service it has not been implemented.")}),
        name = "resetPassword")
    public Promise<ActionResponse, ResourceException> resetPasswd(Context context, String id, ActionRequest request) {
        final ResourceException e =
                new UncategorizedException(501, "Action `resetPassword` reached. "
                        + "As it is an example service it has not been implemented.", null);
        return newExceptionPromise(e);
    }

    /**
     * User devices provider.
     * @return User devices provider
     */
    @Path("/devices")
    public DeviceCollectionProviderV1 devices() {
        return deviceCollectionProvider;
    }

}
