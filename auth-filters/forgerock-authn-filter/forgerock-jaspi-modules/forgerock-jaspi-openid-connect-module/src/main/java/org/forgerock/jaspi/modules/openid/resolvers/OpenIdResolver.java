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
 * Validation of Open ID Connect JWTs via verification of their internals
 * (issuer, audience, signature, etc.). Each Resolver relates to one
 * specific issuer (which can be retrieved via
 * {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver#getIssuer()}) and
 * performs validation against a supplied {@link SignedJwt}, throwing an
 * {@link OpenIdConnectVerificationException} if there are any issues which do not
 * conform to the verification spec as per:
 *
 * More details on how the verification should be completed can be found at
 * <a href="http://openid.net/specs/openid-authentication-2_0.html">
 *     http://openid.net/specs/openid-authentication-2_0.html</a>
 *
 * {@link OpenIdResolver#validateIdentity(org.forgerock.json.jose.jws.SignedJwt)} performs all individual checks.
 */
public interface OpenIdResolver {

    /**
     * Lookup key for a key stored in a keystore.
     */
    public static final String KEY_ALIAS_KEY = "keyAlias";

    /**
     * Lookup key for the issuer's name.
     */
    public static final String ISSUER_KEY = "issuer";

    /**
     * Lookup key for the client secret.
     */
    public static final String CLIENT_SECRET_KEY = "clientSecret";

    /**
     * Lookup key for JWK configuration.
     */
    public static final String JWK = "jwk";

    /**
     * Lookup key for a .well-known Open ID Connect config.
     */
    public static final String WELL_KNOWN_CONFIGURATION = "well-known";

    /**
     * Lookup key for the location of a keystore.
     */
    public static final String KEYSTORE_LOCATION_KEY = "keystoreLocation";

    /**
     * Lookup key for the type of a keystore.
     */
    public static final String KEYSTORE_TYPE_KEY = "keystoreType";

    /**
     * Lookup key for the password to a keystore.
     */
    public static final String KEYSTORE_PASS_KEY = "keystorePassword";

    /**
     * Validates the supplied Jwt against this OpenId Connect Idp.
     *
     * @param idClaim The Jwt to test is authenticated from this issuer
     * @throws OpenIdConnectVerificationException If the Jwt is unable to be verified
     */
    public void validateIdentity(final SignedJwt idClaim) throws OpenIdConnectVerificationException;

    /**
     * Returns the issuer (IdP) for which this resolver will resolve identities.
     *
     * @return the name of the issuer
     */
    public String getIssuer();
}
