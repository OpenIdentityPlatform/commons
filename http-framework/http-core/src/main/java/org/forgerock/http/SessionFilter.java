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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.http;

import static org.forgerock.http.HttpApplication.LOGGER;

import java.io.IOException;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

/**
 * Filter implementation that uses a {@link SessionManager} to set the {@link Session} in the {@link HttpContext}.
 *
 * The previous {@code Session} value will be saved and restored after the {@code Handler} has been executed.
 *
 * @since 1.0.0
 */
class SessionFilter implements Filter {
    private SessionManager sessionManager;

    SessionFilter(SessionManager sessionManager) {
        Reject.ifNull(sessionManager, "sessionManager must not be null");
        this.sessionManager = sessionManager;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        final HttpContext httpContext = context.asContext(HttpContext.class);
        final Session oldSession = httpContext.getSession();
        httpContext.setSession(sessionManager.load(request));
        return next.handle(context, request)
                   .thenOnResult(new ResultHandler<Response>() {
                       @Override
                       public void handleResult(Response response) {
                           try {
                               sessionManager.save(httpContext.getSession(), response);
                           } catch (IOException e) {
                               LOGGER.error("Failed to save session", e);
                           } finally {
                               httpContext.setSession(oldSession);
                           }
                       }
                   });
    }
}
