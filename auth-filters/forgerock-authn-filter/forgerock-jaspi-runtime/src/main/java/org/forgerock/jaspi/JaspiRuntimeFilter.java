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

package org.forgerock.jaspi;

import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.jaspi.runtime.ContextFactory;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.runtime.RuntimeResultHandler;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * <p>This Servlet Filter class provides a method of integrating the Jaspi runtime into a Servlet Container, such that
 * requests made to the url this filter is registered at will cause the Jaspi runtime to validate and secure the
 * request and response using it configured {@code ServerAuthModule}s.</p>
 *
 * <p>To configure the Jaspi runtime the Servlet Filter registration must contain the configuration for the
 * {@link ContextFactory} and the {@link AuditApi}.</p>
 *
 * @since 1.3.0
 */
public class JaspiRuntimeFilter implements Filter {

    private static final String INIT_PARAM_CONTEXT_FACTORY_CLASS = "context-factory-class";
    private static final String INIT_PARAM_CONTEXT_FACTORY_METHOD = "context-factory-method";
    private static final String INIT_PARAM_CONTEXT_FACTORY_METHOD_DEFAULT = "getContextFactory";

    private static final String INIT_PARAM_AUDIT_API_CLASS = "audit-api-factory-class";
    private static final String INIT_PARAM_AUDIT_API_METHOD = "audit-api-factory-method";
    private static final String INIT_PARAM_AUDIT_API_METHOD_DEFAULT = "getAuditApi";

    private ContextFactory contextFactory;
    private AuditApi auditApi;
    private JaspiRuntime runtime;

    /**
     * Default constructor called during normal Filter initialization. Implementations MUST override
     * {@link #getContextFactory(FilterConfig)} and {@link #getAuditApi(FilterConfig)} in order for the HTTP Servlet to
     * function.
     */
    public JaspiRuntimeFilter() {
        this.contextFactory = null;
        this.auditApi = null;
    }

    /**
     * <p>Creates a new Jaspi HTTP Filter with the provided context factory and audit api. This constructor is provided
     * as a convenience for cases where the HTTP Filter is instantiated programmatically.</p>
     *
     * <p>If the HTTP Filter is created using this constructor then there is no need to override
     * {@link #getContextFactory(FilterConfig)} or {@link #getAuditApi(FilterConfig)}.</p>
     *
     * @param contextFactory The context factory.
     * @param auditApi The audit api.
     */
    public JaspiRuntimeFilter(ContextFactory contextFactory, AuditApi auditApi) {
        this.contextFactory = contextFactory;
        this.auditApi = auditApi;
    }

    /**
     * Initialises the filter with the provided filter configuration.
     *
     * @param config The filter config.
     * @throws ServletException If either the {@code ContextFactory} or {@code AuditApi} instance could not be created.
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        if (contextFactory == null) {
            contextFactory = getContextFactory(config);
        }
        if (auditApi == null) {
            auditApi = getAuditApi(config);
        }
        this.runtime = new JaspiRuntime(contextFactory, new RuntimeResultHandler(), auditApi);
    }

    /**
     * On receipt of a request the Jaspi runtime is invoked to validate the request and secure the response, (providing
     * validation was successful).
     *
     * @param request The ServletRequest. MUST be of type HttpServletRequest.
     * @param response The ServletResponse. MUST be of type HttpServletResponse.
     * @param chain The filter chain.
     * @throws ServletException If there is an exception thrown from the Jaspi runtime.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {

        if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
            throw new ServletException("Non HTTP request");
        }

        runtime.processMessage((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private ContextFactory getContextFactory(FilterConfig config) throws ServletException {

        if (config != null) {
            // Check for configured connection factory class first.
            final String className = config.getInitParameter(INIT_PARAM_CONTEXT_FACTORY_CLASS);
            if (className != null) {
                try {
                    final Class<?> cls = Class.forName(className);
                    final String tmp = config.getInitParameter(INIT_PARAM_CONTEXT_FACTORY_METHOD);
                    final String methodName = tmp != null ? tmp : INIT_PARAM_CONTEXT_FACTORY_METHOD_DEFAULT;
                    try {
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (ContextFactory) factoryMethod.invoke(null, config);
                    } catch (final IllegalArgumentException e) {
                        // Try no-arg method.
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (ContextFactory) factoryMethod.invoke(null);
                    }
                } catch (final Exception e) {
                    throw new ServletException(e);
                }
            }
        }

        // FIXME: i18n
        throw new ServletException("Unable to initialize ContextFactory");
    }

    private AuditApi getAuditApi(FilterConfig config) throws ServletException {

        if (config != null) {
            // Check for configured connection factory class first.
            final String className = config.getInitParameter(INIT_PARAM_AUDIT_API_CLASS);
            if (className != null) {
                try {
                    final Class<?> cls = Class.forName(className);
                    final String tmp = config.getInitParameter(INIT_PARAM_AUDIT_API_METHOD);
                    final String methodName = tmp != null ? tmp : INIT_PARAM_AUDIT_API_METHOD_DEFAULT;
                    try {
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (AuditApi) factoryMethod.invoke(null, config);
                    } catch (final IllegalArgumentException e) {
                        // Try no-arg method.
                        final Method factoryMethod = cls.getDeclaredMethod(methodName);
                        return (AuditApi) factoryMethod.invoke(null);
                    }
                } catch (final Exception e) {
                    throw new ServletException(e);
                }
            }
        }

        // FIXME: i18n
        throw new ServletException("Unable to initialize AuditApi");
    }

    @Override
    public void destroy() {
        //TODO what if context is still processing something?...
    }
}
