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

import java.util.Date;
import java.util.List;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidAudException;
import org.forgerock.jaspi.modules.openid.exceptions.InvalidIssException;
import org.forgerock.jaspi.modules.openid.exceptions.JwtExpiredException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.SignedJwt;

/**
 * Implementation of the OpenIdResolver interface. Comments in the specific verify methods
 * are taken directly from OpenID Connect Basic Client Implementer's Guide 1.0,
 * section 2.2.1 - ID Token Validation
 *
 * @see <a href="http://openid.net/specs/openid-connect-basic-1_0.html">http://openid.net/specs/openid-connect-basic-1_0.html</a>
 */
public abstract class BaseOpenIdResolver implements OpenIdResolver {

    private final String issuer;
    private final String clientId;

    /**
     * Default constructor
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param clientId The client ID (consumer) of the Open Id Connect id token
     */
    public BaseOpenIdResolver(String issuer, String clientId) {
        this.issuer = issuer;
        this.clientId = clientId;
    }

    /**
     * Verifies that our client is part of the target audience, and if there's a specific
     * authorized party, or multiple members of the target audience, that we are the
     * authorized party.
     *
     * @param audiences list of audiences to which this jwt is directed
     * @param authorizedParty optional specific member of audiences which if set must equal our client id
     * @throws org.forgerock.jaspi.modules.openid.exceptions.InvalidAudException if our client id is not in the list of audiences or the specified authorizedParty
     */
    void verifyAudience(final List<String> audiences, final JsonValue authorizedParty) throws InvalidAudException {
        //The Client MUST validate that the aud (audience) Claim contains its client_id value
        //registered at the Issuer identified by the iss (issuer) Claim as an audience.
        if (audiences == null || !audiences.contains(clientId)) {
            throw new InvalidAudException("Invalid audience");
        }

        //If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
        //If an azp (authorized party) Claim is present, the Client SHOULD verify and that its
        //client_id is the Claim value.
        if (audiences.size() > 1 && authorizedParty != null) {
            if (!clientId.equals(authorizedParty.asString())) {
                throw new InvalidAudException("Invalid Authorized Party");
            }
        }
    }

    /**
     * Verifies the issuer is exactly who it is expected to be.
     *
     * @param issuerName The name of the claimed issuer
     * @throws org.forgerock.jaspi.modules.openid.exceptions.InvalidIssException if the expected issuer and actual issuer do not match
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
     * @throws org.forgerock.jaspi.modules.openid.exceptions.JwtExpiredException if the current time is after the expired time
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
        verifyIssuer(idClaim.getClaimsSet().getIssuer());
        verifyAudience(idClaim.getClaimsSet().getAudience(), idClaim.getClaimsSet().get(AUTHORIZED_PARTY));
        verifyExpiration(idClaim.getClaimsSet().getExpirationTime());
    }

}
