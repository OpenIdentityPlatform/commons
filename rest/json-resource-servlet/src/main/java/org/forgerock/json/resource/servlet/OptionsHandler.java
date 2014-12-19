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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource.servlet;

import java.util.Arrays;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.Request;
import org.forgerock.http.Response;
import org.forgerock.http.ResponseException;
import org.forgerock.resource.core.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * {@link Filter} which handles OPTION HTTP requests to CREST resources.
 *
 * @since 3.0.0
 */
public class OptionsHandler implements Filter {

    /**
     * Handles all OPTION requests to CREST resources, all other request methods are handled by the {@link Handler}.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param next {@inheritDoc}
     * @return {@inheritDoc}
     * @throws ResponseException {@inheritDoc}
     */
    @Override
    public Promise<Response, ResponseException> filter(Context context, Request request, Handler next)
            throws ResponseException {
        if ("OPTIONS".equals(request.getMethod())) {
            Response response = new Response()
                    .setStatusAndReason(200);
            response.getHeaders().put("Allow",
                    Arrays.asList("DELETE", "GET", "HEAD", "PATCH", "POST", "PUT", "OPTIONS", "TRACE"));
            return Promises.newSuccessfulPromise(response);
        } else {
            return next.handle(context, request);
        }
    }
}
