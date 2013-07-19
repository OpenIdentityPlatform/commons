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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A builder class to create the configuration for a set of Auth Contexts.
 */
public class Configuration {

    private static final String SESSION_MODULE_KEY = "sessionModule";
    private static final String AUTH_MODULES_KEY = "authModules";

    private Map<String, Map<String, Object>> authContexts = new HashMap<String, Map<String, Object>>();
    private String auditLoggerClassName;

    /**
     * Sets the class name for the Audit Logger to use to audit authentication attempts.
     *
     * @param auditLoggerClassName The Audit Logger class name.
     * @return This configuration object.
     */
    public Configuration setAuditLoggerClassName(String auditLoggerClassName) {
        this.auditLoggerClassName = auditLoggerClassName;
        return this;
    }

    /**
     * Gets the class name for the Audit Logger to use to audit authentication attempts.
     *
     * @return The Audit Logger class name.
     */
    public String getAuditLoggerClassName() {
        return auditLoggerClassName;
    }

    /**
     * Adds an AuthContext to this configuration, returning an AuthContextConfiguration instance to configure
     * the Session and Authentication Modules for the AuthContext.
     *
     * @param authContextId The Auth Context Id.
     * @return An AuthContextConfiguration instance.
     */
    public AuthContextConfiguration addAuthContext(String authContextId) {
        return new AuthContextConfiguration(this, authContextId);
    }

    /**
     * Adds an AuthContext to this configuration, with the Map of configuration properties for this configuration
     * object.
     *
     * @param authContextId The Auth Context Id.
     * @param contextProperties The configuration properties.
     * @return This configuration object.
     */
    public Configuration addAuthContext(String authContextId, Map<String, Object> contextProperties) {
        authContexts.put(authContextId, contextProperties);
        return this;
    }

    /**
     * Adds an Auth Context to this configuration.
     *
     * @param authContextId The Auth Context Id.
     * @param sessionModule The Session Module configuration properties, including the Session Module class name.
     * @param authenticationModules A List of Authentication Module configuration properties, including the
     *                              Authentication Modules class names.
     */
    void addAuthContext(String authContextId, Map<String, Object> sessionModule,
            List<Map<String, Object>> authenticationModules) {

        Map<String, Object> authContext = new HashMap<String, Object>();

        if (sessionModule != null) {
            authContext.put(SESSION_MODULE_KEY, sessionModule);
        }

        authContext.put(AUTH_MODULES_KEY, authenticationModules);

        authContexts.put(authContextId, authContext);
    }

    /**
     * Gets the Auth Context Id key set.
     *
     * @return The Auth Context Key set.
     */
    public Set<String> keySet() {
        return authContexts.keySet();
    }

    /**
     * Gets the Auth Context properties for the given Auth Context Id.
     *
     * @param authContextId The Auth Context Id.
     * @return The Auth Context properties.
     */
    public Map<String, Object> get(String authContextId) {
        return authContexts.get(authContextId);
    }
}
