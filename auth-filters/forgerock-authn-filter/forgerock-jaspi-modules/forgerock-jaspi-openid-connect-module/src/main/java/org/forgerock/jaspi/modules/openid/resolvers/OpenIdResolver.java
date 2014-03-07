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
package org.forgerock.jaspi.modules.openid.resolvers;

import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.json.jose.jws.SignedJwt;

/**
 * Interface for different OpenId Connect resolvers.
 *
 * {@link OpenIdResolver#validateIdentity(org.forgerock.json.jose.jws.SignedJwt)} performs all individual checks.
 */
public interface OpenIdResolver {

    public static final String KEY_ALIAS_KEY = "keyAlias";
    public static final String CLIENT_ID_KEY = "clientId";
    public static final String ISSUER_KEY = "issuer";
    public static final String CLIENT_SECRET_KEY = "clientSecret";

    static final String AUTHORIZED_PARTY = "azp";

    /**
     * Validates the supplied Jwt against this OpenId Connect Idp.
     *
     * @param idClaim The Jwt to test is authenticated from this issuer
     * @throws OpenIdConnectVerificationException If the Jwt is unable to be verified
     */
    public void validateIdentity(final SignedJwt idClaim) throws OpenIdConnectVerificationException;

}
