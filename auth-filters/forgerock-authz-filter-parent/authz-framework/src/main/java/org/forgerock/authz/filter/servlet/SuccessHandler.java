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

package org.forgerock.authz.filter.servlet;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles the success result of the call to
 * {@link org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule#authorize(HttpServletRequest,
 * org.forgerock.authz.filter.api.AuthorizationContext)} asynchronously.
 *
 * @since 1.5.0
 */
class SuccessHandler implements AsyncFunction<AuthorizationResult, Void, ServletException> {

    private final Logger logger = LoggerFactory.getLogger(SuccessHandler.class);
    private final ResultHandler resultHandler;
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final FilterChain chain;

    /**
     * Creates a new {@code SuccessHandler} instance.
     *
     * @param resultHandler A {@code ResultHandler} instance.
     * @param req The {@code HttpServletRequest} instance.
     * @param resp The {@code HttpServletResponse} instance.
     * @param chain The {@code FilterChain} instance.
     */
    SuccessHandler(ResultHandler resultHandler, HttpServletRequest req, HttpServletResponse resp, FilterChain chain) {
        this.resultHandler = resultHandler;
        this.req = req;
        this.resp = resp;
        this.chain = chain;
    }

    /**
     * <p>Asynchronously applies this function to the {@code result} parameter and returns a {@link Promise} for the
     * result.</p>
     *
     * <p>If the {@code AuthorizationResult} is successful, i.e. the request is authorized to access the requested
     * resource, the {@link FilterChain#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
     * is called to allow the request access to the resource.
     * If the {@code AuthorizationResult} is not successful, i.e. the request is not authorized to access the requested
     * resource, the {@code HttpServletResponse} will have a status of 403 set and the output of the reason for not
     * being authorized written to the response as JSON.</p>
     *
     * @param result The result of the authorization of the request.
     * @return The {@code Promise} representing the result of applying this function to {@code result}.
     */
    @Override
    public Promise<Void, ServletException> apply(AuthorizationResult result) {
        try {
            if (result.isAuthorized()) {
                logger.debug("Request authorized.");
                chain.doFilter(req, resp);
                return Promises.newSuccessfulPromise(null);
            } else {
                logger.debug("Request unauthorized.");
                final PrintWriter writer = resultHandler.getWriter(resp);
                // If writer is null then response has already been committed!
                if (writer != null) {
                    resp.reset();
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    String message = resultHandler.getJsonForbiddenResponse(result.getReason(), result.getDetail())
                            .toString();
                    writer.write(message);
                }
                return Promises.newSuccessfulPromise(null);
            }
        } catch (IOException e) {
            logger.debug("Exception whilst authorizing: {}", e.getMessage());
            return Promises.newFailedPromise(new ServletException(e));
        } catch (ServletException e) {
            logger.debug("Exception whilst authorizing: {}", e.getMessage());
            return Promises.newFailedPromise(e);
        }
    }
}
