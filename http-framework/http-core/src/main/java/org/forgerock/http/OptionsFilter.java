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

package org.forgerock.http;

import java.util.Arrays;
import java.util.List;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * {@link Filter} which handles OPTION HTTP requests to CREST resources.
 *
 * @since 1.0.0
 */
public final class OptionsFilter implements Filter {

    /**  */
    public static final String METHOD_DELETE = "DELETE";
    /**  */
    public static final String METHOD_GET = "GET";
    /**  */
    public static final String METHOD_HEAD = "HEAD";
    /**  */
    public static final String METHOD_OPTIONS = "OPTIONS";
    /**  */
    public static final String METHOD_PATCH = "PATCH";
    /**  */
    public static final String METHOD_POST = "POST";
    /**  */
    public static final String METHOD_PUT = "PUT";
    /**  */
    public static final String METHOD_TRACE = "TRACE";

    private final List<String> allowedMethods;

    OptionsFilter(String... allowedMethods) {
        this.allowedMethods = Arrays.asList(allowedMethods);
    }

    /**
     * Handles all OPTION requests to CREST resources, all other request methods are handled by the {@link Handler}.
     */
    @Override
    public Promise<Response, ResponseException> filter(Context context, Request request,
            Handler next) {
        if ("OPTIONS".equals(request.getMethod())) {
            Response response = new Response(Status.OK);
            response.getHeaders().put("Allow", allowedMethods);
            return Promises.newResultPromise(response);
        } else {
            return next.handle(context, request);
        }
    }
}
