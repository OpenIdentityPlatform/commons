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

import org.forgerock.authz.filter.servlet.api.HttpAuthorizationContext;
import org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>The {@code HttpServletAuthorizationFilter} provides an entry point to the authorization framework for
 * authorizing HTTP request/response messages.</p>
 *
 * <p>The filter is fully asynchronous, with support for Servlet 3.x asynchronous processing and Servlet 2.x synchronous
 * model. (In Servlet 2.x environments the filter will block until the framework's asynchronous processing has
 * completed).</p>
 *
 * @since 1.5.0
 */
public class HttpServletAuthorizationFilter implements Filter {

    private static final String INIT_PARAM_MODULE_FACTORY_CLASS = "authorization-module-factory-class";
    private static final String INIT_PARAM_MODULE_FACTORY_METHOD = "authorization-module-factory-method";
    private static final String INIT_PARAM_MODULE_FACTORY_METHOD_DEFAULT = "getAuthorizationModuleFactory";

    private final Logger logger = LoggerFactory.getLogger(HttpServletAuthorizationFilter.class);
    private final InitParamClassConstructor initParamClassConstructor;
    private final ResponseHandlerFactory responseHandlerFactory;

    private HttpServletAuthorizationModule module;

    /**
     * Default constructor called during normal Servlet Filter initialisation.
     */
    public HttpServletAuthorizationFilter() {
        this.initParamClassConstructor = new InitParamClassConstructor();
        this.responseHandlerFactory = new ResponseHandlerFactory();
    }

    /**
     * <p>Creates a new <p>HttpServletAuthorizationFilter</p> with the provided dependency instances.</p>
     *
     * <p>Used for test purposes. Do not use in production code.</p>
     *
     * @param initParamClassConstructor An instance of the {@link InitParamClassConstructor}.
     * @param responseHandlerFactory An instance of the {@link ResponseHandlerFactory}.
     */
    HttpServletAuthorizationFilter(InitParamClassConstructor initParamClassConstructor,
            ResponseHandlerFactory responseHandlerFactory) {
        this.initParamClassConstructor = initParamClassConstructor;
        this.responseHandlerFactory = responseHandlerFactory;
    }

    /**
     * <p>Initialises the filter by constructing an instance of the {@link AuthorizationModuleFactory} from the
     * properties defined in the {@code FilterConfig}.</p>
     *
     * <p>The class defined by the required 'authorization-module-factory-class' init param, must contain an accessible
     * static method with the name 'getAuthorizationModuleFactory' or the name specified by the
     * 'authorization-module-factory-method' init param. The static method can either have no args or a single
     * {@code FilterConfig} param.</p>
     *
     * @param filterConfig The {@code FilterConfig} instance containing the config used to initialise the filter.
     * @throws ServletException If the {@code FilterConfig} does not contain an init param named
     * 'authorization-module-factory-class' or the authorization module factory class could not be instantiated.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        AuthorizationModuleFactory authorizationModuleFactory = initParamClassConstructor.construct(filterConfig,
                INIT_PARAM_MODULE_FACTORY_CLASS, INIT_PARAM_MODULE_FACTORY_METHOD,
                INIT_PARAM_MODULE_FACTORY_METHOD_DEFAULT);

        if (authorizationModuleFactory == null) {
            logger.error("No AuthorizationModuleFactory class init param set.");
            throw new ServletException("No AuthorizationModuleFactory class init param set.");
        }

        module = authorizationModuleFactory.getAuthorizationModule();
    }

    /**
     * Each intercepted HTTP request/response will be passed to the configured {@link HttpServletAuthorizationModule} to
     * be authorized and if authorization succeeds will be allowed through to the requested resource.
     *
     * @param request The {@code HttpServletRequest} instance.
     * @param response The {@code HttpServletResponse} instance.
     * @param chain The {@code FilterChain} instance.
     * @throws ServletException If there is no {@code HttpServletAuthorizationModule} configured, if the request and
     * response are not for the HTTP protocol or if any problem occurs whilst performing the authorization.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {

        if (module == null) {
            throw new ServletException("Authorization Filter not correctly initialised. Missing Authorization "
                    + "Module.");
        }

        if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
            logger.error("Request and response are not HTTP request and response.");
            throw new ServletException("Request and response are not HTTP request and response.");
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        ResultHandler resultHandler = responseHandlerFactory.newSuccessHandler(req, resp, chain);
        ExceptionHandler exceptionHandler = responseHandlerFactory.newFailureHandler(resp);

        final Promise<Void, ServletException> promise = module.authorize(req, HttpAuthorizationContext.forRequest(req))
            .thenAsync(resultHandler, exceptionHandler);

        try {
            promise.getOrThrowUninterruptibly();       //TODO need to make async supported?...
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Nullifies the {@link HttpServletAuthorizationModule} in the filter.
     */
    @Override
    public void destroy() {
        module = null;
    }
}
