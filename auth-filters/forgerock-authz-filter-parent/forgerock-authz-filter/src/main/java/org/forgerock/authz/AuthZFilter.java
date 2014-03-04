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
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.auth.common.FilterConfigurationImpl;
import org.forgerock.json.fluent.JsonValue;
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
 * @since 1.0.0
 */
public class AuthZFilter implements Filter {

    private static final String INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_CLASS = "logging-configurator-factory-class";
    private static final String INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_METHOD = "logging-configuration-factory-method";
    private static final String INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_METHOD_DEFAULT = "getLoggingConfigurator";

    private static final String INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_CLASS = "module-configurator-factory-class";
    private static final String INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_METHOD = "module-configuration-factory-method";
    private static final String INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_METHOD_DEFAULT = "getModuleConfigurator";

    private static final String INIT_PARAM_MODULE_CLASS = "module-class";

    private final InstanceCreator instanceCreator;
    private final FilterConfiguration filterConfiguration;

    private AuditLogger<HttpServletRequest> auditLogger;
    private AuthorizationModule authorizationModule;

    /**
     * Constructs a new instance of the AuthZFilter.
     */
    public AuthZFilter() {
        this(new InstanceCreator(), FilterConfigurationImpl.INSTANCE);
    }

    /**
     * Constructs a new instance of the AuthZFilter for use in tests.
     * <br/>
     * Allows tests to pass in a mock of the FilterConfiguration.
     *
     * @param instanceCreator An instance of the InstanceCreator.
     * @param filterConfiguration A mock of the FilterConfiguration.
     */
    protected AuthZFilter(final InstanceCreator instanceCreator, final FilterConfiguration filterConfiguration) {
        this.instanceCreator = instanceCreator;
        this.filterConfiguration = filterConfiguration;
    }

    /**
     * Initialises the instance of the Authorization Module that will be used to authorize requests passed through the
     * AuthZFilter.
     *
     * @param filterConfig {@inheritDoc}
     * @throws ServletException If the Authorization Module can not be created.
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        auditLogger = getAuditLogger(filterConfig);
        if (auditLogger == null) {
            throw new ServletException("AuditLogger cannot be null.");
        }

        authorizationModule = initAuthorizationModule(filterConfig);

        if (authorizationModule == null) {
            throw new ServletException("AuthorizationModule must be configured in web.xml by either "
                    + "'module-configuration-factory-class' param or 'module-class' param. See documentation for more "
                    + "details.");
        }
    }

    /**
     * Gets the AuditLogger from the AuthorizationLoggingConfigurator instance specified in the web.xml for the filter
     * instance.
     *
     * @param filterConfig The filter config.
     * @return The AuditLogger instance.
     * @throws ServletException If there is an error getting the AuditLogger.
     */
    private AuditLogger<HttpServletRequest> getAuditLogger(final FilterConfig filterConfig) throws ServletException {

        final AuthorizationLoggingConfigurator loggingConfigurator = filterConfiguration.get(filterConfig,
                INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_CLASS, INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_METHOD,
                INIT_PARAM_LOGGING_CONFIGURATOR_FACTORY_METHOD_DEFAULT);

        if (loggingConfigurator == null) {
            throw new ServletException("AuditLogger must be configured.");
        }
        return loggingConfigurator.getAuditLogger();
    }

    /**
     * Gets an instance of the Authorization Module and configures it.
     * <br/>
     * First it tries to get an instance of the AuthorizationModule from the AuthorizationModuleConfigurator, if
     * configured in the web.xml for the filter instance. If it has been configured will also get the JsonValue
     * configuration from the AuthorizationModuleConfigurator and use that in the call to the modules initialise method.
     * <br/>
     * If the AuthorizationModuleConfigurator has not been configured then an instance of the class specified in the
     * web.xml init-param, 'module-class', will be created and the initialise method will be called with an empty
     * JsonValue.
     * <br/>
     * If the Authorization Module has not been configured in either or these ways then <code>null</code> is returned.
     *
     * @param filterConfig The filter config.
     * @return An initialised instance of an Authorization Module or <code>null</code>.
     * @throws ServletException If there is an error  the AuditLogger.
     */
    private AuthorizationModule initAuthorizationModule(final FilterConfig filterConfig) throws ServletException {

        final AuthorizationModuleConfigurator moduleConfigurator = filterConfiguration.get(filterConfig,
                INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_CLASS,INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_METHOD,
                INIT_PARAM_MODULE_CONFIGURATOR_FACTORY_METHOD_DEFAULT);

        final AuthorizationModule authorizationModule;
        if (moduleConfigurator != null) {
            authorizationModule = moduleConfigurator.getModule();
            authorizationModule.initialise(moduleConfigurator.getConfiguration());
            return authorizationModule;
        }

        final String authorizationModuleClassName = filterConfig.getInitParameter(INIT_PARAM_MODULE_CLASS);
        if (authorizationModuleClassName == null) {
            return null;
        }
        try {
            authorizationModule = instanceCreator.createInstance(authorizationModuleClassName,                    AuthorizationModule.class);
            authorizationModule.initialise(JsonValue.json(JsonValue.object()));
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (InstantiationException e) {
            throw new ServletException(e);
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        }

        return authorizationModule;
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

        if (!(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("Request/response must be of types HttpServletRequest and HttpServletResponse");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        AuthorizationContext context = AuthorizationContext.forRequest(request);

        if (authorizationModule.authorize(request, context)) {
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
     * Chain through to the authorizationFilter's destroy
     */
    @Override
    public void destroy() {
        authorizationModule.destroy();
    }
}
