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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.runtime.config.inject.DefaultRuntimeInjector;
import org.forgerock.jaspi.runtime.config.inject.RuntimeInjector;
import org.forgerock.jaspi.utils.FilterConfiguration;
import org.forgerock.jaspi.utils.FilterConfigurationImpl;

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
 * This Servlet Filter class provides a method of integrating the Jaspi runtime into a Servlet Container, such that
 * requests made to the url this filter is registered at will cause the Jaspi runtime to validate and secure the
 * request and response using it configured ServerAuthModules.
 * <p>
 * To configure the Jaspi runtime the Servlet Filter registration must contain the configuration for the
 * LoggingConfigurator.
 *
 * @since 1.3.0
 */
public class JaspiRuntimeFilter implements Filter {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    private static final String INIT_PARAM_INJECTOR_CLASS = "runtime-injector-class";
    private static final String INIT_PARAM_INJECTOR_METHOD = "runtime-injector-method";
    private static final String INIT_PARAM_INJECTOR_METHOD_DEFAULT = "getRuntimeInjector";

    private final FilterConfiguration filterConfiguration;

    private FilterConfig filterConfig;
    private JaspiRuntime jaspiRuntime;

    /**
     * Constructs a new instance of the JaspiRuntimeFilter.
     */
    public JaspiRuntimeFilter() {
        this(FilterConfigurationImpl.INSTANCE);
    }

    /**
     * Constructs a new instance of the JaspiRuntimeFilter for use in tests.
     * <p>
     * Allows tests to pass in a mock of the FilterConfiguration.
     *
     * @param filterConfiguration A mock of the FilterConfiguration.
     */
    protected JaspiRuntimeFilter(final FilterConfiguration filterConfiguration) {
        this.filterConfiguration = filterConfiguration;
    }

    /**
     * Initialises the filter with the provided filter configuration.
     *
     * @param filterConfig The filter config.
     */
    @Override
    public void init(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Lazy initialises the Jaspi runtime, using the config classes provided in the init-param filter config.
     *
     * @return The instance of the Jaspi Runtime.
     * @throws ServletException If there is an error configuring the Jaspi Runtime.
     */
    private JaspiRuntime getJaspiRuntime() throws ServletException {
        if (jaspiRuntime == null) {
            LOGGER.debug("Initialising the JaspiRuntime");
            RuntimeInjector runtimeInjector = getRuntimeInjector(filterConfig);
            jaspiRuntime = runtimeInjector.getInstance(JaspiRuntime.class);
        }
        return jaspiRuntime;
    }

    /**
     * On receipt of a request the Jaspi runtime is invoked to validate the request and secure the response, (providing
     * validation was successful).
     *
     * @param servletRequest The ServletRequest. MUST be of type HttpServletRequest.
     * @param servletResponse The ServletResponse. MUST be of type HttpServletResponse.
     * @param chain The filter chain.
     * @throws IOException If there is an exception thrown from the Jaspi runtime.
     * @throws ServletException If there is an exception thrown from the Jaspi runtime.
     */
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain chain) throws IOException, ServletException {

        if ((!HttpServletRequest.class.isAssignableFrom(servletRequest.getClass())
                || !HttpServletResponse.class.isAssignableFrom(servletResponse.getClass()))) {
            LOGGER.error("Unsupported protocol");
            throw new ServletException("Unsupported protocol");
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        getJaspiRuntime().processMessage(request, response, chain);
    }

    /**
     * Gets the instance of the configured {@link RuntimeInjector}, configured in the Filter Config init params.
     * <p>
     * If no RuntimeInjector is configured in the Filter Config, then the {@link DefaultRuntimeInjector} will be
     * returned.
     *
     * @param config The Filter Config.
     * @return An instance of the RuntimeInjector.
     * @throws ServletException If there is an error reading the Filter Config.
     */
    private RuntimeInjector getRuntimeInjector(final FilterConfig config) throws ServletException {
        RuntimeInjector runtimeInjector = filterConfiguration.get(config, INIT_PARAM_INJECTOR_CLASS,
                INIT_PARAM_INJECTOR_METHOD, INIT_PARAM_INJECTOR_METHOD_DEFAULT);

        if (runtimeInjector == null) {
            LOGGER.debug("Filter init param, " + INIT_PARAM_INJECTOR_CLASS + ", not set. Falling back to the "
                    + DefaultRuntimeInjector.class.getSimpleName() + ".");
            runtimeInjector = DefaultRuntimeInjector.getRuntimeInjector(config);
        }

        return runtimeInjector;
    }

    /**
     * Nullifies any state in the filter.
     */
    @Override
    public void destroy() {
        jaspiRuntime = null;
    }
}
