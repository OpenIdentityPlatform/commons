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

import java.util.Collection;
import java.util.LinkedList;

import org.forgerock.json.fluent.JsonValue;

/**
 * Implementation class for {@link Resources#asRequestHandler}.
 */
final class SynchronousRequestHandlerAdapter implements RequestHandler {
    private final SynchronousRequestHandler syncHandler;

    SynchronousRequestHandlerAdapter(final SynchronousRequestHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            handler.handleResult(syncHandler.handleUpdate(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            handler.handleResult(syncHandler.handleRead(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final Collection<Resource> resources = new LinkedList<Resource>();
            final QueryResult result = syncHandler.handleQuery(context, request, resources);
            for (final Resource resource : resources) {
                handler.handleResource(resource);
            }
            handler.handleResult(result);
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {
            handler.handleResult(syncHandler.handlePatch(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {
            handler.handleResult(syncHandler.handleDelete(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            handler.handleResult(syncHandler.handleCreate(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            handler.handleResult(syncHandler.handleAction(context, request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }
}
