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

/**
 * Plugin interface for allowing different implementations for determining whether a OAuth2 Access Token is valid
 * and for retrieving scope on user profile information.
 *
 * @since 1.4.0
 */
public interface OAuth2AccessTokenValidator {

    /**
     * Validates whether the given access token is valid, by referring to the OAuth2 Provider and gaining user
     * profile information as well.
     *
     * @param accessToken The access token to validate.
     * @return An AccessTokenValidationResponse containing the result of the validation and scope and profile
     * information.
     * @throws OAuth2Exception If there is a problem validating the access token.
     */
    AccessTokenValidationResponse validate(String accessToken);
}
