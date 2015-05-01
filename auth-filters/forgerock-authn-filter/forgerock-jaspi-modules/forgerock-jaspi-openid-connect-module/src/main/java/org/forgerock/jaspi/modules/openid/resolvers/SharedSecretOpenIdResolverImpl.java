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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.jaspi.modules.openid.resolvers;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;

import java.nio.charset.Charset;

import org.forgerock.jaspi.modules.openid.exceptions.InvalidSignatureException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;

/**
 * This class exists to allow functionality for those Open ID Connect providers which
 * supply their signatures through symmetric key algorithms (e.g. HMAC). In these cases
 * we want to use the shared secret (known to both the provider and client) such that we can
 * generate a "private key". We do this using the SecretKeySpec call in
 * {@link SharedSecretOpenIdResolverImpl#verifySignature}.
 */
public class SharedSecretOpenIdResolverImpl extends BaseOpenIdResolver {

    private final SigningManager signingManager;

    private final String sharedSecret;

    /**
     * Constructor for SharedSecretOpenIdResolverImpl.
     *
     * @param issuer The issuer (provider) of the Open Id Connect id token
     * @param sharedSecret The secret String, known to both provider and consumer
     * @throws IllegalArgumentException if the sharedSecret is null
     */
    public SharedSecretOpenIdResolverImpl(String issuer, String sharedSecret) {
        super(issuer);

        signingManager = new SigningManager();
        if (sharedSecret == null) {
            throw new IllegalArgumentException("sharedSecret must not be null.");
        }

        this.sharedSecret = sharedSecret;
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
     * Verifies that the JWS was signed by the supplied key. Throws an exception otherwise.
     *
     * @param idClaim The JWS to verify
     * @throws InvalidSignatureException If the JWS supplied does not match the key for this resolver
     */
    public void verifySignature(final SignedJwt idClaim) throws InvalidSignatureException {
        if (!idClaim.verify(signingManager.newHmacSigningHandler(sharedSecret.getBytes(Charset.forName("UTF-8"))))) {
            LOG.debug("JWS signature not signed with supplied key");
            throw new InvalidSignatureException("JWS signature not signed with supplied key");
        }
    }
}
