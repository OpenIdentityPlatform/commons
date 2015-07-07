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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.servlet;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.http.HttpApplication;

/**
 * Determines whether the context path and/or the servlet path will get
 * consumed when routing to the {@link HttpApplication}.
 *
 * <p>Configured by Servlet init-param of
 * {@link HttpFrameworkServlet#ROUTING_BASE_INIT_PARAM_NAME}.</p>
 *
 * <p>By default, if no servlet init-param is set, {@link #SERVLET_PATH} will
 * be used, meaning that the {@code HttpApplication} will be given requests
 * which DO NOT contain any information about the web application
 * configuration.
 * When {@link #CONTEXT_PATH} is selected the {@code HttpApplication} will be
 * given requests which DO contain the relative Servlet context path and
 * therefore will be dependant on the web application configuration.</p>
 */
enum ServletRoutingBase {

    /**
     * Only the context path will be consumed when routing requests to the
     * {@link HttpApplication}.
     */
    CONTEXT_PATH {
        @Override
        String extractMatchedUri(HttpServletRequest request) {
            String contextPath = forceEmptyIfNull(request.getContextPath());
            return contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
        }
    },

    /**
     * Both the context path and the servlet path will be consumed when routing
     * requests to the {@link HttpApplication}.
     */
    SERVLET_PATH {
        @Override
        String extractMatchedUri(HttpServletRequest request) {
            String contextPath = CONTEXT_PATH.extractMatchedUri(request);
            return contextPath + forceEmptyIfNull(request.getServletPath());
        }
    };

    /**
     * Determines the portion of the request URI that will be matched based on
     * the selected {@code ServletRoutingBase}.
     *
     * @param request The request.
     * @return The matched portion of the request URI.
     */
    abstract String extractMatchedUri(HttpServletRequest request);

    private static String forceEmptyIfNull(final String s) {
        return s != null ? s : "";
    }
}
