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

package org.forgerock.jaspi.runtime.context.config;

import org.forgerock.json.fluent.JsonValue;

/**
 * A factory which is responsible for getting the ServerAuthModule configuration for the JASPI runtime.
 *
 * @since 1.3.0
 */
public interface ModuleConfigurationFactory {

    /**
     * Server Auth Context configuration key.
     */
    String SERVER_AUTH_CONTEXT_KEY = "serverAuthContext";

    /**
     * Session Auth Module configuration key.
     */
    String SESSION_MODULE_KEY = "sessionModule";

    /**
     * Server Auth Modules configuration key.
     */
    String AUTH_MODULES_KEY = "authModules";

    /**
     * Auth Module class name configuration key.
     */
    String AUTH_MODULE_CLASS_NAME_KEY = "className";

    /**
     * Auth Module properties configuration key.
     */
    String AUTH_MODULE_PROPERTIES_KEY = "properties";

    /**
     * Gets the configuration, as a {@link JsonValue}, which contains all the information to configure the
     * ServerAuthContext for the JASPI runtime.
     *
     * @return A JsonValue of configuration.
     */
    JsonValue getConfiguration();
}
