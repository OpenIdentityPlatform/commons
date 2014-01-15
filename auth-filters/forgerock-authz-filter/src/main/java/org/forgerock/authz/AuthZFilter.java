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

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.json.resource.ResourceException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authorization Filter which protects resources based on the user's privileges who made the request.
 * <br/>
 * Uses the Configurator implementation given as a init-param, to delegate the authorization processing to
 * the configured implementation of a AuthorizationFilter interface.
 *
 * &lt;filter&gt;
 *     &lt;filter-name&gt;SessionAdminOnlyFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.forgerock.authz.AuthZFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;configurationImpl&lt;/param-name&gt;
 *         &lt;param-value&gt;org.forgerock.openam.authz.filter.AdminOnlyAuthZConfigurator&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 *
 * @author Phill Cunnington
 * @since 1.0.0
 */
public class AuthZFilter implements Filter {

    private static final String CONFIGURATOR_IMPL_INIT_PARAM = "configurator";

    private AuthorizationConfigurator configurator;
    private AuditLogger<HttpServletRequest> auditLogger;
    private AuthorizationFilter authorizationFilter;

    /**
     * Initialises the instance of the Configurator that will be used to set up this AuthZFilter.
     *
     * @param filterConfig {@inheritDoc}
     * @throws ServletException If the Configurator can not be created.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(FilterConfig filterConfig) throws ServletException {
        String configuratorClassName = filterConfig.getInitParameter(CONFIGURATOR_IMPL_INIT_PARAM);

        try {
            Class<? extends AuthorizationConfigurator> configuratorClass =
                    Class.forName(configuratorClassName).asSubclass(AuthorizationConfigurator.class);

            configurator = configuratorClass.newInstance();

        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (InstantiationException e) {
            throw new ServletException(e);
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Lazily initialises the AuditLogger, DebugLogger and AuthorizationFilter member variables from the AuthZFilter
     * Configurator instance.
     */
    private synchronized void init() {
        if (authorizationFilter == null) {

            auditLogger = configurator.getAuditLogger();
            DebugLogger debugLogger = configurator.getDebugLogger();

            authorizationFilter = configurator.getAuthorizationFilter();
            authorizationFilter.initialise(configurator, auditLogger, debugLogger);
        }
    }

    /**
     * Makes a call to the AuthorizationFilter instance to perform the authorization logic and based on the result of
     * that call will either, send the request through or return a 403 Http response.
     * <br/>
     * The method will also make a call to the AuditLoggers audit method to audit the authorization request.
     *
     * @param servletRequest {@inheritDoc}
     * @param servletResponse {@inheritDoc}
     * @param filterChain {@inheritDoc}
     * @throws IOException If there is a problem down the filter chain or a problem writing the exception response.
     * @throws ServletException {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        init();

        if (!(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("Request/response must be of types HttpServletRequest and HttpServletResponse");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (authorizationFilter.authorize(request, response)) {
            audit(AuthResult.SUCCESS, request);
            filterChain.doFilter(request, response);
        } else {
            audit(AuthResult.FAILURE, request);
            handleUnauthorisedException(response);
        }
    }

    /**
     * Makes a call to the AuditLoggers audit method to perform an audit of the authorization request.
     *
     * @param authResult The AuthResult of the authorization request.
     * @param request The HttpServletRequest of the authorization request.
     */
    private void audit(AuthResult authResult, HttpServletRequest request) {
        auditLogger.audit(new AuditRecord<HttpServletRequest>(authResult, request));
    }

    /**
     * Handles the case where the authorization request was denied and will write a Http 403 to the response.
     *
     * @param response The HttpServletResponse.
     * @throws IOException If there is a problem writing to the response.
     */
    private void handleUnauthorisedException(HttpServletResponse response) throws IOException {
        ResourceException jre = ResourceException.getException(403, "Access denied");
        response.setStatus(403);
        try {
            handleException(response, jre);
        } catch (IOException e) {
            handleServerException(response);
        }
    }

    /**
     * Handles the case where an internal server error has occured and will write a Http 500 to the response.
     *
     * @param response The HttpServletResponse.
     * @throws IOException If there is a problem writing to the response.
     */
    private void handleServerException(HttpServletResponse response) throws IOException {
        ResourceException jre = ResourceException.getException(500, "Server Error");
        response.setStatus(500);
        handleException(response, jre);
    }

    /**
     * Performs the actual writing to the response.
     *
     * @param response The HttpServletResponse.
     * @param jsonResourceException The JsonResourceException containing the Http code and message.
     * @throws IOException If there is a problem writing to the response.
     */
    private void handleException(HttpServletResponse response, ResourceException jsonResourceException)
            throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(jsonResourceException.toJsonValue().toString());
    }

    /**
     * Nullifies the AuthZFilter's member variables.
     */
    @Override
    public void destroy() {
        configurator = null;
        auditLogger = null;
        authorizationFilter = null;
    }
}
