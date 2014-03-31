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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.authz.test.crest;

import org.forgerock.authz.AuthorizationModule;
import org.forgerock.authz.AuthorizationModuleConfigurator;
import org.forgerock.json.fluent.JsonValue;

/**
 * Simple {@link AuthorizationModuleConfigurator} that feeds the
 * {@link TestCrestAuthorizationModule} with a key and a value to populate into the
 * {@link org.forgerock.authz.AuthorizationContext}.
 */
public class TestCrestAuthorizationModuleConfigurator implements AuthorizationModuleConfigurator {

    public static final String TEST_KEY = "myKey";
    public static final String TEST_VALUE = "myValue";

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationModule getModule() {
        return new TestCrestAuthorizationModule();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getConfiguration() {

        return JsonValue.json(JsonValue.object(
                JsonValue.field(TEST_KEY, "testKey"),
                JsonValue.field(TEST_VALUE, "testValue")));

    }

    /**
     * Static factory method to return the configurator instance.
     *
     * @return a configurator instance.
     */
    public static AuthorizationModuleConfigurator getModuleConfigurator() {
        return new TestCrestAuthorizationModuleConfigurator();
    }

}
