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

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ServerAuthConfig;
import java.util.Map;

/**
 * This provider is to be used to obtain an ServerAuthConfigImpl instance, which is used to obtain authentication
 * context configuration objects. The provider must be populated with a single ServerAuthConfig in some
 * context-specific manner.
 *
 * The following represents a typical sequence of calls for obtaining a server authentication context object, and then
 * using it to secure a request.
 *
 * <ol>
 *     <li>AuthConfigProvider provider;</li>
 *     <li>ClientAuthConfig config = provider.getClientAuthConfig(layer,appID,cbh);</li>
 *     <li>String authContextID = config.getAuthContextID(messageInfo);</li>
 *     <li>ClientAuthContext context = config.getAuthContext(authContextID,subject,properties);</li>
 *     <li>context.secureRequest(messageInfo,subject);</li>
 * </ol>
 *
 * The initialization component typically performs these calls to set up the provider.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class AuthConfigProviderImpl implements AuthConfigProvider {

    private ServerAuthConfig serverAuthConfig;

    /**
     * Required constructor for instantiation by an AuthConfigFactory.
     *
     * @param properties A Map object containing the initialization properties for the provider. This argument may be
     *                   null. When this argument is not null, all the values and keys occurring in the Map must be
     *                   of type String.
     * @param factory An instance of the AuthConfigFactory. Non-null if this provider should self-register,
     *                otherwise it will be null.
     */
    public AuthConfigProviderImpl(Map properties, AuthConfigFactory factory) {
        if (factory != null) {
            factory.registerConfigProvider(this, null/*layer*/, null/*appContext*/, null/*description*/);
        }
    }

    /**
     * Primes this provider with the server auth config to return.
     *
     * @param serverAuthConfig The single serverAuthConfig that will always be returned by this provider.
     */
    public void setServerAuthConfig(ServerAuthConfig serverAuthConfig) {
        this.serverAuthConfig = serverAuthConfig;
    }

    /**
     * This method is not supported!
     *
     * Gets an instance of ClientAuthConfig from this provider.
     *
     * @param layer A String identifying the message layer for the returned ClientAuthConfig object. This argument must
     *              not be null.
     * @param appContext A String that identifies the messaging context for the returned ClientAuthConfig object. This
     *                   argument must not be null.
     * @param handler A CallbackHandler to be passed to the ServerAuthModules encapsulated by ServerAuthContext objects
     *                derived from the returned ServerAuthConfig. The CallbackHandler assigned to the configuration
     *                must support the Callback objects required to be supported by the Servlet Container profile of
     *                this specification being followed by the messaging runtime. The CallbackHandler instance must
     *                be initialized with any application context needed to process the required callbacks on behalf
     *                of the corresponding application.
     * @return A ServerAuthConfig Object that describes the configuration of ServerAuthModules at a given message
     *          layer, and for a particular application context. This method does not return null.
     * @throws UnsupportedOperationException Is thrown always as this method is NOT supported.
     */
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler) {
        throw new UnsupportedOperationException("ClientAuthConfig not supported!");
    }

    /**
     * Get an instance of ServerAuthConfigImpl from this provider. This implementation only provides a single context
     * and will return this regardless of the layer and appContext provided. Providing a default handler is not
     * supported.
     *
     * @param layer A String identifying the message layer for the returned ServerAuthConfig object. This argument must
     *              not be null.
     * @param appContext A String that identifies the messaging context for the returned ServerAuthConfig object. This
     *                   argument must not be null.
     * @param handler A CallbackHandler to be passed to the ServerAuthModules encapsulated by ServerAuthContext objects
     *                derived from the returned ServerAuthConfig. The CallbackHandler assigned to the configuration
     *                must support the Callback objects required to be supported by the Servlet Container profile of
     *                this specification being followed by the messaging runtime. The CallbackHandler instance must
     *                be initialized with any application context needed to process the required callbacks on behalf
     *                of the corresponding application.
     * @return A ServerAuthConfig Object that describes the configuration of ServerAuthModules at a given message layer,
     *          and for a particular application context. This method does not return null.
     * @throws AuthException If this provider does not support the assignment of a default CallbackHandler to the
     *          returned ServerAuthConfig.
     */
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler)
            throws AuthException {
        if (handler != null) {
            throw new AuthException("Assigning a default CallbackHandler is not supported");
        }
        return serverAuthConfig;
    }

    /**
     * Causes a the provider to refresh its internal state and any resulting change to its state is reflected in the
     * corresponding authentication context configuration objects previously created by the provider within the
     * current process context.
     */
    public void refresh() {
        serverAuthConfig.refresh();
    }
}
