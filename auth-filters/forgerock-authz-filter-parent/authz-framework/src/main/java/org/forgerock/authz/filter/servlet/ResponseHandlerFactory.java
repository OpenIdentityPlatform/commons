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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Factory class to handle the creation of new {@link ResultHandler} and {@link ExceptionHandler} instances.
 *
 * @since 1.5.0
 */
public class ResponseHandlerFactory {

    /**
     * Creates a new {@code SuccessHandler} instance, configured with the given parameters.
     *
     * @param req The {@code HttpServletRequest} instance.
     * @param resp The {@code HttpServletResponse} instance.
     * @param chain The {@code FilterChain} instance.
     * @return A new {@code SuccessHandler} instance.
     */
    ResultHandler newSuccessHandler(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) {
        return new ResultHandler(new ResponseHandler(), req, resp, chain);
    }

    /**
     * Creates a new {@code FailureHandler} instance, configured with the given parameters.
     *
     * @param resp The {@code HttpServletResponse} instance.
     * @return A new {@code FailureHandler} instance.
     */
    ExceptionHandler newFailureHandler(HttpServletResponse resp) {
        return new ExceptionHandler(new ResponseHandler(), resp);
    }
}
