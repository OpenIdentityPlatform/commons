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

package org.forgerock.jaspi.context;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.auth.common.FilterConfiguration;
import org.forgerock.auth.common.FilterConfigurationImpl;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.runtime.config.ServerContextFactory;
import org.forgerock.jaspi.runtime.config.SessionServerContextFactory;
import org.forgerock.jaspi.runtime.context.ContextHandler;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.fluent.JsonValue;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.FilterConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of a factory class for getting the ServerAuthContext.
 *
 * @since 1.3.0
 */
public class DefaultServerContextFactory extends SessionServerContextFactory {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    /**
     * Constructs an instance of the DefaultServerContextFactory.
     *
     * @param config The instance of the FilterConfig.
     */
    private DefaultServerContextFactory(final FilterConfig config) {
        this(config, FilterConfigurationImpl.INSTANCE, ServerAuthModuleInstanceCreatorImpl.INSTANCE);
    }

    /**
     * Constructs an instances of the DefaultServerContextFactory, for test use.
     *
     * @param config The instance of the FilterConfig.
     * @param filterConfiguration An instance of the FilterConfiguration.
     * @param moduleInstanceCreator An instance of the ServerAuthModuleInstanceCreator.
     */
    DefaultServerContextFactory(final FilterConfig config, final FilterConfiguration filterConfiguration,
            ServerAuthModuleInstanceCreator moduleInstanceCreator) {
        super(config, filterConfiguration, moduleInstanceCreator);
    }

    /**
     * Returns a new instance of the DefaultServerContextFactory
     * <br/>
     * This does and should return a new instance of a ServerContextFactory, as it is only ever called once per
     * configured filter.
     *
     * @param config The instance of the FilterConfig.
     * @return A ServerContextFactory instance.
     */
    public static ServerContextFactory getServerContextFactory(final FilterConfig config) {
        return new DefaultServerContextFactory(config);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ServerAuthContext getServerAuthContext(final MessageInfoUtils messageInfoUtils,
            final CallbackHandler handler, final ContextHandler contextHandler, final JsonValue configuration,
            final ServerAuthModule sessionAuthModule) throws AuthException {

        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        if (configuration.isDefined(ModuleConfigurationFactory.AUTH_MODULES_KEY)) {
            if (!configuration.get(ModuleConfigurationFactory.AUTH_MODULES_KEY).isList()) {
                LOGGER.error("Auth Modules must be defined as a List.");
                throw new JaspiAuthException("Auth Modules must be defined as a List.");
            }
            for (JsonValue authModule : configuration.get(ModuleConfigurationFactory.AUTH_MODULES_KEY)) {
                String className = authModule.get(ModuleConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY).required()
                        .asString();
                Map<String, Object> moduleProperties =
                        authModule.get(ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY).asMap();
                ServerAuthModule serverAuthModule = getModuleInstanceCreator().construct(className,
                        moduleProperties, createRequestMessagePolicy(), handler);
                authModules.add(serverAuthModule);
            }
        }

        return new FallbackServerAuthContext(messageInfoUtils, contextHandler, sessionAuthModule, authModules);
    }
}
