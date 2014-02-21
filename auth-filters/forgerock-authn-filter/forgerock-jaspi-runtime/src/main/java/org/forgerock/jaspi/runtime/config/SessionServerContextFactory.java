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

package org.forgerock.jaspi.runtime.config;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.context.ServerAuthModuleInstanceCreator;
import org.forgerock.jaspi.context.ServerAuthModuleInstanceCreatorImpl;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.auth.common.FilterConfigurationImpl;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.util.Map;

/**
 * Factory class for getting the ServerAuthContext instance that the JaspiRuntime will be configured to use.
 * <br/>
 * Configures the Session AuthModule for the ServerAuthContext and then delegates the configuration of the list
 * of ServerAuthModules and creation of the ServerAuthContext instance to its concrete sub-type.
 * <br/>
 * Uses Json to model the configuration for the ServerAuthContext.
 *
 * @since 1.3.0
 */
public abstract class SessionServerContextFactory implements ServerContextFactory {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    private static final String INIT_PARAM_CONTEXT_CLASS = "module-configuration-factory-class";
    private static final String INIT_PARAM_CONTEXT_METHOD = "module-configuration-factory-method";
    private static final String INIT_PARAM_CONTEXT_METHOD_DEFAULT = "getModuleConfigurationFactory";

    private final FilterConfig config;
    private final FilterConfiguration filterConfiguration;
    private final ServerAuthModuleInstanceCreator moduleInstanceCreator;

    /**
     * Constructs an instance of the SessionServerContextFactory.
     *
     * @param config The instance of the FilterConfig.
     */
    public SessionServerContextFactory(final FilterConfig config) {
        this(config, FilterConfigurationImpl.INSTANCE, ServerAuthModuleInstanceCreatorImpl.INSTANCE);
    }

    /**
     * Constructs an instances of the SessionServerContextFactory, for test use.
     *
     * @param config The instance of the FilterConfig.
     * @param filterConfiguration An instance of the FilterConfiguration.
     * @param moduleInstanceCreator An instance of the ServerAuthModuleInstanceCreator.
     */
    protected SessionServerContextFactory(final FilterConfig config, final FilterConfiguration filterConfiguration,
            final ServerAuthModuleInstanceCreator moduleInstanceCreator) {
        this.config = config;
        this.filterConfiguration = filterConfiguration;
        this.moduleInstanceCreator = moduleInstanceCreator;
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Gets the Json configuration from the Filter Config and configures the Session AuthModule, if defined in
     * configuration. Then passes onto concrete sub-class to configure the list of ServerAuthModules and creating
     * the instance of the ServerAuthContext that the Jaspi Runtime will be configured with.
     *
     * @param messageInfoUtils {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param contextHandler {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ServerAuthContext getServerAuthContext(final MessageInfoUtils messageInfoUtils,
            final CallbackHandler handler, final ContextHandler contextHandler) throws AuthException {

        ModuleConfigurationFactory configurationFactory;
        try {
            configurationFactory = filterConfiguration.get(config,
                    INIT_PARAM_CONTEXT_CLASS, INIT_PARAM_CONTEXT_METHOD, INIT_PARAM_CONTEXT_METHOD_DEFAULT);
        } catch (ServletException e) {
            LOGGER.error("Failed to get ModuleConfigurationFactory from Servlet Filter init params", e);
            throw new JaspiAuthException("Failed to get ModuleConfigurationFactory from Servlet Filter init params", e);
        }

        JsonValue configuration = configurationFactory.getConfiguration()
                .get(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY).required();

        ServerAuthModule sessionAuthModule = null;
        if (configuration.isDefined(ModuleConfigurationFactory.SESSION_MODULE_KEY)) {
            JsonValue sessionModule = configuration.get(ModuleConfigurationFactory.SESSION_MODULE_KEY);
            if (!sessionModule.isMap()) {
                LOGGER.error("Only one Session Auth Module can be defined.");
                throw new JaspiAuthException("Only one Session Auth Module can be defined.");
            }
            String className = sessionModule.get(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY).required()
                    .asString();
            Map<String, Object> moduleProperties = sessionModule.get(
                    ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY).asMap();
            sessionAuthModule = moduleInstanceCreator.construct(className, moduleProperties,
                    createRequestMessagePolicy(), handler);
        }

        return getServerAuthContext(messageInfoUtils, handler, contextHandler, configuration, sessionAuthModule);
    }

    /**
     * Returns the ServerAuthContext instance that the JaspiRuntime will be configured to use.
     * <br/>
     * Responsible for configuring the list of ServerAuthModules and creating the instance of the ServerAuthContext
     * that the Jaspi Runtime will be configured with.
     *
     * @param messageInfoUtils An instance of the MessageInfoUtils.
     * @param handler An instance of a CallbackHandler.
     * @param contextHandler An instance of the ContextHandler.
     * @param configuration The Json configuration.
     * @param sessionAuthModule The configured Session AuthModule.
     * @return A ServerAuthContext instance.
     * @throws AuthException If there is an error getting the ServerAuthContext instance.
     */
    protected abstract ServerAuthContext getServerAuthContext(final MessageInfoUtils messageInfoUtils,
            final CallbackHandler handler, final ContextHandler contextHandler, final JsonValue configuration,
            final ServerAuthModule sessionAuthModule) throws AuthException;

    /**
     * Returns the ServerAuthModuleInstanceCreator instance.
     *
     * @return The ServerAuthModuleInstanceCreator.
     */
    protected ServerAuthModuleInstanceCreator getModuleInstanceCreator() {
        return moduleInstanceCreator;
    }

    /**
     * Creates a MessagePolicy instance for the Request Policy, using the AUTHENTICATE_SENDER ProtectionPolicy and
     * ensuring the message policy is mandatory.
     *
     * @return A MessagePolicy.
     */
    protected MessagePolicy createRequestMessagePolicy() {
        MessagePolicy.Target[] targets = new MessagePolicy.Target[]{};
        MessagePolicy.ProtectionPolicy protectionPolicy = new MessagePolicy.ProtectionPolicy() {
            @Override
            public String getID() {
                return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
            }
        };
        MessagePolicy.TargetPolicy targetPolicy = new MessagePolicy.TargetPolicy(targets, protectionPolicy);
        MessagePolicy.TargetPolicy[] targetPolicies = new MessagePolicy.TargetPolicy[]{targetPolicy};
        return new MessagePolicy(targetPolicies, true);
    }
}
