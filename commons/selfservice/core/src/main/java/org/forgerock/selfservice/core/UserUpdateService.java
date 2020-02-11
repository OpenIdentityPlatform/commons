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
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.util.Answers;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A RequestHandler that proxies user requests to update the user's KBA answers.
 *
 * @since 0.8.0
 */
public final class UserUpdateService implements CollectionResourceProvider {

    private static final String FIELD_QUESTION_ID = "questionId";
    private static final String FIELD_CUSTOM_QUESTION = "customQuestion";
    private static final String FIELD_ANSWER = "answer";

    private final CryptoService cryptoService;
    private final ConnectionFactory connectionFactory;
    private final ResourcePath identityService;
    private final JsonPointer kbaPropertyField;

    /**
     * Construct a service to update the user's KBA info.
     *
     * @param connectionFactory a ConnectionFactory with access to the <em>identityService</em> route.
     * @param identityService the route to the identity service used to patch the user
     * @param kbaPropertyField the pointer where KBA is stored in the user
     */
    @Inject
    public UserUpdateService(@SelfService ConnectionFactory connectionFactory, ResourcePath identityService,
            JsonPointer kbaPropertyField) {
        this.connectionFactory = connectionFactory;
        this.cryptoService = new CryptoService();
        this.identityService = identityService;
        this.kbaPropertyField = kbaPropertyField;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        if (request.getPatchOperations().isEmpty() || request.getPatchOperations().size() > 1) {
            return new BadRequestException("Patch expects one operation").asPromise();
        }
        PatchOperation patch = request.getPatchOperations().get(0);
        if (!PatchOperation.OPERATION_REPLACE.equals(patch.getOperation())
                || !kbaPropertyField.equals(patch.getField())
                || !patch.getValue().isList()) {
            return new BadRequestException("Patch operation must replace " + kbaPropertyField).asPromise();
        }

        try {
            final JsonValue hashedAnswers = json(array());
            for (JsonValue value : patch.getValue()) {
                final JsonValue answer = value.get(FIELD_ANSWER);
                if (answer.isNull()) {
                    throw new BadRequestException("Patch content must contain an " + FIELD_ANSWER);
                }
                final JsonValue hashedAnswer = Answers.hashAnswer(cryptoService, answer);
                if (value.isDefined(FIELD_QUESTION_ID)) {
                    hashedAnswers.add(object(
                            field(FIELD_QUESTION_ID, value.get(FIELD_QUESTION_ID).asString()),
                            field(FIELD_ANSWER, hashedAnswer.getObject())));
                } else if (value.isDefined(FIELD_CUSTOM_QUESTION)) {
                    hashedAnswers.add(object(
                            field(FIELD_CUSTOM_QUESTION, value.get(FIELD_CUSTOM_QUESTION).asString()),
                            field(FIELD_ANSWER, hashedAnswer.getObject())));
                } else {
                    throw new BadRequestException("Patch content must contain either a " + FIELD_QUESTION_ID
                            + " or a " + FIELD_CUSTOM_QUESTION);
                }
            }

            return connectionFactory.getConnection().patch(
                    // do NOT wrap context in SelfServiceContext for this call -- it is authenticated as the user
                    // performing the patch/update
                    context,
                    newPatchRequest(identityService.child(resourceId),
                            PatchOperation.replace(kbaPropertyField, hashedAnswers.getObject())))
                    .asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }
}
