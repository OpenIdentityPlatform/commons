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

package org.forgerock.authz.filter.servlet;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles the failure result of the call to
 * {@link org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule#authorize(
 * javax.servlet.http.HttpServletRequest, org.forgerock.authz.filter.api.AuthorizationContext)} asynchronously.
 *
 * @since 1.5.0
 */
class ExceptionHandler implements AsyncFunction<Exception, Void, ServletException> {

    private final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    private final ResponseHandler responseHandler;
    private final HttpServletResponse resp;

    /**
     * Creates a new {@code FailureHandler} instance.
     *
     * @param responseHandler A {@code ResultHandler} instance.
     * @param resp The {@code HttpServletResponse} instance.
     */
    ExceptionHandler(ResponseHandler responseHandler, HttpServletResponse resp) {
        this.responseHandler = responseHandler;
        this.resp = resp;
    }

    /**
     * <p>Asynchronously applies this function to the {@code result} parameter and returns a {@link Promise} for the
     * result.</p>
     *
     * <p>Handles the failure case when a server exception occurs whilst determining if the request is authorized, the
     * {@code HttpServletResponse} will have a status of 500 set and the output of the reason for the exception written
     * to the response as JSON.</p>
     *
     * @param e The exception thrown when authorizing the request.
     * @return The {@code Promise} representing the result of applying this function to {@code e}.
     * @throws ServletException If the response could not be written to.
     */
    @Override
    public Promise<Void, ServletException> apply(Exception e) throws ServletException {

        try {
            logger.error("Authorization failed", e);
            PrintWriter writer = responseHandler.getWriter(resp);
            // If writer is null then response has already been committed!
            if (writer != null) {
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String message = responseHandler.getJsonErrorResponse(e.getMessage(), null).toString();
                writer.write(message);
            }

            return Promises.newResultPromise(null);
        } catch (IOException ex) {
            logger.error("Writing authorization failure response failed.", ex);
            return Promises.newExceptionPromise(new ServletException(ex));
        }
    }
}
