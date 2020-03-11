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

import org.forgerock.services.context.Context;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

/**
 * API Version routing filter which creates a {@link ApiVersionRouterContext}
 * which contains the default routing behaviour when the
 * {@literal Accept-API-Version} header is set on the request. In addition also
 * sets the {@literal Warning} and {@literal Content-API-Version} headers on
 * the response.
 */
public class ResourceApiVersionRoutingFilter extends org.forgerock.http.routing.ResourceApiVersionRoutingFilter
        implements Filter {

    ResourceApiVersionRoutingFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        super(behaviourManager);
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleAction(apiVersionRouterContext, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleCreate(apiVersionRouterContext, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleDelete(apiVersionRouterContext, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handlePatch(apiVersionRouterContext, request));
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleQuery(apiVersionRouterContext, request, handler));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleRead(apiVersionRouterContext, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        return wrapWithApiVersionInfo(apiVersionRouterContext, request,
                next.handleUpdate(apiVersionRouterContext, request));
    }

    private <V extends Response> Promise<V, ResourceException> wrapWithApiVersionInfo(
            final ApiVersionRouterContext apiVersionRouterContext, final Request request,
            Promise<V, ResourceException> promise) {
        return promise
                .thenOnResult(new ResultHandler<V>() {
                    @Override
                    public void handleResult(V response) {
                        setApiVersionInfo(apiVersionRouterContext, request, response);
                    }
                })
                .thenOnException(new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException e) {
                        setApiVersionInfo(apiVersionRouterContext, request, e);
                    }
                });
    }

    static void setApiVersionInfo(ApiVersionRouterContext apiVersionRouterContext, Request request, Response response) {
        Version resourceVersion = apiVersionRouterContext.getResourceVersion();
        if (resourceVersion != null) {
            response.setResourceApiVersion(resourceVersion);
        }
        if (apiVersionRouterContext.isWarningEnabled() && request.getResourceVersion() == null) {
            AdviceContext adviceContext = apiVersionRouterContext.asContext(AdviceContext.class);
            if (!adviceContext.getAdvices().containsKey("crest")) {
                adviceContext.putAdvice("Warning", AdviceWarning.getNotPresent("CREST", "Accept-API-Version")
                        .toString());
            }
        }
    }
}
