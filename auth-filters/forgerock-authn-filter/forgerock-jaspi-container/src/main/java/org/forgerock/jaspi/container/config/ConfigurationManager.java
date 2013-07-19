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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container.config;

import org.forgerock.jaspi.container.AuditLogger;
import org.forgerock.jaspi.container.AuditLoggerHolder;
import org.forgerock.jaspi.container.AuthConfigFactoryImpl;
import org.forgerock.jaspi.container.AuthConfigProviderImpl;
import org.forgerock.jaspi.container.ServerAuthConfigImpl;
import org.forgerock.jaspi.container.callback.CallbackHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;

/**
 * Responsible for configuring the Auth Contexts for the Authentication Filter.
 */
public final class ConfigurationManager {

    private final static Logger DEBUG = LoggerFactory.getLogger(ConfigurationManager.class);

    private static boolean configured = false;

    /**
     * Private default constructor.
     */
    private ConfigurationManager() {
    }

    /**
     * Configures the Authentication Filter with the given Auth Context configuration.
     *
     * Can only be called once, unless the unconfigure method is called.
     *
     * @param authContextConfiguration The Configuration object containing the Auth Context configuration.
     * @throws AuthException If there is a problem configuring the Authentication Filter.
     */
    public static synchronized void configure(Configuration authContextConfiguration) throws AuthException {

        if (!configured) {
            CallbackHandler callbackHandler = new CallbackHandlerImpl();
            ServerAuthConfigImpl serverAuthConfig = new ServerAuthConfigImpl(null, null, callbackHandler);
            AuditLogger auditLogger = createAuditLogger(
                    authContextConfiguration.getAuditLoggerClassName());
            AuditLoggerHolder.INSTANCE.setAuthenticationAuditLogger(auditLogger);

            for (String authContextId : authContextConfiguration.keySet()) {
                serverAuthConfig.registerAuthContext(authContextId, authContextConfiguration.get(authContextId));
            }

            // Now assemble the factory-provider-config-context-module structure
            AuthConfigFactory authConfigFactory = AuthConfigFactoryImpl.getInstance();
            AuthConfigProviderImpl authConfigProvider = new AuthConfigProviderImpl(null, null);
            authConfigProvider.setServerAuthConfig(serverAuthConfig);
            authConfigFactory.registerConfigProvider(authConfigProvider, null, null, null);

            configured = true;
        } else {
            throw new AuthException("JASPI Authn Filter already configured.");
        }
    }

    /**
     * Sets the configured flag to false.
     */
    public static synchronized void unconfigure() {
        configured = false;
    }

    /**
     * Creates an instance of a AuditLogger, if the class name is null then null is returned.
     *
     * @param auditLoggerClassName The class name of the AuditLogger implementation.
     * @return An instance of a AuditLogger
     * @throws AuthException If the AuditLogger could not be created.
     */
    private static AuditLogger createAuditLogger(String auditLoggerClassName)
            throws AuthException {

        if (auditLoggerClassName == null) {
            return null;
        }

        try {
            return ((Class<? extends AuditLogger>) Class.forName(auditLoggerClassName)).newInstance();
        } catch (InstantiationException e) {
            DEBUG.error("Could not create AuditLogger", e);
            throw new AuthException(e.getMessage());
        } catch (IllegalAccessException e) {
            DEBUG.error("Could not create AuditLogger", e);
            throw new AuthException(e.getMessage());
        } catch (ClassNotFoundException e) {
            DEBUG.error("Could not create AuditLogger", e);
            throw new AuthException(e.getMessage());
        }
    }
}
