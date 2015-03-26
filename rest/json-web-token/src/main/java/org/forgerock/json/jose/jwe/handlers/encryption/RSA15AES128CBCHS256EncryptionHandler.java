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

package org.forgerock.json.jose.jwe.handlers.encryption;

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.utils.Utils;

/**
 * An implementation of an EncryptionHandler that provides encryption and decryption methods using the JweAlgorithm
 * RSAES_PCKS1_V1_5 and EncryptionMethod A128CBC_HS256.
 *
 * @since 2.0.0
 */
public class RSA15AES128CBCHS256EncryptionHandler extends AbstractEncryptionHandler {

    private static final JweAlgorithm ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;
    private static final String INITIALISATION_VECTOR_ALGORITHM = "SHA1PRNG";

    private final SigningManager signingManager;

    /**
     * Constructs a new RSA1_5_AWS128CBC_HS256EncryptionHandler instance.
     *
     * @param signingManager An instance of the SigningManager to use to create the authenticate tag.
     */
    public RSA15AES128CBCHS256EncryptionHandler(SigningManager signingManager) {
        this.signingManager = signingManager;
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

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_METHOD.getEncryptionAlgorithm());
            keyGenerator.init(ENCRYPTION_METHOD.getKeySize());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, "
                    + ENCRYPTION_METHOD.getEncryptionAlgorithm(), e);
        }
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
        return encrypt(ALGORITHM.getAlgorithm(), key, contentEncryptionKey.getEncoded());
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
        try {
            final int ivBitLength = 128;
            SecureRandom randomGen = SecureRandom.getInstance(INITIALISATION_VECTOR_ALGORITHM);

            byte[] bytes = new byte[ivBitLength / 8];
            randomGen.nextBytes(bytes);
            return bytes;
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Algorithm, " + INITIALISATION_VECTOR_ALGORITHM, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JweEncryption encryptPlaintext(Key contentEncryptionKey, byte[] initialisationVector, byte[] plaintext,
            byte[] additionalAuthenticatedData) {

        int keyOffset = ENCRYPTION_METHOD.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset,
                ENCRYPTION_METHOD.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset,
                ENCRYPTION_METHOD.getEncryptionAlgorithm());

        byte[] ciphertext = encrypt(ENCRYPTION_METHOD.getTransformation(), encryptionKey, initialisationVector,
                plaintext);


        int alLength = additionalAuthenticatedData.length * 8;
        byte[] al = ByteBuffer.allocate(8).putInt(alLength).array();


        int authenticationTagInputLength = additionalAuthenticatedData.length + initialisationVector.length
                + ciphertext.length + al.length;
        byte[] dataBytes = ByteBuffer.allocate(authenticationTagInputLength).put(additionalAuthenticatedData)
                .put(initialisationVector).put(ciphertext).put(al).array();
        SigningHandler signingHandler = signingManager.newHmacSigningHandler(macKey.getEncoded());
        byte[] hmac = signingHandler.sign(JwsAlgorithm.getJwsAlgorithm(macKey.getAlgorithm()),
                new String(dataBytes, Utils.CHARSET));

        byte[] authenticationTag = Arrays.copyOf(hmac, ENCRYPTION_METHOD.getKeyOffset());

        return new JweEncryption(ciphertext, authenticationTag);
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

        byte[] contentEncryptionKey = decrypt(ALGORITHM.getAlgorithm(), key, encryptedContentEncryptionKey);

        return new SecretKeySpec(contentEncryptionKey, ENCRYPTION_METHOD.getEncryptionAlgorithm());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext,
            byte[] authenticationTag, byte[] additionalAuthenticatedData) {

        int keyOffset = ENCRYPTION_METHOD.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset,
                ENCRYPTION_METHOD.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset,
                ENCRYPTION_METHOD.getEncryptionAlgorithm());


        int alLength = additionalAuthenticatedData.length * 8;
        byte[] al = ByteBuffer.allocate(8).putInt(alLength).array();

        int authenticationTagInputLength = additionalAuthenticatedData.length + initialisationVector.length
                + ciphertext.length + al.length;
        byte[] dataBytes = ByteBuffer.allocate(authenticationTagInputLength).put(additionalAuthenticatedData)
                .put(initialisationVector).put(ciphertext).put(al).array();
        SigningHandler signingHandler = signingManager.newHmacSigningHandler(macKey.getEncoded());
        byte[] hmac = signingHandler.sign(JwsAlgorithm.getJwsAlgorithm(macKey.getAlgorithm()),
                new String(dataBytes, Utils.CHARSET));

        byte[] expectedAuthenticationTag = Arrays.copyOf(hmac, ENCRYPTION_METHOD.getKeyOffset());

        boolean macValid = false;
        if (Utils.constantEquals(expectedAuthenticationTag, authenticationTag)) {
            macValid = true;
        }

        try {
            byte[] plaintext = decrypt(ENCRYPTION_METHOD.getTransformation(), encryptionKey, initialisationVector,
                    ciphertext);

            if (!macValid) {
                throw new JweDecryptionException();
            }

            return plaintext;

        } catch (JweDecryptionException ex) {
            // Catch and re-throw any exception so that even the stack trace reveals no information about how
            // decryption failed.
            throw new JweDecryptionException();
        }
    }
}
