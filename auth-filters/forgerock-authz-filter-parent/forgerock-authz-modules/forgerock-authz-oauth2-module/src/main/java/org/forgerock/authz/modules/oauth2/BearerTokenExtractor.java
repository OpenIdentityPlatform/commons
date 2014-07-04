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

package org.forgerock.authz.modules.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the bearer token from the request's authorization header.
 *
 * @since 1.5.0
 */
public class BearerTokenExtractor {

    private static final String BEARER_TOKEN_KEY = "BEARER";

    private final Logger logger = LoggerFactory.getLogger(BearerTokenExtractor.class);

    /**
     * Pulls the access token off of the request, by looking for the Authorization header containing a Bearer token.
     *
     * @param authorizationHeader The authorization header from the request.
     * @return The access token, or <code>null</code> if the access token was not present or was not using Bearer
     * authorization.
     */
    public String getAccessToken(String authorizationHeader) {

        if (authorizationHeader == null) {
            logger.debug("Authorization header not present");
            return null;
        }
        final int index = authorizationHeader.indexOf(' ');
        if (index <= 0) {
            logger.debug("Invalid Authorization header");
            return null;
        }

        final String tokenType = authorizationHeader.substring(0, index);

        if (BEARER_TOKEN_KEY.equalsIgnoreCase(tokenType)) {
            return authorizationHeader.substring(index + 1);
        }

        logger.debug("Unknown Access Token Type, {}", tokenType);
        return null;
    }
}
