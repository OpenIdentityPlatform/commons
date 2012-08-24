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

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;

/**
 * An interface for implementing different request dispatch strategies.
 * Depending on the available Servlet API version a different strategy may be
 * used:
 * <ul>
 * <li>2.5 (JavaEE 5) - synchronous processing and synchronous IO
 * <li>3.0 (JavaEE 6) - asynchronous (non-blocking) processing and synchronous
 * IO
 * <li>3.1 (JavaEE 7) - asynchronous (non-blocking) processing and asynchronous
 * IO (NIO)
 * </ul>
 */
interface RequestDispatcher {

    /**
     * Dispatches a request.
     *
     * @param context
     *            The request context.
     * @param request
     *            The request.
     * @param httpRequest
     *            The HTTP request.
     * @param httpResponse
     *            The HTTP response.
     */
    void dispatchRequest(Context context, Request request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse);

}
