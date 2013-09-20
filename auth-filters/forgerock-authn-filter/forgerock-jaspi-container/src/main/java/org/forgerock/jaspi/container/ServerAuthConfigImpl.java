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

package org.forgerock.jaspi.container;

import org.forgerock.jaspi.filter.AuthNFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the configuration of ServerAuthModules at a given message layer, and for a particular application context.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class ServerAuthConfigImpl implements ServerAuthConfig {

    private static final Logger DEBUG = LoggerFactory.getLogger(ServerAuthConfigImpl.class);

    private final String layer;
    private final String appContext;
    private final CallbackHandler handler;
    private final Map<String, ServerAuthContext> serverAuthContextMap = new HashMap<String, ServerAuthContext>();

    private Map<String, Map<String, Object>> contexts;

    /**
     * Constructs an instance of the ServerAuthConfigImpl.
     *
     * @param layer The layer this ServerAuthConfig should be registered at.
     * @param appContext The appContext this ServerAuthConfig shoud be registered at.
     * @param handler The CallbackHandler instance.
     */
    public ServerAuthConfigImpl(String layer, String appContext, CallbackHandler handler) {
        this.layer = layer;
        this.appContext = appContext;
        this.handler = handler;

        contexts = new HashMap<String, Map<String, Object>>();
    }

    /**
     * Registers an auth context with the provided context properties.
     *
     * @param authContextID The auth context ID used to identify this auth context.
     * @param contextProperties The properties that will be passed to the AuthModule instance.
     */
    public void registerAuthContext(String authContextID, Map<String, Object> contextProperties) {
        contexts.put(authContextID, contextProperties);
    }

    /**
     * Gets a ServerAuthContextImpl instance, initialised with the Authentication modules defined by the
     * authContextID and a map of property key pairs.
     *
     * @param authContextID An identifier used to index the provided config, or null. This value must be identical to
     *                      the value returned by the getAuthContextID method for all MessageInfo objects passed to the
     *                      validateRequest method of the returned ServerAuthContext.
     * @param serviceSubject A Subject that represents the source of the service response to be secured by the acquired
     *                       authentication context. A null value may be passed for this parameter.
     * @param properties A Map object that may be used by the caller to augment the properties that will be passed to
     *                   the encapsulated modules at module initialization. The null value may be passed for this
     *                   parameter.
     * @return A ServerAuthContext instance that encapsulates the ServerAuthModules used to secure and validate
     *          requests/responses associated with the given authContextID, or null (indicating that no modules are
     *          configured).
     * @throws AuthException If this method fails.
     */
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties)
            throws AuthException {

        ServerAuthContext serverAuthContext = serverAuthContextMap.get(authContextID);
        if (serverAuthContext == null) {
            Map<String, Object> contextProperties = contexts.get(authContextID);

            // Verify that a context was found
            if (contextProperties == null || contextProperties.size() == 0) {
                throw new AuthException("No auth context found for authContextID: " + authContextID);
            }

            // Create the modules
            List<ServerAuthModule> serverAuthModules = new ArrayList<ServerAuthModule>(2);
            Map<String, String> sessionModule = (Map<String, String>) contextProperties.get("sessionModule");
            ServerAuthModule sessionAuthModule = createModule(sessionModule);

            List<Map<String, String>> authModules = (List<Map<String, String>>) contextProperties.get("authModules");
            if (authModules != null) {
                for (Map<String, String> authModule : authModules) {
                    ServerAuthModule serverAuthModule = createModule(authModule);
                    if (serverAuthModule != null) {
                        serverAuthModules.add(serverAuthModule);
                    }
                }
            }

            // Now create the context
            serverAuthContext = new ServerAuthContextImpl(sessionAuthModule, serverAuthModules,
                    createRequestMessagePolicy(), null, properties, handler);
            serverAuthContextMap.put(authContextID, serverAuthContext);
        }

        return serverAuthContext;
    }

    /**
     * Creates an instance of the Server Auth Module, using the class name in the given module properties and the
     * remaining module properties to initialise the Server Auth Module.
     *
     * @param moduleProperties The module properties.
     * @return The ServerAuthModule instance.
     * @throws AuthException If there is an error creating the ServerAuthModule.
     */
    private ServerAuthModule createModule(Map<String, String> moduleProperties) throws AuthException {
        if (moduleProperties == null) {
            return null;
        }

        String className = moduleProperties.get("className");

        if (className == null || !className.isEmpty()) {
            try {
                ServerAuthModule module = (ServerAuthModule) Class.forName(className).newInstance();
                module.initialize(createRequestMessagePolicy(), null, handler, moduleProperties);
                DEBUG.debug("Created module, className: {}", className);
                return module;
            } catch (ClassNotFoundException e) {
                DEBUG.debug("Failed to instantiate module, className: {}", className);
                throw new AuthException("Failed to instantiate module, className: " + className);
            } catch (IllegalAccessException e) {
                DEBUG.debug("Failed to instantiate module, className: {}", className);
                throw new AuthException("Failed to instantiate module, className: " + className);
            } catch (InstantiationException e) {
                DEBUG.debug("Failed to instantiate module, className: {}", className);
                throw new AuthException("Failed to instantiate module, className: " + className);
            }
        }

        return null;
    }

    /**
     * Creates a MessagePolicy instance for the Request Policy, using the AUTHENTICATE_SENDER ProtectionPolicy and
     * ensuring the message policy is mandatory.
     *
     * @return A MessagePolicy.
     */
    private MessagePolicy createRequestMessagePolicy() {
        MessagePolicy.Target[] targets = new MessagePolicy.Target[]{};
        MessagePolicy.ProtectionPolicy protectionPolicy = new ProtectionPolicyImpl();
        MessagePolicy.TargetPolicy targetPolicy = new MessagePolicy.TargetPolicy(targets, protectionPolicy);
        MessagePolicy.TargetPolicy[] targetPolicies = new MessagePolicy.TargetPolicy[]{targetPolicy};
        return new MessagePolicy(targetPolicies, true);
    }

    /**
     * Get the message layer name.
     * @return The message layer name of this configuration object, or null.
     */
    public String getMessageLayer() {
        return layer;
    }

    /**
     * Get the application context identifier of this authentication context configuration object.
     * @return The String identifying the application context of this configuration object, or null.
     */
    public String getAppContext() {
        return appContext;
    }

    /**
     * Get the authentication context identifier corresponding to the request and response objects encapsulated in
     * messageInfo. This is constructed using the moduleChain configured in the filter.
     *
     * @param messageInfo A contextual Object that encapsulates the client request and server response objects.
     * @return The authentication context identifier corresponding to the encapsulated request and response objects,
     *          or null.
     */
    public String getAuthContextID(MessageInfo messageInfo) {
        return (String) messageInfo.getMap().get(AuthNFilter.MODULE_CONFIGURATION_PROPERTY);
    }

    /**
     * No state to refresh so this method does nothing.
     */
    public void refresh() {
        // No state to refresh
    }

    /**
     * Used to determine whether the authentication context configuration object encapsulates any protected
     * authentication contexts.
     *
     * @return false.
     */
    public boolean isProtected() {
        return false;
    }
}
