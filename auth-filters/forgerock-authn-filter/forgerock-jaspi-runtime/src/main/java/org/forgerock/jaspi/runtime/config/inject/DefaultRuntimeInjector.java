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

package org.forgerock.jaspi.runtime.config.inject;

import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.auth.common.FilterConfigurationImpl;
import org.forgerock.jaspi.context.DefaultServerContextFactory;
import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.jaspi.runtime.HttpServletCallbackHandler;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.runtime.RuntimeResultHandler;
import org.forgerock.jaspi.runtime.config.ServerContextFactory;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import static org.forgerock.jaspi.runtime.JaspiRuntime.LOG;

/**
 * Default implementation of the RuntimeInjector interface which will allow for getting the instance of the
 * JaspiRuntime.
 *
 * @since 1.3.0
 */
public class DefaultRuntimeInjector implements RuntimeInjector {

    private static final String INIT_PARAM_CONTEXT_CLASS = "context-factory-class";
    private static final String INIT_PARAM_CONTEXT_METHOD = "context-factory-method";
    private static final String INIT_PARAM_CONTEXT_METHOD_DEFAULT = "getServerContextFactory";
    private static final String INIT_PARAM_AUDIT_API_CLASS = "audit-api-class";
    private static final String INIT_PARAM_AUDIT_API_METHOD = "audit-api-method";
    private static final String INIT_PARAM_AUDIT_API_METHOD_DEFAULT = "getAuditApi";

    private final FilterConfiguration filterConfiguration;

    private final JaspiRuntime jaspiRuntime;

    /**
     * Constructs a new instance of the DefaultRuntimeInjector.
     *
     * @param config The instance of the FilterConfig.
     * @throws ServletException If there is an error reading the Filter Config.
     * @throws AuthException If there is an error getting the ServerAuthContext instance.
     */
    private DefaultRuntimeInjector(final FilterConfig config) throws ServletException, AuthException {
        this(config, FilterConfigurationImpl.INSTANCE);
    }

    /**
     * Constructs a new instance of the DefaultRuntimeInjector, for test use.
     *
     * @param config The instance of the FilterConfig.
     * @param filterConfiguration An instance of the FilterConfiguration.
     * @throws ServletException If there is an error reading the Filter Config.
     * @throws AuthException If there is an error getting the ServerAuthContext instance.
     */
    DefaultRuntimeInjector(final FilterConfig config, final FilterConfiguration filterConfiguration)
            throws ServletException, AuthException {

        this.filterConfiguration = filterConfiguration;

        ServerContextFactory serverContextFactory = getServerContextFactory(config);

        MessageInfoUtils messageInfoUtils = new MessageInfoUtils();
        HttpServletCallbackHandler callbackHandler = new HttpServletCallbackHandler();
        ContextHandler contextHandler = new ContextHandler(messageInfoUtils);
        AuditApi auditApi = getAuditApi(config);
        ServerAuthContext serverAuthContext = serverContextFactory.getServerAuthContext(messageInfoUtils,
                callbackHandler, contextHandler);

        RuntimeResultHandler runtimeResultHandler = new RuntimeResultHandler();

        this.jaspiRuntime = new JaspiRuntime(serverAuthContext, runtimeResultHandler, auditApi);
        LOG.debug("Finished initialising the DefaultRuntimeInjector");
    }

    /**
     * Returns a new instance of the DefaultRuntimeInjector.
     * <br/>
     * This does and should return a new instance of a RuntimeInjector, as it is only ever called once per configured
     * filter. This means that a web application can create multiple instances of the JaspiRuntimeFilter and each
     * instance will have its own self contained injector which uses that instances filter configuration.
     *
     * @param config The instance of the FilterConfig.
     * @return A RuntimeInjector.
     * @throws ServletException If there is an error creating the RuntimeInjector.
     */
    public static RuntimeInjector getRuntimeInjector(final FilterConfig config) throws ServletException {
        try {
            return new DefaultRuntimeInjector(config);
        } catch (AuthException e) {
            LOG.error("Failed to construct RuntimeInjector", e);
            throw new ServletException("Failed to construct RuntimeInjector", e);
        }
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Will only ever return the singleton instance (for this instance of the JaspiRuntimeFilter) and if asked
     * for an instance of a different type will throw a {@link RuntimeException}.
     *
     * @param type {@inheritDoc}
     * @param <T> {@inheritDoc}
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(final Class<T> type) {

        if (!JaspiRuntime.class.equals(type)) {
            LOG.error("Type not registered! {}", type.getName());
            throw new RuntimeException("Type not registered! " + type.getName());
        }

        return (T) jaspiRuntime;
    }

    /**
     * Gets the instance of the configured {@link ServerContextFactory}, configured in the Filter Config init params.
     * <br/>
     * If no ServerContextFactory is configured in the Filter Config, then the {@link DefaultServerContextFactory} will
     * be returned.
     *
     * @param config The Filter Config.
     * @return An instance of the ServerContextFactory.
     * @throws ServletException If there is an error reading the Filter Config.
     */
    private ServerContextFactory getServerContextFactory(final FilterConfig config) throws ServletException {

        ServerContextFactory serverContextFactory = filterConfiguration.get(config, INIT_PARAM_CONTEXT_CLASS,
                INIT_PARAM_CONTEXT_METHOD, INIT_PARAM_CONTEXT_METHOD_DEFAULT);

        if (serverContextFactory == null) {
            LOG.debug("Filter init param, {}, not set. Falling back to the .", INIT_PARAM_CONTEXT_CLASS,
                    DefaultServerContextFactory.class.getSimpleName());
            return DefaultServerContextFactory.getServerContextFactory(config);
        }

        return serverContextFactory;
    }

    /**
     * Gets the {@code AuditApi} instance from the filter configuration. If none defined a NoOp implementation will be
     * returned.
     */
    private AuditApi getAuditApi(FilterConfig config) throws ServletException {
        AuditApi auditApi = filterConfiguration.get(config, INIT_PARAM_AUDIT_API_CLASS, INIT_PARAM_AUDIT_API_METHOD,
                INIT_PARAM_AUDIT_API_METHOD_DEFAULT);

        if (auditApi == null) {
            LOG.debug("Filter init param, {}, not set. Falling back to the NoOp Audit Api.",
                    INIT_PARAM_AUDIT_API_CLASS);
            auditApi = new AuditApi() {
                @Override
                public void audit(JsonValue auditMessage) {
                }
            };
        }

        return auditApi;
    }
}
