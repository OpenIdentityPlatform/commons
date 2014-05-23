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

package org.forgerock.authz.basic.servlet;

import org.forgerock.authz.filter.servlet.AuthorizationModuleFactory;

import static org.forgerock.authz.filter.servlet.AuthorizationModules.newAuthorizationModuleFactory;

/**
 * Factory class for creating an {@link AuthorizationModuleFactory} which will return the
 * {@link org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule} instance that will be used to protect
 * access to resources by performing authorization on each incoming request.
 */
public final class BasicAuthorizationFactory {

    /**
     * Private utility constructor.
     */
    private BasicAuthorizationFactory() { }

    /**
     * Returns a new {@link AuthorizationModuleFactory} which contains the {@code HttpServletAuthorizationModule} which
     * will be used to protect access to resources by performing authorization on each incoming request.
     *
     * @return A new {@code AuthorizationModuleFactory} which contains the {@code HttpServletAuthorizationModule} which
     * will perform the authorization of requests.
     */
    public static AuthorizationModuleFactory getAuthorizationModuleFactory() {
        return newAuthorizationModuleFactory(new HttpServletEndpointCheckerAuthorizationModule(new EndpointChecker()));
    }
}
