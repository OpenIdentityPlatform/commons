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

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * An HTTP client which forwards requests to a wrapped {@link Handler}.
 */
public final class Client {
    private final Context context;
    private final Handler handler;

    /**
     * Creates a new {@code Client} which will route HTTP requests to the
     * provided {@link Handler} using a {@link RootContext} allocated during
     * construction.
     *
     * @param handler
     *            The HTTP handler.
     */
    public Client(final Handler handler) {
        this(handler, new RootContext());
    }

    /**
     * Creates a new {@code Client} which will route HTTP requests to the
     * provided {@link Handler} using the specified {@link Context}.
     *
     * @param handler
     *            The HTTP handler.
     * @param context
     *            The context to pass in with each HTTP request.
     */
    public Client(final Handler handler, final Context context) {
        this.handler = handler;
        this.context = context;
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
        return handler.handle(context, request);
    }
}
