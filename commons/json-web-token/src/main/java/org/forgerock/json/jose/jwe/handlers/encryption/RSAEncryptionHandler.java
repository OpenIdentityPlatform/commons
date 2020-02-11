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

import static java.security.spec.MGF1ParameterSpec.SHA256;
import static javax.crypto.spec.PSource.PSpecified.DEFAULT;
import static org.forgerock.json.jose.jwe.JweAlgorithm.RSA_OAEP_256;
import static org.forgerock.json.jose.jwe.JweAlgorithmType.RSA;
import static org.forgerock.util.Reject.checkNotNull;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.util.Reject;

/**
 * Abstract base class for implementations of the RSAES-PKCS1-v1_5 and RSA-OAEP encryption schemes.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7518#section-4.2">RFC 7518 Section 4.2 and 4.3</a>
 */
public final class RSAEncryptionHandler implements EncryptionHandler {
    private static final OAEPParameterSpec RSA_OAEP_256_PARAMS = new OAEPParameterSpec("SHA-256", "MGF1", SHA256,
            DEFAULT);
    private final EncryptionMethod encryptionMethod;
    private final ContentEncryptionHandler contentEncryptionHandler;
    private final JweAlgorithm jweAlgorithm;
    private final AlgorithmParameterSpec parameterSpec;

    /**
     * Constructs a new RSAEncryptionHandler instance.
     *
     * @param encryptionMethod the content encryption method. Must not be null.
     * @param jweAlgorithm the JWE algorithm. Must not be null. Must be an RSA encryption algorithm.
     */
    public RSAEncryptionHandler(EncryptionMethod encryptionMethod, final JweAlgorithm jweAlgorithm) {
        this.encryptionMethod = checkNotNull(encryptionMethod, "EncryptionMethod must not be null");
        this.jweAlgorithm = checkNotNull(jweAlgorithm, "JweAlgorithm must not be null");
        Reject.ifFalse(jweAlgorithm.getAlgorithmType() == RSA, "JweAlgorithm type must be RSA");
        this.contentEncryptionHandler = ContentEncryptionHandler.getInstance(encryptionMethod);
        // RSA-OAEP-256 requires non-default algorithm parameters to conform to the JWE spec. The JRE defaults are
        // correct for all other modes, so leave as null.
        this.parameterSpec = jweAlgorithm == RSA_OAEP_256 ? RSA_OAEP_256_PARAMS : null;
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
        return contentEncryptionHandler.generateEncryptionKey();
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
        return encryptKey((RSAPublicKey) key, contentEncryptionKey);
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
        return contentEncryptionHandler.generateInitialisationVector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JweEncryption encryptPlaintext(Key contentEncryptionKey, byte[] initialisationVector, byte[] plaintext,
                                          byte[] additionalAuthenticatedData) {

        return contentEncryptionHandler.encrypt(contentEncryptionKey, initialisationVector, plaintext,
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
        try {
            final Cipher cipher = Cipher.getInstance(jweAlgorithm.getAlgorithm());
            cipher.init(Cipher.UNWRAP_MODE, key, parameterSpec);
            return cipher.unwrap(encryptedContentEncryptionKey, encryptionMethod.getEncryptionAlgorithm(),
                    Cipher.SECRET_KEY);
        } catch (GeneralSecurityException e) {
            throw new JweDecryptionException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext,
                                    byte[] authenticationTag, byte[] additionalAuthenticatedData) {
        return contentEncryptionHandler.decrypt(contentEncryptionKey, initialisationVector,
                new JweEncryption(ciphertext, authenticationTag), additionalAuthenticatedData);
    }

    private byte[] encryptKey(final RSAPublicKey keyEncryptionKey, final Key contentKey) {
        try {
            final Cipher cipher = Cipher.getInstance(jweAlgorithm.getAlgorithm());
            cipher.init(Cipher.WRAP_MODE, keyEncryptionKey, parameterSpec);
            return cipher.wrap(contentKey);
        } catch (GeneralSecurityException e) {
            throw new JweEncryptionException(e);
        }
    }
}
