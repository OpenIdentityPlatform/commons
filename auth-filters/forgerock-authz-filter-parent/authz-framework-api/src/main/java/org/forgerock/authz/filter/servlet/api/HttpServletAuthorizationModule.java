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

package org.forgerock.authz.filter.servlet.api;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.util.promise.Promise;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>A {@code HttpServletAuthorizationModule} authorizes client HTTP requests asynchronously.</p>
 *
 * <p>A module implementation should assume it may be used to authorize different requests from different clients. A
 * module should also assume it may be used concurrently by multiple callers. It is the module implementation's
 * responsibility to properly save and restore state as necessary. A module that does not need to do so may remain
 * completely stateless.</p>
 *
 * @since 1.5.0
 */
public interface HttpServletAuthorizationModule {

    /**
     * <p>Authorizes a received HTTP Servlet request.</p>
     *
     * <p>This method conveys the outcome of its authorization either by returning an {@link AuthorizationResult} value
     * or an {@link Exception}</p>
     *
     * @param req The {@code HttpServletRequest} to authorize.
     * @param context The authorization context. Attributes in this context will be propagated to other authorization
     *                modules and to the protected resource via the
     *                {@link HttpAuthorizationContext#ATTRIBUTE_AUTHORIZATION_CONTEXT} attribute in the request.
     * @return A {@link Promise} representing the result of the method call. The result of the {@code Promise}, when the
     * method completes successfully, will be an {@code AuthorizationResult} containing the result of the authorization,
     * or will be an {@code AuthorizationException} detailing the cause of the failure.
     */
    Promise<AuthorizationResult, AuthorizationException> authorize(HttpServletRequest req,
            AuthorizationContext context);
}
