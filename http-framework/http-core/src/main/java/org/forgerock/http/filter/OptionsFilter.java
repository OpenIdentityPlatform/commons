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

package org.forgerock.http.filter;

import java.util.Arrays;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * {@link Filter} which handles OPTION HTTP requests to CREST resources.
 */
public final class OptionsFilter implements Filter {

    /** The HTTP DELETE method. */
    public static final String METHOD_DELETE = "DELETE";
    /** The HTTP GET method. */
    public static final String METHOD_GET = "GET";
    /** The HTTP HEAD method. */
    public static final String METHOD_HEAD = "HEAD";
    /** The HTTP OPTIONS method. */
    public static final String METHOD_OPTIONS = "OPTIONS";
    /** The HTTP PATCH method. */
    public static final String METHOD_PATCH = "PATCH";
    /** The HTTP POST method. */
    public static final String METHOD_POST = "POST";
    /** The HTTP PUT method. */
    public static final String METHOD_PUT = "PUT";
    /** The HTTP TRACE method. */
    public static final String METHOD_TRACE = "TRACE";

    private final List<String> allowedMethods;

    OptionsFilter(String... allowedMethods) {
        this.allowedMethods = Arrays.asList(allowedMethods);
    }

    /**
     * Handles all OPTION requests to CREST resources, all other request methods are handled by the {@link Handler}.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param next {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request,
            Handler next) {
        switch (request.getMethod()) {
        case METHOD_OPTIONS:
            Response response = new Response(Status.OK);
            response.getHeaders().put("Allow", allowedMethods);
            return Promises.newResultPromise(response);
        default:
            return next.handle(context, request);
        }
    }
}
