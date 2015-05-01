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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authn;

import java.util.Arrays;
import java.util.List;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;

/**
 * Object containing auth module configuration parameters.
 *
 * @since 1.5.0
 */
final class AuthModuleParameters {

    private final Class<? extends AsyncServerAuthModule> moduleClass;
    private final String moduleName;
    private final String validateRequestReturnValue;
    private final String secureResponseReturnValue;

    private AuthModuleParameters(Class<? extends AsyncServerAuthModule> moduleClass, String moduleName,
            String validateRequestReturnValue, String secureResponseReturnValue) {
        this.moduleClass = moduleClass;
        this.moduleName = moduleName;
        this.validateRequestReturnValue = validateRequestReturnValue;
        this.secureResponseReturnValue = secureResponseReturnValue;
    }

    /**
     * Creates a {@code List} of {@code AuthModuleParameters}.
     *
     * @param moduleParams An array of {@code AuthModuleParameters}.
     * @return A {@code List} of {@code AuthModuleParameters}.
     */
    static List<AuthModuleParameters> moduleArray(AuthModuleParameters... moduleParams) {
        return Arrays.asList(moduleParams);
    }

    /**
     * Creates a new {@code AuthModuleParameters} object with the given parameters.
     *
     * @param moduleClass The module's fully qualified class name.
     * @param moduleName The name of the module. Used to determine which headers to set on the request.
     * @param validateRequestReturnValue The requested return value for the #validateRequest method.
     * @param secureResponseReturnValue The requested return value for the #secureResponse method.
     * @return A {@code AuthModuleParameters} object.
     */
    static AuthModuleParameters moduleParams(Class<? extends AsyncServerAuthModule> moduleClass, String moduleName,
            String validateRequestReturnValue, String secureResponseReturnValue) {
        return new AuthModuleParameters(moduleClass, moduleName, validateRequestReturnValue, secureResponseReturnValue);
    }

    Class<? extends AsyncServerAuthModule> getModuleClass() {
        return moduleClass;
    }

    String getModuleName() {
        return moduleName;
    }

    String validateRequestReturnValue() {
        return validateRequestReturnValue;
    }

    String secureResponseReturnValue() {
        return secureResponseReturnValue;
    }

    @Override
    public String toString() {
        return "ModuleParams:" + moduleClass.getSimpleName() + "," + moduleName + "," + validateRequestReturnValue + ","
                + secureResponseReturnValue;
    }
}
