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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.authz;

import org.forgerock.json.fluent.JsonValue;

import javax.servlet.http.HttpServletRequest;

/**
 * Base interface for all Authorization Module implementations, which will contain the logic required
 * to determine if the request is authorized to proceed or not.
 *
 * @since 1.0.0
 */
public interface AuthorizationModule {

    /**
     * Initialises the Authorization Module by providing it with the configuration that the module was configured with.
     *
     * @param config A JsonValue of module configuration options.
     */
    void initialise(JsonValue config);

    /**
     * Determines whether the request is authorized to proceed or not.
     *
     * @param servletRequest The HttpServletRequest.
     * @param context The authorization context. Attributes in this context will be propagated to other authz modules
     *                and to the protected resource via the {@link AuthorizationContext#ATTRIBUTE_AUTHORIZATION_CONTEXT}
     *                attribute in the request.
     * @return <code>true</code> if the request is authorized to proceed, otherwise <code>false</code>.
     */
    boolean authorize(HttpServletRequest servletRequest, AuthorizationContext context);

    /**
     * Gives us an opportunity to clean up any resources that are being held by this module
     */
    void destroy();
}
