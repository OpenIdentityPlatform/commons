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

package org.forgerock.authz.test;

import org.forgerock.authz.AuthorizationModule;
import org.forgerock.authz.AuthorizationModuleConfigurator;
import org.forgerock.authz.modules.oauth2.OAuth2Module;
import org.forgerock.authz.modules.oauth2.RestOAuth2AccessTokenValidator;
import org.forgerock.json.fluent.JsonValue;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Configures the {@link OAuth2Module}.
 */
public class OAuth2AuthorizationModuleConfigurator implements AuthorizationModuleConfigurator {

//    private static final JsonValue OPENAM_INTERNAL_VALIDATOR = json(object(
//            field("access-token-validator-class", OpenAMOAuth2AccessTokenValidator.class.getName()),
//            field("required-scopes", array())
//    ));

//    private static final JsonValue OPENAM_REST_VALIDATOR = json(object(
//            field("access-token-validator-class", OpenAMRestOAuth2AccessTokenValidator.class.getName()),
//            field("required-scopes", array()),
//            field("token-info-endpoint", "http://phill.internal.forgerock.com:8080/openam/oauth2/tokeninfo"),
//            field("user-info-endpoint", "http://phill.internal.forgerock.com:8080/openam/oauth2/userinfo")
//    ));

    private static final JsonValue GOOGLE_VALIDATOR = json(object(
            field("access-token-validator-class", RestOAuth2AccessTokenValidator.class.getName()),
            field("required-scopes", array()),
            field("token-info-endpoint", "https://www.googleapis.com/oauth2/v1/tokeninfo"),
            field("user-info-endpoint", "https://www.googleapis.com/oauth2/v2/userinfo")
    ));

    @Override
    public AuthorizationModule getModule() {
        return new OAuth2Module();
    }

    @Override
    public JsonValue getConfiguration() {
        return GOOGLE_VALIDATOR;
    }

    /**
     * Static factory method to return the configurator instance.
     *
     * @return a configurator instance.
     */
    public static AuthorizationModuleConfigurator getModuleConfigurator() {
        return new OAuth2AuthorizationModuleConfigurator();
    }
}
