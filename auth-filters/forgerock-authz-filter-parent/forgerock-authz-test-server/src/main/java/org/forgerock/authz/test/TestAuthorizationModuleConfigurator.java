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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.authz.test;

import org.forgerock.authz.AuthorizationModule;
import org.forgerock.authz.AuthorizationModuleConfigurator;
import org.forgerock.json.fluent.JsonValue;

/**
 * Configures the {@link TestAuthorizationModule}.
 */
public class TestAuthorizationModuleConfigurator implements AuthorizationModuleConfigurator {

    @Override
    public AuthorizationModule getModule() {
        return new TestAuthorizationModule();
    }

    @Override
    public JsonValue getConfiguration() {
        // Override the magic parameter and value to require additional politeness
        return JsonValue.json(JsonValue.object(
                JsonValue.field(TestAuthorizationModule.CONFIG_MAGIC_PARAM_NAME, "pleaseletmein"),
                JsonValue.field(TestAuthorizationModule.CONFIG_MAGIC_PARAM_VALUE, "thankyou")));
    }

    /**
     * Static factory method to return the configurator instance.
     *
     * @return a configurator instance.
     */
    public static TestAuthorizationModuleConfigurator getModuleConfigurator() {
        return new TestAuthorizationModuleConfigurator();
    }
}
