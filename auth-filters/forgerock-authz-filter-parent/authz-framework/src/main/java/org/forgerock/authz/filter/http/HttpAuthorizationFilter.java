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

package org.forgerock.authz.filter.http;

import org.forgerock.authz.filter.http.api.HttpAuthorizationContext;
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * <p>The {@code HttpAuthorizationFilter} provides an entry point to the authorization framework for
 * authorizing CHF HTTP request/response messages.</p>
 *
 * @since 1.5.0
 */
public class HttpAuthorizationFilter implements Filter {

    private final ResponseHandler responseHandler;
    private final HttpAuthorizationModule module;

    public HttpAuthorizationFilter(HttpAuthorizationModule module) {
        Reject.ifNull(module);
        this.module = module;
        this.responseHandler = new ResponseHandler();
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        return module.authorize(context, request, HttpAuthorizationContext.forRequest(context))
                .thenAsync(new ResultHandler(responseHandler, context, request, next),
                        new ExceptionHandler(responseHandler));
    }
}
