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

import org.forgerock.http.Context;
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

final class InternalConnection extends AbstractAsynchronousConnection {
    private final RequestHandler requestHandler;

    InternalConnection(final RequestHandler handler) {
        this.requestHandler = handler;
    }

    @Override
    public Promise<JsonValue, ResourceException> actionAsync(final Context context,
            final ActionRequest request) {
        return requestHandler.handleAction(getServerContext(context), request);
    }

    @Override
    public void close() {
        // Do nothing.
    }

    @Override
    public Promise<Resource, ResourceException> createAsync(final Context context,
            final CreateRequest request) {
        return requestHandler.handleCreate(getServerContext(context), request);
    }

    @Override
    public Promise<Resource, ResourceException> deleteAsync(final Context context,
            final DeleteRequest request) {
        return requestHandler.handleDelete(getServerContext(context), request);
    }

    @Override
    public boolean isClosed() {
        // Always open.
        return false;
    }

    @Override
    public boolean isValid() {
        // Always valid.
        return true;
    }

    @Override
    public Promise<Resource, ResourceException> patchAsync(final Context context,
            final PatchRequest request) {
        return requestHandler.handlePatch(getServerContext(context), request);
    }

    @Override
    public Promise<QueryResult, ResourceException> queryAsync(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return requestHandler.handleQuery(getServerContext(context), request,
                new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(Resource resource) {
                        return handler.handleResource(Resources.filterResource(resource, request.getFields()));
                    }
                });
    }

    @Override
    public Promise<Resource, ResourceException> readAsync(final Context context,
            final ReadRequest request) {
        return requestHandler.handleRead(getServerContext(context), request);
    }

    @Override
    public Promise<Resource, ResourceException> updateAsync(final Context context,
            final UpdateRequest request) {
        return requestHandler.handleUpdate(getServerContext(context), request);
    }

    private ServerContext getServerContext(final Context context) {
        if (context instanceof ServerContext) {
            return (ServerContext) context;
        } else {
            return new ServerContext(context);
        }
    }
}
