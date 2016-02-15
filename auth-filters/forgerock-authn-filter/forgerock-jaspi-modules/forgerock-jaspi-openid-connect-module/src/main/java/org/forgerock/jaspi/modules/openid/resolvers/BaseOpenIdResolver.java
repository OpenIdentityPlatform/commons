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
* Copyright 2014-2016 ForgeRock AS.
*/
package org.forgerock.jaspi.modules.openid.resolvers;

import java.security.Key;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import javax.crypto.SecretKey;

import org.forgerock.jaspi.modules.openid.exceptions.InvalidIssException;
import org.forgerock.jaspi.modules.openid.exceptions.JwtExpiredException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;

/**
 * Implementation of the OpenIdResolver interface. Comments in the specific verify methods
 * are taken directly from OpenID Connect Basic Client Implementer's Guide 1.0,
 * section 2.2.1 - ID Token Validation
 *
 * Currently we do NO validation against the client ID/intended audience.
 *
 * @see <a href="http://openid.net/specs/openid-connect-basic-1_0.html">
 *     http://openid.net/specs/openid-connect-basic-1_0.html</a>
 */
public abstract class BaseOpenIdResolver implements OpenIdResolver {

    private final String issuer;

    /**
     * Abstract constructor for setting the issuer's identity.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     */
    public BaseOpenIdResolver(final String issuer) {
        this.issuer = issuer;
    }

    /**
     * Verifies the issuer is exactly who it is expected to be.
     *
     * @param issuerName The name of the claimed issuer
     * @throws InvalidIssException if the expected issuer and actual issuer do not match
     */
    void verifyIssuer(final String issuerName) throws InvalidIssException {
        //The issuer MUST exactly match the value of the iss (issuer) Claim.
        if (!issuer.equals(issuerName)) {
            throw new InvalidIssException("Invalid issuer");
        }
    }

    /**
     * Verifies that the current date is no later than the expiry date on the JWT.
     *
     * @param expirationTime time at which this id token expires
     * @throws JwtExpiredException if the current time is after the expired time
     */
    void verifyExpiration(final Date expirationTime) throws JwtExpiredException {
        //Expiration time on or after which the ID Token MUST NOT be accepted for processing.
        if (new Date().after(expirationTime)) {
            throw new JwtExpiredException("Token expired");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateIdentity(final SignedJwt idClaim) throws OpenIdConnectVerificationException {

        if (idClaim == null) {
            throw new OpenIdConnectVerificationException("A valid SignedJWT must be supplied to the resolver");
        }

        verifyIssuer(idClaim.getClaimsSet().getIssuer());
        verifyExpiration(idClaim.getClaimsSet().getExpirationTime());
    }

    /**
     * Determine an appropriate signing handler to use for verifying signatures using the given verification key.
     *
     * @param signingManager the signing manager.
     * @param key the verification key.
     * @return the appropriate signing handler.
     * @throws IllegalArgumentException if no handler can be determined for the given key.
     */
    protected SigningHandler createSigningHandlerForKey(final SigningManager signingManager, final Key key) {
        if (key instanceof ECPublicKey) {
            return signingManager.newEcdsaVerificationHandler(((ECPublicKey) key));
        } else if (key instanceof RSAPublicKey) {
            return signingManager.newRsaSigningHandler(key);
        } else if (key instanceof SecretKey) {
            return signingManager.newHmacSigningHandler(key.getEncoded());
        } else {
            throw new IllegalArgumentException("Unable to determine signing algorithm");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIssuer() {
        return issuer;
    }
}
