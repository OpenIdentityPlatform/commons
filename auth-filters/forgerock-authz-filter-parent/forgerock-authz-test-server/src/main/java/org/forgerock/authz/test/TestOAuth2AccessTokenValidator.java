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

import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2AccessTokenValidator;
import org.forgerock.authz.modules.oauth2.OAuth2Exception;

import java.util.Collections;

/**
 * Test OAuth2AccessTokenValidator which will return a validation response based on the access token String.
 * <br/>
 * If the access token string does not match any of the constants, a OAuth2Exception is returned.
 */
public class TestOAuth2AccessTokenValidator implements OAuth2AccessTokenValidator {

    /** Invalid Access Token. */
    public static final String INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";
    /** Valid Access Token with no profile info or scope set. */
    public static final String VALID_ACCESS_TOKEN_NO_PROFILE_OR_SCOPE = "VALID_ACCESS_TOKEN_WITH_NO_PROFILE_OR_SCOPE";
    /** Valid Access Token with scope set but no profile info. */
    public static final String VALID_ACCESS_TOKEN_WITH_SCOPE = "VALID_ACCESS_TOKEN_WITH_SCOPE";
    /** Valid Access Token with scope and profile info set. */
    public static final String VALID_ACCESS_TOKEN_WITH_PROFILE_AND_SCOPE = "VALID_ACCESS_TOKEN_WITH_PROFILE_AND_SCOPE";

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenValidationResponse validate(String accessToken) {

        if (INVALID_ACCESS_TOKEN.equalsIgnoreCase(accessToken)) {
            return new AccessTokenValidationResponse(System.currentTimeMillis() - 1);
        }

        if (VALID_ACCESS_TOKEN_NO_PROFILE_OR_SCOPE.equalsIgnoreCase(accessToken)) {
            return new AccessTokenValidationResponse(System.currentTimeMillis() + 1000);
        }

        if (VALID_ACCESS_TOKEN_WITH_SCOPE.equalsIgnoreCase(accessToken)) {
            return new AccessTokenValidationResponse(System.currentTimeMillis() + 1000,
                    Collections.singleton("SCOPE_A"));
        }

        if (VALID_ACCESS_TOKEN_WITH_PROFILE_AND_SCOPE.equalsIgnoreCase(accessToken)) {
            return new AccessTokenValidationResponse(System.currentTimeMillis() + 1000,
                    Collections.<String, Object>singletonMap("name", "NAME"), Collections.singleton("SCOPE_A"));
        }

        throw new OAuth2Exception("Unknown Access Token");
    }
}
