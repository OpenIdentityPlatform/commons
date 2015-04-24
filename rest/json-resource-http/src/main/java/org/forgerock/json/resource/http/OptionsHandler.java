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

package org.forgerock.json.resource.http;

import java.util.Arrays;

import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import static org.forgerock.json.resource.http.HttpUtils.*;

/**
 * {@link Filter} which handles OPTION HTTP requests to CREST resources.
 *
 * @since 3.0.0
 */
public class OptionsHandler implements Filter {

    /**
     * Handles all OPTION requests to CREST resources, all other request methods are handled by the {@link Handler}.
     */
    @Override
    public Promise<Response, ResponseException> filter(Context context, Request request,
            Handler next) {
        if ("OPTIONS".equals(request.getMethod())) {
            Response response = new Response().setStatusAndReason(200);
            response.getHeaders().put("Allow",
                    Arrays.asList(METHOD_DELETE, METHOD_GET, METHOD_HEAD, METHOD_PATCH, METHOD_POST, METHOD_PUT,
                            METHOD_OPTIONS, METHOD_TRACE));
            return Promises.newResultPromise(response);
        } else {
            return next.handle(context, request);
        }
    }
}
