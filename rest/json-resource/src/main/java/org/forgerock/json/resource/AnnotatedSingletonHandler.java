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

import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as an instance {@link org.forgerock.json.resource.RequestHandler} by looking for annotated
 * and/or conventionally-named methods (as per {@link SingletonProvider}).
 * <p>
 * This class handles the requests to singleton endpoints, so only Read, Update, Patch and Action are supported.
 * are implemented - the remaining methods delegate to the {@link InterfaceSingletonHandler} for reporting the
 * erroneous request to the caller.
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotatedSingletonHandler extends InterfaceSingletonHandler implements Describable<ApiDescription, Request> {

    private final AnnotatedMethod readMethod;
    private final AnnotatedMethod updateMethod;
    private final AnnotatedMethod patchMethod;
    private final AnnotatedActionMethods actionMethods;
    private final Describable<ApiDescription, Request> describable;

    public AnnotatedSingletonHandler(Object requestHandler) {
        super(null);
        if (!requestHandler.getClass().isAnnotationPresent(SingletonProvider.class)) {
            throw new IllegalArgumentException("SingletonProvider missing from class: "
                    + requestHandler.getClass().getName());
        }
        this.readMethod = AnnotatedMethod.findMethod(requestHandler, Read.class, false);
        this.updateMethod = AnnotatedMethod.findMethod(requestHandler, Update.class, false);
        this.patchMethod = AnnotatedMethod.findMethod(requestHandler, Patch.class, false);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, false);
        this.describable = requestHandler instanceof Describable
                ? (Describable<ApiDescription, Request>) requestHandler
                : null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return readMethod.invoke(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return updateMethod.invoke(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return patchMethod.invoke(context, request);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return actionMethods.invoke(context, request);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> producer) {
        if (describable == null) {
            throw new UnsupportedOperationException(
                    "The provided request handler does not support API Descriptor methods");
        }
        return describable.api(producer);
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        if (describable == null) {
            throw new UnsupportedOperationException(
                    "The provided request handler does not support API Descriptor methods");
        }
        return describable.handleApiRequest(context, request);
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        if (describable != null) {
            describable.addDescriptorListener(listener);
        }
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        if (describable != null) {
            describable.removeDescriptorListener(listener);
        }
    }
}
