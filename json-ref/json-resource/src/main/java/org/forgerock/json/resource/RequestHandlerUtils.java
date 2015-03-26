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

import org.forgerock.http.ServerContext;
import org.forgerock.util.promise.FailureHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.SuccessHandler;

/**
 * Assorted utility methods useful to request handlers.
 */
class RequestHandlerUtils {

    static <T> void handle(AnnotatedMethod method, ServerContext context, Request request,
            final ResultHandler<T> handler) {
        handle(method.<T>invoke(context, request, null), handler);
    }

    static <T> void handle(AnnotatedMethod method, ServerContext context, Request request,
            final ResultHandler<T> handler, QueryResultHandler queryResultHandler) {
        handle(method.<T>invoke(context, request, queryResultHandler, null), handler);
    }

    static <T> void handle(AnnotatedMethod method, ServerContext context, Request request, String id,
            final ResultHandler<T> handler) {
        handle(method.<T>invoke(context, request, id), handler);
    }

    static <T> void handle(Promise<T, ? extends ResourceException> promise, final ResultHandler<T> handler) {
        promise.onSuccess(new SuccessHandler<T>() {
            @Override
            public void handleResult(T result) {
                handler.handleResult(result);
            }
        }).onFailure(new FailureHandler<ResourceException>() {
            @Override
            public void handleError(ResourceException error) {
                handler.handleError(error);
            }
        });
    }

    private RequestHandlerUtils() {}
}
