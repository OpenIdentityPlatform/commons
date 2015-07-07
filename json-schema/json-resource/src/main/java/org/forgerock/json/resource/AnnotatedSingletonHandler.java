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

package org.forgerock.json.resource;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.annotations.Patch;
import org.forgerock.json.resource.annotations.Read;
import org.forgerock.json.resource.annotations.RequestHandler;
import org.forgerock.json.resource.annotations.Update;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as an instance {@link org.forgerock.json.resource.RequestHandler} by looking for annotated
 * and/or conventionally-named methods (as per {@link org.forgerock.json.resource.annotations.RequestHandler}).
 * <p>
 * This class handles the requests to singleton endpoints, so only Read, Update, Patch and Action are supported.
 * are implemented - the remaining methods delegate to the {@link InterfaceSingletonHandler} for reporting the
 * erroneous request to the caller.
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotatedSingletonHandler extends InterfaceSingletonHandler {

    private final AnnotatedMethod readMethod;
    private final AnnotatedMethod updateMethod;
    private final AnnotatedMethod patchMethod;
    private final AnnotatedActionMethods actionMethods;

    public AnnotatedSingletonHandler(Object requestHandler) {
        super(null);
        if (!requestHandler.getClass().isAnnotationPresent(RequestHandler.class)) {
            throw new IllegalArgumentException("RequestHandler missing from class: " +
                    requestHandler.getClass().getName());
        }
        this.readMethod = AnnotatedMethod.findMethod(requestHandler, Read.class, false);
        this.updateMethod = AnnotatedMethod.findMethod(requestHandler, Update.class, false);
        this.patchMethod = AnnotatedMethod.findMethod(requestHandler, Patch.class, false);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, false);
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(ServerContext context, ReadRequest request) {
        return RequestHandlerUtils.handle(readMethod, context, request);
    }

    @Override
    public Promise<Resource, ResourceException> handleUpdate(ServerContext context, UpdateRequest request) {
        return RequestHandlerUtils.handle(updateMethod, context, request);
    }

    @Override
    public Promise<Resource, ResourceException> handlePatch(ServerContext context, PatchRequest request) {
        return RequestHandlerUtils.handle(patchMethod, context, request);
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request) {
        return actionMethods.invoke(context, request, null);
    }

}
