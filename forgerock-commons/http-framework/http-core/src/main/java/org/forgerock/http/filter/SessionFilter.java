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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.http.filter;

import java.io.IOException;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.session.SessionManager;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter implementation that uses a {@link SessionManager} to set the {@link Session} in the
 * {@link SessionContext}.
 *
 * <p>The previous {@code Session} value will be saved and restored after the {@code Handler} has been executed.</p>
 */
class SessionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SessionFilter.class);

    private final SessionManager sessionManager;

    SessionFilter(SessionManager sessionManager) {
        Reject.ifNull(sessionManager, "sessionManager must not be null");
        this.sessionManager = sessionManager;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        final SessionContext sessionContext = context.asContext(SessionContext.class);
        final Session oldSession = sessionContext.getSession();
        sessionContext.setSession(sessionManager.load(request));
        return next.handle(context, request)
                   .thenOnResult(new ResultHandler<Response>() {
                       @Override
                       public void handleResult(Response response) {
                           try {
                               sessionManager.save(sessionContext.getSession(), response);
                           } catch (IOException e) {
                               logger.error("Failed to save session", e);
                           } finally {
                               sessionContext.setSession(oldSession);
                           }
                       }
                   });
    }
}
