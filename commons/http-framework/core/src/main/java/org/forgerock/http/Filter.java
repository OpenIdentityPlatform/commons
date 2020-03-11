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
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Filters the request and/or response of an HTTP exchange.
 */
public interface Filter {
    /**
     * Filters the request and/or response of an exchange. To pass the request
     * to the next filter or handler in the chain, the filter calls
     * {@code next.handle(context, request)}.
     * <p>
     * This method may elect not to pass the request to the next filter or
     * handler, and instead handle the request itself. It can achieve this by
     * merely avoiding a call to {@code next.handle(context, request)}
     * and creating its own response object. The filter is also at liberty to
     * replace a response with another of its own by intercepting the response
     * returned by the next handler.
     *
     * @param context
     *            The request context.
     * @param request
     *            The request.
     * @param next
     *            The next filter or handler in the chain to handle the request.
     * @return A {@code Promise} representing the response to be returned to the
     *         client.
     */
    Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next);
}
