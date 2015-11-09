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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.selfservice.core;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newPatchRequest;

import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.crypto.CryptoConstants;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.crypto.JsonCryptoException;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A RequestHandler that proxies user requests to update the user's KBA answers.
 *
 * @since 0.8.0
 */
public final class UserUpdateService implements RequestHandler {
    private static final JsonPointer KBAINFO = new JsonPointer("/kbaInfo");

    private final CryptoService cryptoService;
    private final ConnectionFactory connectionFactory;
    private final ResourcePath identityService;

    /**
     * Construct a service to update the user's KBA info.
     *
     * @param connectionFactory a ConnectionFactory with access to the <em>identityService</em> route.
     * @param identityService the route to the identity service used to patch the user
     */
    @Inject
    public UserUpdateService(@SelfService ConnectionFactory connectionFactory, ResourcePath identityService) {
        this.connectionFactory = connectionFactory;
        this.cryptoService = new CryptoService();
        this.identityService = identityService;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest actionRequest) {
        return new NotSupportedException("Action not supported for this endpoint").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest createRequest) {
        return new NotSupportedException("Create not supported for this endpoint").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest deleteRequest) {
        return new NotSupportedException("Delete not supported for this endpoint").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest patchRequest) {
        if (patchRequest.getPatchOperations().isEmpty() || patchRequest.getPatchOperations().size() > 1) {
            return new BadRequestException("Patch expects one operation").asPromise();
        }
        PatchOperation patch = patchRequest.getPatchOperations().get(0);
        if (!PatchOperation.OPERATION_REPLACE.equals(patch.getOperation())
                || !KBAINFO.equals(patch.getField())
                || !patch.getValue().isList()) {
            return new BadRequestException("Patch operation must replace " + KBAINFO).asPromise();
        }

        try {
            final JsonValue hashedAnswers = json(array());
            for (JsonValue value : patch.getValue()) {
                final String questionId = value.get("questionId").asString();
                final JsonValue answer = value.get("answer");
                final JsonValue hashedAnswer = cryptoService.isHashed(answer)
                        ? answer
                        : cryptoService.hash(answer.asString(), CryptoConstants.ALGORITHM_SHA_256);
                hashedAnswers.add(object(
                                field("questionId", questionId),
                                field("answer", hashedAnswer.getObject())));
            }

            return connectionFactory.getConnection().patch(
                    // do NOT wrap context in SelfServiceContext for this call -- it is authenticated as the user
                    // performing the patch/update
                    context,
                    newPatchRequest(identityService.child(patchRequest.getResourcePath()),
                            PatchOperation.replace(KBAINFO, hashedAnswers.getObject())))
                    .asPromise();
        } catch (JsonCryptoException e) {
            return new InternalServerErrorException("Error while hashing the answer", e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return new NotSupportedException("Query not supported for this endpoint").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        return new NotSupportedException("Read not supported for this endpoint").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest updateRequest) {
        return new NotSupportedException("Update not supported for this endpoint").asPromise();
    }
}
