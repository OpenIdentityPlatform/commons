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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collection;
import java.util.LinkedList;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Implementation class for {@link Resources#asRequestHandler}.
 */
class SynchronousRequestHandlerAdapter implements RequestHandler {
    private final SynchronousRequestHandler syncHandler;

    SynchronousRequestHandlerAdapter(final SynchronousRequestHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        try {
            return newResultPromise(syncHandler.handleUpdate(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
            final ReadRequest request) {
        try {
            return newResultPromise(syncHandler.handleRead(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
            final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            final Collection<ResourceResponse> resources = new LinkedList<>();
            final QueryResponse result = syncHandler.handleQuery(context, request, resources);
            for (final ResourceResponse resource : resources) {
                handler.handleResource(resource);
            }
            return newResultPromise(result);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
            final PatchRequest request) {
        try {
            return newResultPromise(syncHandler.handlePatch(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        try {
            return newResultPromise(syncHandler.handleDelete(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        try {
            return newResultPromise(syncHandler.handleCreate(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
            final ActionRequest request) {
        try {
            return newResultPromise(syncHandler.handleAction(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }
}
