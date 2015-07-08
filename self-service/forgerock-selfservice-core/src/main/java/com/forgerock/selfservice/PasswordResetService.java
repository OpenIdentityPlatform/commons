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
package com.forgerock.selfservice;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Password reset service is responsible for handling the flow which eventually leads to a users password being reset.
 *
 * @since 0.1.0
 */
public class PasswordResetService implements RequestHandler {

    private static final String SUBMIT_ACTION = "submitRequirements";

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        if (SUBMIT_ACTION.equals(request.getAction())) {
            handler.handleResult(handleNextStage());
            return;
        }

        handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
    }

    private JsonValue handleNextStage() {
        return JsonValue.json(JsonValue.object());
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

}
