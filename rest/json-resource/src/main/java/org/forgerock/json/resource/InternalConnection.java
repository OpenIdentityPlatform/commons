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
import org.forgerock.util.promise.PromiseImpl;

final class InternalConnection extends AbstractAsynchronousConnection {
    private final RequestHandler requestHandler;

    InternalConnection(final RequestHandler handler) {
        this.requestHandler = handler;
    }

    @Override
    public Promise<JsonValue, ResourceException> actionAsync(final Context context,
            final ActionRequest request) {
        final PromiseImpl<JsonValue, ResourceException> promise = PromiseImpl.create();
        requestHandler.handleAction(getServerContext(context), request,
                new ResultHandler<JsonValue>() {
                    @Override
                    public void handleResult(JsonValue result) {
                        promise.handleResult(result);
                    }

                    @Override
                    public void handleException(ResourceException error) {
                        promise.handleException(error);
                    }
                });
        return promise;
    }

    @Override
    public void close() {
        // Do nothing.
    }

    @Override
    public Promise<Resource, ResourceException> createAsync(final Context context,
            final CreateRequest request) {
        final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        requestHandler
                .handleCreate(getServerContext(context), request, adapt(request, promise));
        return promise;
    }

    private ResultHandler<Resource> adapt(final Request request,
            final PromiseImpl<Resource, ResourceException> promise) {
        return new ResultHandler<Resource>() {
            @Override
            public void handleResult(Resource result) {
                promise.handleResult(Resources.filterResource(result, request.getFields()));
            }

            @Override
            public void handleException(ResourceException error) {
                promise.handleException(error);
            }
        };
    }

    @Override
    public Promise<Resource, ResourceException> deleteAsync(final Context context,
            final DeleteRequest request) {
        final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        requestHandler
                .handleDelete(getServerContext(context), request, adapt(request, promise));
        return promise;
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
        final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        requestHandler.handlePatch(getServerContext(context), request, adapt(request, promise));
        return promise;
    }

    @Override
    public Promise<QueryResult, ResourceException> queryAsync(final Context context,
            final QueryRequest request, final QueryResultHandler handler) {
        final PromiseImpl<QueryResult, ResourceException> promise = PromiseImpl.create();
        requestHandler.handleQuery(getServerContext(context), request,
                new QueryResultHandler() {

                    @Override
                    public void handleResult(QueryResult result) {
                        promise.handleResult(result);
                        if (handler != null) {
                            handler.handleResult(result);
                        }
                    }

                    @Override
                    public boolean handleResource(Resource resource) {
                        if (handler != null) {
                            return handler.handleResource(Resources.filterResource(resource, request
                                    .getFields()));
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void handleException(ResourceException error) {
                        promise.handleException(error);
                        if (handler != null) {
                            handler.handleException(error);
                        }
                    }
                });
        return promise;
    }

    @Override
    public Promise<Resource, ResourceException> readAsync(final Context context,
            final ReadRequest request) {
        final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        requestHandler.handleRead(getServerContext(context), request, adapt(request, promise));
        return promise;
    }

    @Override
    public Promise<Resource, ResourceException> updateAsync(final Context context,
            final UpdateRequest request) {
        final PromiseImpl<Resource, ResourceException> promise = PromiseImpl.create();
        requestHandler
                .handleUpdate(getServerContext(context), request, adapt(request, promise));
        return promise;
    }

    private ServerContext getServerContext(final Context context) {
        if (context instanceof ServerContext) {
            return (ServerContext) context;
        } else {
            return new ServerContext(context);
        }
    }

}
