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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe.handlers.encryption;

import java.security.Key;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.json.jose.jws.SigningManager;

/**
 * Abstract base class for implementations of the RSAES-PKCS1-v1_5 content encryption scheme. In this scheme a random
 * Content Encryption Key (CEK) is generated for some underlying AES CBC-mode {@link EncryptionMethod} with HMAC for
 * authentication.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7518#section-4.2">RFC 7518 Section 4.2</a>
 * @deprecated Use {@link RSAEncryptionHandler} and {@link AESCBCHMACSHA2ContentEncryptionHandler} instead.
 */
@Deprecated
abstract class AbstractRSAESPkcs1V15AesCbcHmacEncryptionHandler extends AbstractEncryptionHandler {

    private static final JweAlgorithm ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;

    private final RSAEncryptionHandler encryptionHandler;

    /**
     * Constructs a new AbstractRSAES_PKCS1_V1_5EncryptionHandler instance.
     *
     * @param signingManager An instance of the SigningManager to use to create the authenticate tag.
     */
    protected AbstractRSAESPkcs1V15AesCbcHmacEncryptionHandler(SigningManager signingManager,
            EncryptionMethod encryptionMethod) {

        this.encryptionHandler = new RSAEncryptionHandler(encryptionMethod, ALGORITHM);

        if (encryptionMethod != EncryptionMethod.A128CBC_HS256 && encryptionMethod != EncryptionMethod.A256CBC_HS512) {
            throw new IllegalArgumentException("Not an AES/CBC/HMAC encryption method: " + encryptionMethod);
        }
    }

    /**
     * Creates a Content Encryption Key (CEK) by generating a random key value with a length equal to the
     * EncryptionMethod A128CBC_HS256 key size.
     * <p>
     * See point 2 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Key getContentEncryptionKey() {
        return encryptionHandler.getContentEncryptionKey();
    }

    /**
     * Generates the JWE Encrypted Key by encrypting the Content Encryption Key (CEK) using the JweAlgorithm
     * RSAES_PCKCS1_V1_5.
     * <p>
     * See point 4 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @param key {@inheritDoc}
     * @param contentEncryptionKey {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public byte[] generateJWEEncryptedKey(Key key, Key contentEncryptionKey) {
        return encryptionHandler.generateJWEEncryptedKey(key, contentEncryptionKey);
    }

    /**
     * Generates a random JWE Initialisation Vector of the correct size for the encryption algorithm.
     * <p>
     * See points 9 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @return {@inheritDoc}
     */
    @Override
    public byte[] generateInitialisationVector() {
        return encryptionHandler.generateInitialisationVector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JweEncryption encryptPlaintext(Key contentEncryptionKey, byte[] initialisationVector, byte[] plaintext,
                                          byte[] additionalAuthenticatedData) {

        return encryptionHandler.encryptPlaintext(contentEncryptionKey, initialisationVector, plaintext,
                additionalAuthenticatedData);
    }

    /**
     * Decrypts the JWE Encrypted Key to produce the Content Encryption Key (CEK).
     * <p>
     * See points 10 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.2">
     *     Section 5.2</a> of the JWE Specification.
     *
     * @param key {@inheritDoc}
     * @param encryptedContentEncryptionKey {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Key decryptContentEncryptionKey(Key key, byte[] encryptedContentEncryptionKey) {
        return encryptionHandler.decryptContentEncryptionKey(key, encryptedContentEncryptionKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext,
                                    byte[] authenticationTag, byte[] additionalAuthenticatedData) {
        return encryptionHandler.decryptCiphertext(contentEncryptionKey, initialisationVector, ciphertext,
                authenticationTag, additionalAuthenticatedData);
    }

}
