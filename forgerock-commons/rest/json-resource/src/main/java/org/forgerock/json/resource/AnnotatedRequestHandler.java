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

package org.forgerock.json.resource;

import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as an instance {@link org.forgerock.json.resource.RequestHandler} by looking for annotated
 * and/or conventionally-named methods (as per {@link RequestHandler}).
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotatedRequestHandler implements org.forgerock.json.resource.RequestHandler,
        Describable<ApiDescription, Request> {

    private final AnnotatedMethod createMethod;
    private final AnnotatedMethod readMethod;
    private final AnnotatedMethod updateMethod;
    private final AnnotatedMethod deleteMethod;
    private final AnnotatedMethod patchMethod;
    private final AnnotatedMethod queryMethod;
    private final AnnotatedActionMethods actionMethods;
    private final Describable<ApiDescription, Request> describable;

    AnnotatedRequestHandler(Object requestHandler) {
        if (!requestHandler.getClass().isAnnotationPresent(RequestHandler.class)) {
            throw new IllegalArgumentException("RequestHandler missing from class: "
                    + requestHandler.getClass().getName());
        }
        this.createMethod = AnnotatedMethod.findMethod(requestHandler, Create.class, false);
        this.readMethod = AnnotatedMethod.findMethod(requestHandler, Read.class, false);
        this.updateMethod = AnnotatedMethod.findMethod(requestHandler, Update.class, false);
        this.deleteMethod = AnnotatedMethod.findMethod(requestHandler, Delete.class, false);
        this.patchMethod = AnnotatedMethod.findMethod(requestHandler, Patch.class, false);
        this.queryMethod = AnnotatedMethod.findMethod(requestHandler, Query.class, false);
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
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return queryMethod.invoke(context, request, handler);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return actionMethods.invoke(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return createMethod.invoke(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return deleteMethod.invoke(context, request);
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
