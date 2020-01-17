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
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as collection methods {@link org.forgerock.json.resource.RequestHandler} by
 * looking for annotated and/or conventionally-named methods (as per {@link CollectionProvider}).
 * <p>
 * This class will handle the requests to the collection-level endpoint, so only Create, Query and Action
 * are implemented - the remaining methods delegate to the {@link InterfaceCollectionHandler} for
 * reporting the erroneous request to the caller.
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotatedCollectionHandler extends InterfaceCollectionHandler implements Describable<ApiDescription, Request> {

    private final AnnotatedMethod createMethod;
    private final AnnotatedMethod queryMethod;
    private final AnnotatedActionMethods actionMethods;
    private final Describable<ApiDescription, Request> describable;

    public AnnotatedCollectionHandler(Object requestHandler) {
        super(null);
        if (!requestHandler.getClass().isAnnotationPresent(CollectionProvider.class)) {
            throw new IllegalArgumentException("CollectionProvider missing from class: "
                    + requestHandler.getClass().getName());
        }
        this.createMethod = AnnotatedMethod.findMethod(requestHandler, Create.class, false);
        this.queryMethod = AnnotatedMethod.findMethod(requestHandler, Query.class, false);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, false);
        this.describable = requestHandler instanceof Describable
                ? (Describable<ApiDescription, Request>) requestHandler
                : null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return createMethod.invoke(context, request);
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
    public void addDescriptorListener(Describable.Listener listener) {
        if (describable != null) {
            describable.addDescriptorListener(listener);
        }
    }

    @Override
    public void removeDescriptorListener(Describable.Listener listener) {
        if (describable != null) {
            describable.removeDescriptorListener(listener);
        }
    }
}
