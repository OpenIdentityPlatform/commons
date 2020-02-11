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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Update;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as collection instance methods {@link org.forgerock.json.resource.RequestHandler} by
 * looking for annotated and/or conventionally-named methods (as per {@link CollectionProvider}).
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

    AnnotationCollectionInstance(Object requestHandler) {
        super(null);
        if (!requestHandler.getClass().isAnnotationPresent(CollectionProvider.class)) {
            throw new IllegalArgumentException("CollectionProvider missing from class: "
                    + requestHandler.getClass().getName());
        }
        this.readMethod = AnnotatedMethod.findMethod(requestHandler, Read.class, true);
        this.updateMethod = AnnotatedMethod.findMethod(requestHandler, Update.class, true);
        this.deleteMethod = AnnotatedMethod.findMethod(requestHandler, Delete.class, true);
        this.patchMethod = AnnotatedMethod.findMethod(requestHandler, Patch.class, true);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, true);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return readMethod.invoke(context, request, Resources.idOf(context));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return updateMethod.invoke(context, request, Resources.idOf(context));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return deleteMethod.invoke(context, request, Resources.idOf(context));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return patchMethod.invoke(context, request, Resources.idOf(context));
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return actionMethods.invoke(context, request, Resources.idOf(context));
    }
}
