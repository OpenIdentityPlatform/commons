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

package org.forgerock.authz.filter.http;

import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.http.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the failure result of the call to
 * {@link HttpAuthorizationModule#authorize(Context, Request, AuthorizationContext)}
 * asynchronously.
 *
 * @since 1.5.0
 */
class ExceptionHandler implements AsyncFunction<Exception, Response, NeverThrowsException> {

    private final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    private final ResponseHandler responseHandler;

    /**
     * Creates a new {@code FailureHandler} instance.
     *
     * @param responseHandler A {@code ResultHandler} instance.
     */
    ExceptionHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * <p>Asynchronously applies this function to the {@code result} parameter and returns a {@link Promise} for the
     * result.</p>
     *
     * <p>Handles the failure case when a server exception occurs whilst determining if the request is authorized, the
     * {@code Response} will have a status of 500 set and the output of the reason for the exception written
     * to the response as JSON.</p>
     *
     * @param e The exception thrown when authorizing the request.
     * @return The {@code Promise} representing the result of applying this function to {@code e}.
     */
    @Override
    public Promise<Response, NeverThrowsException> apply(Exception e) {
        logger.error("Authorization failed", e);
        Response response = new Response(Status.INTERNAL_SERVER_ERROR);
        response.setEntity(responseHandler.getJsonErrorResponse(e.getMessage(), null).getObject());
        return newResultPromise(response);
    }
}
