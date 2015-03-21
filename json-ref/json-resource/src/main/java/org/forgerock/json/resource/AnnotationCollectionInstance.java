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

import static org.forgerock.json.resource.RequestHandlerUtils.handle;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.annotations.Delete;
import org.forgerock.json.resource.annotations.Patch;
import org.forgerock.json.resource.annotations.Read;
import org.forgerock.json.resource.annotations.Update;

/**
 * Exposes an annotated POJO as collection instance methods {@link org.forgerock.json.resource.RequestHandler} by
 * looking for annotated and/or conventionally-named methods (as per
 * {@link org.forgerock.json.resource.annotations.RequestHandler}).
 * <p>
 * This class will handle the requests to the collection's instance-level endpoint, so only Read, Update, Delete,
 * Patch and Action are implemented - the remaining methods delegate to the {@link InterfaceCollectionInstance} for
 * reporting the erroneous request to the caller.
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotationCollectionInstance extends InterfaceCollectionInstance {

    private final AnnotatedMethod readMethod;
    private final AnnotatedMethod updateMethod;
    private final AnnotatedMethod deleteMethod;
    private final AnnotatedMethod patchMethod;
    private final AnnotatedActionMethods actionMethods;

    public AnnotationCollectionInstance(Object requestHandler) {
        super(null);
        this.readMethod = AnnotatedMethod.findMethod(requestHandler, Read.class, true);
        this.updateMethod = AnnotatedMethod.findMethod(requestHandler, Update.class, true);
        this.deleteMethod = AnnotatedMethod.findMethod(requestHandler, Delete.class, true);
        this.patchMethod = AnnotatedMethod.findMethod(requestHandler, Patch.class, true);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, true);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        RequestHandlerUtils.handle(readMethod, context, request, Resources.idOf(context), handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        RequestHandlerUtils.handle(updateMethod, context, request, Resources.idOf(context), handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        RequestHandlerUtils.handle(deleteMethod, context, request, Resources.idOf(context), handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        RequestHandlerUtils.handle(patchMethod, context, request, Resources.idOf(context), handler);
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RequestHandlerUtils.handle(actionMethods.<JsonValue>invoke(context, request, Resources.idOf(context)), handler);
    }

}
