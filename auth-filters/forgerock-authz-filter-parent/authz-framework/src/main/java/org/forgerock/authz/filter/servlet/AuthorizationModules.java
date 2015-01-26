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

import javax.servlet.FilterConfig;

import org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule;
import org.forgerock.util.Reject;

/**
 * This class contains methods for creating an {@link AuthorizationModuleFactory} which will return the
 * {@link HttpServletAuthorizationModule} instance that will be used to protect access to resources by performing
 * authorization on each incoming request.
 *
 * @since 1.5.0
 */
public final class AuthorizationModules {

    public static final String INIT_PARAM_MODULE_CLASS_NAME = "servlet-authz-module-class";

    /**
     * Private utility constructor.
     */
    private AuthorizationModules() { }

    /**
     * Returns a new {@link AuthorizationModuleFactory} which finds by reflection the
     * {@code HttpServletAuthorizationModule} which will be used to protect access to resources by performing
     * authorization on each incoming request.
     *
     * @param config The {@code FilterConfig} that will be used to obtain the module class name.
     * @return A new {@code AuthorizationModuleFactory} which contains the {@code HttpServletAuthorizationModule} which
     * will perform the authorization of requests.
     * @throws java.lang.IllegalArgumentException If either the specified {@code module} cannot be found or instantiated.
     */
    public static AuthorizationModuleFactory getAuthorizationModuleFactory(final FilterConfig config) {
        String moduleTypeName = config.getInitParameter(INIT_PARAM_MODULE_CLASS_NAME);
        Reject.ifTrue(moduleTypeName == null, "Authorization module class name cannot be null.");
        try {
            Class<?> moduleType = Class.forName(moduleTypeName);
            if (!HttpServletAuthorizationModule.class.isAssignableFrom(moduleType)) {
                throw new IllegalArgumentException("Servlet authz module class is not a " +
                        "HttpServletAuthorizationModule: " + moduleTypeName);
            }
            final HttpServletAuthorizationModule module = moduleType.asSubclass(HttpServletAuthorizationModule.class).newInstance();
            return new AuthorizationModuleFactory() {
                @Override
                public HttpServletAuthorizationModule getAuthorizationModule() {
                    return module;
                }
            };
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Servlet authz module class not found: " + moduleTypeName, e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate module: " + moduleTypeName, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate module: " + moduleTypeName, e);
        }
    }
}
