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

import javax.security.auth.message.module.ServerAuthModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder class to create the configuration for an Auth Context.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class AuthContextConfiguration {

    private final Configuration configuration;
    private final String authContextId;

    private Map<String, Object> sessionModule;
    private final List<Map<String, Object>> authenticationModules = new ArrayList<Map<String, Object>>();

    /**
     * Constructs an instance of the AuthContextConfiguration.
     *
     * @param configuration The parent Configuration builder.
     * @param authContextId The Auth Context Id.
     */
    public AuthContextConfiguration(Configuration configuration, String authContextId) {
        this.configuration = configuration;
        this.authContextId = authContextId;
    }

    /**
     * Sets the Session Module of the Auth Context configuration.
     *
     * @param sessionModuleClass The Session Module class.
     * @param moduleProperties The configuration properties for the Session Module.
     * @param <T> The Session Module class as a subtype of a ServerAuthModule.
     * @return This AuthContextConfiguration instance.
     */
    public <T extends ServerAuthModule> AuthContextConfiguration setSessionModule(Class<T> sessionModuleClass,
            Map<String, Object> moduleProperties) {

        Map<String, Object> moduleProps = new HashMap<String, Object>(moduleProperties);
        moduleProps.put("className", sessionModuleClass.getCanonicalName());

        return setSessionModule(moduleProps);
    }

    /**
     * Sets the Session Module of the Auth Context configuration.
     * <p>
     * Expects that the "className" property is already set in the given module properties map and the class is
     * a sub type of the {@link ServerAuthModule}.
     *
     * @param moduleProperties The configuration properties for the Session Module.
     * @return This AuthContextConfiguration instance.
     * @see #setSessionModule(Class, java.util.Map)
     */
    public AuthContextConfiguration setSessionModule(Map<String, Object> moduleProperties) {
        sessionModule = moduleProperties;
        return this;
    }

    /**
     * Adds an Authentication Module to the Auth Context configuration.
     *
     * @param authenticationModuleClass The Authentication Module class.
     * @param moduleProperties The configuration properties for the Authentication Module.
     * @param <T> The Authentication Module class as a subtype of a ServerAuthModule.
     * @return This AuthContextConfiguration instance.
     */
    public <T extends ServerAuthModule> AuthContextConfiguration addAuthenticationModule(
            Class<T> authenticationModuleClass, Map<String, Object> moduleProperties) {

        Map<String, Object> moduleProps = new HashMap<String, Object>(moduleProperties);
        moduleProps.put("className", authenticationModuleClass.getCanonicalName());

        return addAuthenticationModule(moduleProps);
    }

    /**
     * Adds an Authentication Module to the Auth Context configuration.
     * <p>
     * Expects that the "className" property is already set in the given module properties map and the class is
     * a sub type of the {@link ServerAuthModule}.
     *
     * @param moduleProperties The configuration properties for the Authentication Module.
     * @return This AuthContextConfiguration instance.
     * @see #addAuthenticationModule(Class, java.util.Map)
     */
    public AuthContextConfiguration addAuthenticationModule(Map<String, Object> moduleProperties) {
        authenticationModules.add(moduleProperties);
        return this;
    }

    /**
     * Signals the configuration for the Auth Context is finished.
     *
     * @return The Configuration builder instance.
     */
    public Configuration done() {
        configuration.addAuthContext(authContextId, sessionModule, authenticationModules);
        return configuration;
    }
}
