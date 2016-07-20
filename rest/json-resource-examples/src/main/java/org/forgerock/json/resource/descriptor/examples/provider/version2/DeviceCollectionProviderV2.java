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

package org.forgerock.json.resource.descriptor.examples.provider.version2;

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UncategorizedException;
import org.forgerock.json.resource.descriptor.examples.model.Device;
import org.forgerock.json.resource.descriptor.examples.provider.version1.DeviceCollectionProviderV1;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Example device collection provider with API descriptor annotations.
 */
@CollectionProvider(details = @Handler(
        id = "devices:2.0",
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
public class DeviceCollectionProviderV2 extends DeviceCollectionProviderV1 {

    /**
     * Default constructor.
     * @param memoryBackend The base resource provider
     */
    public DeviceCollectionProviderV2(MemoryBackend memoryBackend) {
        super(memoryBackend);
    }

    /**
     * Performs a roll out device action operation on the resource.
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
            description = LocalizableString.TRANSLATION_KEY_PREFIX
                    + "api-dictionary-example#device_action_rollout_operation_description",
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
                    description = "Action `rollOut` reached. As it is an example "
                            + "service it has not been implemented.")}),
            name = "rollOut")
    public Promise<ActionResponse, ResourceException> rollOutDevice(Context context, String id, ActionRequest request) {
        final ResourceException e =
                new UncategorizedException(501, "Action `rollOut` reached. "
                        + "As it is an example service it has not been implemented.", null);
        return newExceptionPromise(e);
    }

}
