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

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;

import java.security.PublicKey;

import org.forgerock.jaspi.modules.openid.exceptions.InvalidSignatureException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;

/**
 * This class exists to allow functionality for those Open ID Connect providers which
 * supply their signatures through asymmetric key algorithms (e.g. RSA). In these cases
 * we want to use a public key (usually retrieved from a Trust Store) to verify the
 * signature.
 */
public class PublicKeyOpenIdResolverImpl extends BaseOpenIdResolver {

    private final SigningManager signingManager;

    private final PublicKey key;

    /**
     * Constructor for PublicKeyOpenIdResolverImpl.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param key The public key, used to verify a private-key signed signature
     */
    public PublicKeyOpenIdResolverImpl(String issuer, PublicKey key) {
        super(issuer);

        signingManager = new SigningManager();
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateIdentity(final SignedJwt idClaim) throws OpenIdConnectVerificationException {
        super.validateIdentity(idClaim);
        verifySignature(idClaim);
    }

    /**
     * Verifies that the JWS was signed by the corresponding private key to this
     * public key.
     *
     * @param idClaim The JWS to verify
     * @throws InvalidSignatureException If the JWS supplied does not match the key for this resolver
     */
    public void verifySignature(final SignedJwt idClaim) throws InvalidSignatureException {
        if (!idClaim.verify(createSigningHandlerForKey(signingManager, key))) {
            LOG.debug("JWS signature not signed with supplied key");
            throw new InvalidSignatureException("JWS signature not signed with supplied key");
        }
    }

}
