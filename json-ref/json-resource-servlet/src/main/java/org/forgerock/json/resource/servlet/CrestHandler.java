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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource.servlet;

import java.util.Arrays;

import org.forgerock.http.Handler;
import org.forgerock.http.Response;
import org.forgerock.http.ResponseException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.resource.core.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * <p>A HTTP {@link Handler} implementation which forwards requests to an
 * {@link HttpAdapter}.</p>
 *
 * <p>Instances must be provided with a {@code ConnectionFactory} in order to
 * operate and optionally a {@code HttpContextFactory}.</p>
 */
public class CrestHandler implements Handler {

    private final HttpAdapter adapter;

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and no context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     */
    public CrestHandler(ConnectionFactory connectionFactory) {
        Reject.ifNull(connectionFactory);
        adapter = new HttpAdapter(connectionFactory);
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and a context factory which will always return the provided
     * request context.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param parentContext
     *            The parent request context which should be used as the parent
     *            context of each request context.
     */
    public CrestHandler(ConnectionFactory connectionFactory, Context parentContext) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(parentContext);
        adapter = new HttpAdapter(connectionFactory, parentContext);
    }

    /**
     * Creates a new JSON resource HTTP Handler with the provided connection
     * factory and context factory.
     *
     * @param connectionFactory
     *            The connection factory.
     * @param contextFactory
     *            The context factory which will be used to obtain the parent
     *            context of each request context.
     */
    public CrestHandler(ConnectionFactory connectionFactory, HttpContextFactory contextFactory) {
        Reject.ifNull(connectionFactory);
        Reject.ifNull(contextFactory);
        adapter = new HttpAdapter(connectionFactory, contextFactory);
    }

//    public void destroy() { //TODO how to hook this into the shutdown of the application? Maybe its the HttpApplications job?
//        if (connectionFactory != null) {
//            connectionFactory.close();
//        }
//    }

    /**
     * Handles the incoming HTTP request and forwards it on to an
     * {@code HttpAdapter} to be converted to a CREST request.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return Promise containing a {@code Response} or {@code ResponseException}.
     */
    @Override
    public Promise<Response, ResponseException> handle(Context context, org.forgerock.http.Request request) {
        if ("OPTIONS".equals(request.getMethod())) {
            Response response = new Response()
                    .setStatusAndReason(200);
            response.getHeaders().put("Allow",
                    Arrays.asList("DELETE", "GET", "HEAD", "PATCH", "POST", "PUT", "OPTIONS", "TRACE"));
            return Promises.newSuccessfulPromise(response);
        } else {
            return adapter.handle(context, request);
        }
    }
}
