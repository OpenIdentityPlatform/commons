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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jws;

import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.security.Key;

/**
 * An implementation of a JWS with a nested JWE as its payload.
 * <p>
 * @see SignedJwt
 * @see EncryptedJwt
 *
 * @since 2.0.0
 */
public class EncryptedThenSignedJwt extends SignedJwt {

    /**
     * Constructs a fresh, new SignedEncryptedJwt from the given JwsHeader and nested Encrypted JWT.
     * <p>
     * The specified private key will be used in the creation of the JWS signature.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param nestedJwe The nested Encrypted JWT that will be the payload of this JWS.
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     */
    public EncryptedThenSignedJwt(JwsHeader header, EncryptedJwt nestedJwe, SigningHandler signingHandler) {
        super(header, nestedJwe, signingHandler);
    }

    /**
     * Constructs a reconstructed SignedEncryptedJwt from its constituent parts, the JwsHeader, nested Encrypted JWT,
     * signing input and signature.
     * <p>
     * For use when a signed nested encrypted JWT has been reconstructed from its base64url encoded string
     * representation and the signature needs verifying.
     *
     * @param header The JwsHeader containing the header parameters of the JWS.
     * @param nestedJwe The nested Encrypted JWT that is the payload of the JWS.
     * @param signingInput The original data that was signed, being the base64url encoding of the JWS header and
     *                     payload concatenated using a "." character.
     * @param signature The resulting signature of signing the signing input.
     */
    public EncryptedThenSignedJwt(JwsHeader header, EncryptedJwt nestedJwe, byte[] signingInput, byte[] signature) {
        super(header, nestedJwe, signingInput, signature);
    }

    /**
     * Gets the claims set object for the nested Encrypted JWT that is the payload of this JWS.
     *
     * @return {@inheritDoc}
     * @see org.forgerock.json.jose.jwt.Jwt#getClaimsSet()
     */
    @Override
    public JwtClaimsSet getClaimsSet() {
        return ((Jwt) getPayload()).getClaimsSet();
    }

    /**
     * Decrypts the JWE so that it Claims Set can be accessed.
     * <p>
     * The same private key must be given here that is the pair to the public key that was used to encrypt the JWT.
     *
     * @param privateKey The private key pair to the public key that encrypted the JWT.
     */
    public void decrypt(Key privateKey) {
        ((EncryptedJwt) getPayload()).decrypt(privateKey);
    }
}
