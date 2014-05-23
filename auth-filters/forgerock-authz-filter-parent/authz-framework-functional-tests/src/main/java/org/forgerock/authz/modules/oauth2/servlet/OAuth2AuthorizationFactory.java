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

package org.forgerock.authz.modules.oauth2.servlet;

import org.forgerock.authz.filter.servlet.AuthorizationModuleFactory;
import org.forgerock.authz.modules.oauth2.AccessTokenValidationResponse;
import org.forgerock.authz.modules.oauth2.OAuth2AccessTokenValidator;
import org.forgerock.authz.modules.oauth2.OAuth2Authorization;

import java.util.Collections;

import static org.forgerock.authz.filter.servlet.AuthorizationModules.newAuthorizationModuleFactory;

/**
 * A factory class for creating an {@code AuthorizationModuleFactory} configured with an OAuth2 authorization filter.
 *
 * @since 1.5.0
 */
public final class OAuth2AuthorizationFactory {

    /**
     * Private utility constructor.
     */
    private OAuth2AuthorizationFactory() {
        throw new UnsupportedOperationException("OAuth2AuthorizationFactory cannot be instantiated.");
    }

    /**
     * Creates a {@code AuthorizationModuleFactory} for a OAuth2 authorization filter which will deem an access token
     * valid if it matches the string "VALID" and will return a scope of "SCOPE" and user info of "UID"->"DEMO".
     *
     * @return A {@code AuthorizationModuleFactory} instance.
     */
    public static AuthorizationModuleFactory getAuthorizationModuleFactory() {
        return newAuthorizationModuleFactory(OAuth2Authorization.forHttpServlet(
                new OAuth2AccessTokenValidator() {
                    @Override
                    public AccessTokenValidationResponse validate(String accessToken) {
                        if ("VALID".equalsIgnoreCase(accessToken)) {
                            return new AccessTokenValidationResponse(System.currentTimeMillis() + 5000,
                                    Collections.<String, Object>singletonMap("UID", "DEMO"),
                                    Collections.singleton("SCOPE"));
                        } else {
                            return new AccessTokenValidationResponse(0);
                        }
                    }
                }, Collections.<String>emptySet(), false, 0));
    }
}
