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

import javax.servlet.http.HttpServletRequest;
import org.forgerock.authz.AuthorizationContext;
import org.forgerock.authz.AuthorizationModule;
import org.forgerock.json.fluent.JsonValue;

/**
 * Test Authorization module that simply places the key and value set in by the
 * {@link TestCrestAuthorizationModuleConfigurator} into the {@link AuthorizationContext}.
 *
 * If no values are set into the JSON config, it will use the default values.
 */
public class TestCrestAuthorizationModule implements AuthorizationModule {

    private String testKey;
    private String testValue;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise(JsonValue config) {

        this.testKey = config.get(TestCrestAuthorizationModuleConfigurator.TEST_KEY).asString();
        this.testValue = config.get(TestCrestAuthorizationModuleConfigurator.TEST_VALUE).asString();

        if (testKey == null || testKey.isEmpty()) {
            testKey = TestCrestAuthorizationModuleConfigurator.TEST_KEY;
        }

        if (testValue == null || testValue.isEmpty()) {
            testValue = TestCrestAuthorizationModuleConfigurator.TEST_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest servletRequest, AuthorizationContext context) {

        context.setAttribute(testKey, testValue);

        return true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        //nothing to do
    }
}
