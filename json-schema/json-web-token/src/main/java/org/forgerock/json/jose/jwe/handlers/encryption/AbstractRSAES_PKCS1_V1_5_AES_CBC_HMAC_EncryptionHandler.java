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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe.handlers.encryption;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.utils.Utils;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Abstract base class for implementations of the RSAES-PKCS1-v1_5 content encryption scheme. In this scheme a random
 * Content Encryption Key (CEK) is generated for some underlying AES CBC-mode {@link EncryptionMethod} with HMAC for
 * authentication.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7518#section-4.2">RFC 7518 Section 4.2</a>
 */
abstract class AbstractRSAES_PKCS1_V1_5_AES_CBC_HMAC_EncryptionHandler extends AbstractEncryptionHandler {

    private static final JweAlgorithm ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final String INITIALISATION_VECTOR_ALGORITHM = "SHA1PRNG";
    private static final String RAW_KEY_FORMAT = "RAW";

    private final SigningManager signingManager;
    private final EncryptionMethod encryptionMethod;

    /**
     * Constructs a new AbstractRSAES_PKCS1_V1_5EncryptionHandler instance.
     *
     * @param signingManager An instance of the SigningManager to use to create the authenticate tag.
     */
    protected AbstractRSAES_PKCS1_V1_5_AES_CBC_HMAC_EncryptionHandler(SigningManager signingManager,
                                                                      EncryptionMethod encryptionMethod) {
        this.signingManager = checkNotNull(signingManager, "SigningManager must not be null");
        this.encryptionMethod = checkNotNull(encryptionMethod, "EncryptionMethod must not be null");

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

        // We need to generate a CEK sufficiently large to supply the key for the AES block cipher and the HMAC. As
        // the HMAC algorithms are all truncated to half of their output size, it is sufficient to generate a CEK
        // with size (AES key size) + (HMAC SHA Key Size / 2). For example, AES128HS256 will produce a CEK of length
        // 128 + (256/2) = 256 bits, while AES256HS512 will use 256 + (512/2) = 512 bits. In the latter case, we
        // cannot simply generate an "AES" key of 512 bits, as this is an invalid key size for AES (and will generate
        // an exception if we try). So instead, we generate separate keys for the MAC and the AES cipher and
        // concatenate them as CEK = MAC_KEY + ENC_KEY as per https://tools.ietf.org/html/rfc7518#section-5.2.2.1

        try {
            // The keyOffset field gives the size of the MAC key in octets
            final int macKeySize = encryptionMethod.getKeyOffset() * 8;
            final KeyGenerator macKeyGenerator = KeyGenerator.getInstance(encryptionMethod.getMacAlgorithm());
            macKeyGenerator.init(macKeySize);
            final Key macKey = macKeyGenerator.generateKey();
            if (!RAW_KEY_FORMAT.equals(macKey.getFormat())) {
                throw new IllegalStateException("HMAC KeyGenerator returned non-RAW key material!");
            }

            final int encKeySize = encryptionMethod.getKeySize() - macKeySize;
            final KeyGenerator encKeyGenerator = KeyGenerator.getInstance(encryptionMethod.getEncryptionAlgorithm());
            encKeyGenerator.init(encKeySize);
            final Key encKey = encKeyGenerator.generateKey();
            if (!RAW_KEY_FORMAT.equals(macKey.getFormat())) {
                throw new IllegalStateException("AES KeyGenerator returned non-RAW key material!");
            }

            final byte[] combinedKey = ByteBuffer.allocate(encryptionMethod.getKeySize() / 8)
                    .put(macKey.getEncoded())
                    .put(encKey.getEncoded())
                    .array();

            return new SecretKeySpec(combinedKey, encryptionMethod.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, "
                    + encryptionMethod.getEncryptionAlgorithm(), e);
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

        int keyOffset = encryptionMethod.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset,
                encryptionMethod.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset,
                encryptionMethod.getEncryptionAlgorithm());

        byte[] ciphertext = encrypt(encryptionMethod.getTransformation(), encryptionKey, initialisationVector,
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

        byte[] authenticationTag = Arrays.copyOf(hmac, encryptionMethod.getKeyOffset());

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

        return new SecretKeySpec(contentEncryptionKey, encryptionMethod.getEncryptionAlgorithm());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext,
                                    byte[] authenticationTag, byte[] additionalAuthenticatedData) {

        int keyOffset = encryptionMethod.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset,
                encryptionMethod.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset,
                encryptionMethod.getEncryptionAlgorithm());


        int alLength = additionalAuthenticatedData.length * 8;
        byte[] al = ByteBuffer.allocate(8).putInt(alLength).array();

        int authenticationTagInputLength = additionalAuthenticatedData.length + initialisationVector.length
                + ciphertext.length + al.length;
        byte[] dataBytes = ByteBuffer.allocate(authenticationTagInputLength).put(additionalAuthenticatedData)
                .put(initialisationVector).put(ciphertext).put(al).array();
        SigningHandler signingHandler = signingManager.newHmacSigningHandler(macKey.getEncoded());
        byte[] hmac = signingHandler.sign(JwsAlgorithm.getJwsAlgorithm(macKey.getAlgorithm()),
                new String(dataBytes, Utils.CHARSET));

        byte[] expectedAuthenticationTag = Arrays.copyOf(hmac, encryptionMethod.getKeyOffset());

        boolean macValid = false;
        if (Utils.constantEquals(expectedAuthenticationTag, authenticationTag)) {
            macValid = true;
        }

        try {
            byte[] plaintext = decrypt(encryptionMethod.getTransformation(), encryptionKey, initialisationVector,
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
