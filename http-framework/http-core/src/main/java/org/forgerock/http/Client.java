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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.http;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * An HTTP client which forwards requests to a wrapped {@link Handler}.
 */
public final class Client {
    private final Context defaultContext;
    private final Handler handler;

    /**
     * Creates a new {@code Client} which will route HTTP requests to the
     * provided {@link Handler} using a {@link RootContext} allocated during
     * construction when none is provided.
     *
     * @param handler
     *            The HTTP handler.
     */
    public Client(final Handler handler) {
        this(handler, new RootContext());
    }

    /**
     * Creates a new {@code Client} which will route HTTP requests to the
     * provided {@link Handler} using the specified {@link Context} if none is provided.
     *
     * @param handler
     *            The HTTP handler.
     * @param defaultContext
     *            The context to pass in with HTTP request when none is provided.
     */
    public Client(final Handler handler, final Context defaultContext) {
        this.handler = handler;
        this.defaultContext = defaultContext;
    }

    /**
     * Sends an HTTP request and returns a {@code Promise} representing the
     * pending HTTP response.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    public Promise<Response, NeverThrowsException> send(final Request request) {
        return handler.handle(defaultContext, request);
    }

    /**
     * Sends an HTTP request and returns a {@code Promise} representing the
     * pending HTTP response.
     *
     * @param context
     *            The associated processing context.
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    public Promise<Response, NeverThrowsException> send(final Context context, final Request request) {
        return handler.handle(context, request);
    }
}
