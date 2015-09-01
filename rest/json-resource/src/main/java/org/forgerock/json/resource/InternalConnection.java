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
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

final class InternalConnection extends AbstractAsynchronousConnection {
    private final RequestHandler requestHandler;

    InternalConnection(final RequestHandler handler) {
        this.requestHandler = handler;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionAsync(final Context context,
            final ActionRequest request) {
        return requestHandler.handleAction(context, request);
    }

    @Override
    public void close() {
        // Do nothing.
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createAsync(final Context context,
            final CreateRequest request) {
        return requestHandler.handleCreate(context, request)
                             .then(filterResponse(request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteAsync(final Context context,
            final DeleteRequest request) {
        return requestHandler.handleDelete(context, request)
                             .then(filterResponse(request));

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
    public Promise<ResourceResponse, ResourceException> patchAsync(final Context context, final PatchRequest request) {
        return requestHandler.handlePatch(context, request)
                             .then(filterResponse(request));

    }

    @Override
    public Promise<QueryResponse, ResourceException> queryAsync(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return requestHandler.handleQuery(context, request,
                new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        return handler.handleResource(Resources.filterResource(resource, request.getFields()));
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readAsync(final Context context, final ReadRequest request) {
        return requestHandler.handleRead(context, request)
                             .then(filterResponse(request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateAsync(final Context context,
            final UpdateRequest request) {
        return requestHandler.handleUpdate(context, request)
                             .then(filterResponse(request));
    }

    private Function<ResourceResponse, ResourceResponse, ResourceException> filterResponse(final Request request) {
        return new Function<ResourceResponse, ResourceResponse, ResourceException>() {
            @Override
            public ResourceResponse apply(final ResourceResponse response)
                    throws ResourceException {
                return Resources.filterResource(response, request.getFields());
            }
        };
    }
}
