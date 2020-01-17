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

package org.forgerock.http.routing;

import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.header.WarningHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

/**
 * API Version routing filter which creates a {@link ApiVersionRouterContext}
 * which contains the default routing behaviour when the
 * {@literal Accept-API-Version} header is set on the request. In addition also
 * sets the {@literal Warning} and {@literal Content-API-Version} headers on
 * the response.
 */
public class ResourceApiVersionRoutingFilter implements Filter {

    private final ResourceApiVersionBehaviourManager behaviourManager;

    /**
     * Constructs a new {@code ResourceApiVersionRoutingFilter} instance.
     *
     * @param behaviourManager The {@link ResourceApiVersionBehaviourManager} instance
     *                         that manages the API Version routing settings.
     */
    public ResourceApiVersionRoutingFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        this.behaviourManager = behaviourManager;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        final ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(context);
        final Version requestedResourceVersion;
        try {
            requestedResourceVersion = AcceptApiVersionHeader.valueOf(request).getResourceVersion();
        } catch (IllegalArgumentException e) {
            return newResultPromise(new Response(Status.BAD_REQUEST).setEntity(e.getMessage()).setCause(e));
        }
        return next.handle(apiVersionRouterContext, request)
                .thenOnResult(new ResultHandler<Response>() {
                    @Override
                    public void handleResult(Response response) {
                        Version protocolVersion = apiVersionRouterContext.getProtocolVersion();
                        Version resourceVersion = apiVersionRouterContext.getResourceVersion();
                        if (resourceVersion != null) {
                            response.getHeaders().add(new ContentApiVersionHeader(protocolVersion, resourceVersion));
                        }
                        if (apiVersionRouterContext.isWarningEnabled() && requestedResourceVersion == null) {
                            response.getHeaders().add(WarningHeader.newWarning("chf",
                                    "%s should be included in the request.", AcceptApiVersionHeader.NAME));
                        }
                    }
                });
    }

    /**
     * Creates a {@link ApiVersionRouterContext} using the default version
     * behaviour and whether to issue warnings from the
     * {@literal behaviourManager} instance.
     *
     * @param context The parent context.
     * @return A new {@code ApiVersionRouterContext}.
     */
    protected ApiVersionRouterContext createApiVersionRouterContext(Context context) {
        return new ApiVersionRouterContext(context, behaviourManager.getDefaultVersionBehaviour(),
                behaviourManager.isWarningEnabled());
    }
}
