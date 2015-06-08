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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collection;
import java.util.LinkedList;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

/**
 * Implementation class for {@link Resources#asRequestHandler}.
 */
final class SynchronousRequestHandlerAdapter implements RequestHandler {
    private final SynchronousRequestHandler syncHandler;

    SynchronousRequestHandlerAdapter(final SynchronousRequestHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    @Override
    public Promise<Resource, ResourceException> handleUpdate(final ServerContext context, final UpdateRequest request) {
        try {
            return newResultPromise(syncHandler.handleUpdate(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(final ServerContext context, final ReadRequest request) {
        try {
            return newResultPromise(syncHandler.handleRead(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResult, ResourceException> handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            final Collection<Resource> resources = new LinkedList<Resource>();
            final QueryResult result = syncHandler.handleQuery(context, request, resources);
            for (final Resource resource : resources) {
                handler.handleResource(resource);
            }
            return newResultPromise(result);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handlePatch(final ServerContext context, final PatchRequest request) {
        try {
            return newResultPromise(syncHandler.handlePatch(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleDelete(final ServerContext context, final DeleteRequest request) {
        try {
            return newResultPromise(syncHandler.handleDelete(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<Resource, ResourceException> handleCreate(final ServerContext context, final CreateRequest request) {
        try {
            return newResultPromise(syncHandler.handleCreate(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(final ServerContext context,
            final ActionRequest request) {
        try {
            return newResultPromise(syncHandler.handleAction(context, request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }
}
