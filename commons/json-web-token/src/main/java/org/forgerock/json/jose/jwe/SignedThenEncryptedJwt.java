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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

import java.security.Key;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JwsVerifyingException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.Payload;

/**
 * A nested signed-then-encrypted JWT.
 */
public class SignedThenEncryptedJwt extends EncryptedJwt {
    private static final JwtReconstruction JWT_RECONSTRUCTION = new JwtReconstruction();

    /**
     * Constructs a fresh signed-then-encrypted JWT with the given signed JWT payload, JWE headers and encryption key.
     *
     * @param header the JWE headers.
     * @param payload the signed JWT payload.
     * @param publicKey the encryption key.
     */
    public SignedThenEncryptedJwt(final JweHeader header, final SignedJwt payload, final Key publicKey) {
        super(header, payload, publicKey);
    }

    /**
     * Reconstructs a signed-then-encrypted JWT from components parts of the encrypted JWT string.
     *
     * @param header the decoded headers.
     * @param encodedHeader the encoded headers.
     * @param encryptedContentEncryptionKey the encrypted content encryption key (CEK), or null if not used.
     * @param initialisationVector the initialisation vector (IV).
     * @param ciphertext the encrypted ciphertext payload.
     * @param authenticationTag the authentication MAC tag.
     */
    public SignedThenEncryptedJwt(final JweHeader header, final String encodedHeader,
            final byte[] encryptedContentEncryptionKey,
            final byte[] initialisationVector, final byte[] ciphertext, final byte[] authenticationTag) {
        super(header, encodedHeader, encryptedContentEncryptionKey, initialisationVector, ciphertext,
                authenticationTag);
    }

    /**
     * Verifies that the signature is valid on the nested signed JWT.
     * @param signingHandler the handler to use for verifying the signature.
     * @return {@literal true} if the signature is valid, otherwise {@literal false}.
     * @throws JwsVerifyingException if the outer JWT has not already been decrypted.
     */
    public boolean verify(SigningHandler signingHandler) {
        if (getPayload() == null) {
            throw new JwsVerifyingException("JWT must be decrypted before the nested signature can be verified");
        }
        return ((SignedJwt) getPayload()).verify(signingHandler);
    }

    /**
     * Decrypts the outer JWT and then verifies the signature on the inner JWT.
     *
     * @param decryptionKey the decryption key for the outer JWE.
     * @param signingHandler the signing handler for verifying the nested JWS.
     * @return {@literal true} if the nested signature is valid, otherwise {@literal false}.
     * @throws JweDecryptionException if the JWE cannot be decrypted.
     */
    public boolean decryptAndVerify(Key decryptionKey, SigningHandler signingHandler) {
        decrypt(decryptionKey);
        return verify(signingHandler);
    }

    @Override
    public JwtClaimsSet getClaimsSet() {
        return ((SignedJwt) getPayload()).getClaimsSet();
    }


    @Override
    Payload decodePayload(String decryptedPayload) {
        return JWT_RECONSTRUCTION.reconstructJwt(decryptedPayload, SignedJwt.class);
    }
}
