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
 * Copyright 2009 Sun Microsystems Inc.
 * Portions Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Asynchronously handles an HTTP {@link Request} by producing an associated {@link Response}.
 */
public interface Handler {

    /**
     * Returns a {@link Promise} representing the asynchronous {@link Response} of the given {@code request}.
     * If any (asynchronous) processing goes wrong, the promise still contains a {@link Response} (probably from the
     * {@literal 4xx} or {@literal 5xx} status code family).
     * <p>
     * A handler that doesn't hand-off the processing to another downstream handler is responsible for
     * creating the response.
     * <p>
     * The returned {@link Promise} contains the response returned from the server as-is.
     * This is responsibility of the handler to produce the appropriate error response ({@literal 404},
     * {@literal 500}, ...) in case of processing error.
     * <p>
     * <b>Note:</b> As of Promise 2.0 implementation, it is <b>not permitted</b> to throw any runtime exception here.
     * Doing so produce unexpected behaviour (most likely a server-side hang of the processing thread).
     *
     * @param context
     *            The request context.
     * @param request
     *            The request.
     * @return A {@code Promise} representing the response to be returned to the caller.
     */
    Promise<Response, NeverThrowsException> handle(Context context, Request request);
}
