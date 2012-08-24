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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;

/**
 * Servlet 3.x request dispatcher which will attempt to process requests
 * asynchronously and using NIO if the platform supports it.
 */
final class Servlet3RequestDispatcher implements RequestDispatcher {
    // Used when the platform does not support asynchronous processing.
    private final RequestDispatcher blockingDispatcher;

    /**
     * Creates a new Servlet 3.x request dispatcher.
     *
     * @param connectionFactory
     *            The underlying connection factory to which requests should be
     *            dispatched.
     * @param jsonFactory
     *            The JSON factory which should be used for writing and reading
     *            JSON.
     */
    Servlet3RequestDispatcher(final ConnectionFactory connectionFactory,
            final JsonFactory jsonFactory) {
        this.blockingDispatcher = new Servlet2RequestDispatcher(connectionFactory, jsonFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchRequest(final Context context, final Request request,
            final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        if (httpRequest.isAsyncSupported()) {
            // TODO: support async processing.
            blockingDispatcher.dispatchRequest(context, request, httpRequest, httpResponse);
        } else {
            // Fall-back to 2.x synchronous processing.
            blockingDispatcher.dispatchRequest(context, request, httpRequest, httpResponse);
        }
    }

}
