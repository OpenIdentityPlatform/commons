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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.annotations.Create;
import org.forgerock.json.resource.annotations.Query;
import org.forgerock.json.resource.annotations.RequestHandler;
import org.forgerock.util.promise.Promise;

/**
 * Exposes an annotated POJO as collection methods {@link org.forgerock.json.resource.RequestHandler} by
 * looking for annotated and/or conventionally-named methods (as per
 * {@link org.forgerock.json.resource.annotations.RequestHandler}).
 * <p>
 * This class will handle the requests to the collection-level endpoint, so only Create, Query and Action
 * are implemented - the remaining methods delegate to the {@link InterfaceCollectionHandler} for
 * reporting the erroneous request to the caller.
 * {@see org.forgeock.json.resource.annotations}
 */
class AnnotatedCollectionHandler extends InterfaceCollectionHandler {

    private final AnnotatedMethod createMethod;
    private final AnnotatedMethod queryMethod;
    private final AnnotatedActionMethods actionMethods;

    public AnnotatedCollectionHandler(Object requestHandler) {
        super(null);
        if (!requestHandler.getClass().isAnnotationPresent(RequestHandler.class)) {
            throw new IllegalArgumentException("RequestHandler missing from class: " +
                    requestHandler.getClass().getName());
        }
        this.createMethod = AnnotatedMethod.findMethod(requestHandler, Create.class, false);
        this.queryMethod = AnnotatedMethod.findMethod(requestHandler, Query.class, false);
        this.actionMethods = AnnotatedActionMethods.findAll(requestHandler, false);
    }

    @Override
    public Promise<Resource, ResourceException> handleCreate(ServerContext context, CreateRequest request) {
        return RequestHandlerUtils.handle(createMethod, context, request);
    }

    @Override
    public Promise<QueryResult, ResourceException> handleQuery(ServerContext context, QueryRequest request,
            QueryResourceHandler handler) {
        return RequestHandlerUtils.handle(queryMethod, context, request, handler);
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request) {
        return actionMethods.invoke(context, request, null);
    }
}
