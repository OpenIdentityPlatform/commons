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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe.handlers.encryption;

import java.security.Key;

import org.forgerock.json.jose.jwe.JweEncryption;

/**
 * The interface for EncryptionHandlers for all the different encryption algorithms.
 * <p>
 * Provides methods for encrypting plaintexts and decrypting ciphertexts.
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5">
 *     JWE Encryption and Decryption</a>
 *
 * @since 2.0.0
 */
public interface EncryptionHandler {

    /**
     * Creates a Content Encryption Key (CEK) following the appropriate steps defined by the EncryptionHandler
     * JweAlgorithm.
     * <p>
     * See points 1, 2, 3 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @return The Content Encryption Key or {@literal null} if the shared key should be used directly.
     */
    Key getContentEncryptionKey();

    /**
     * Generates the Content Encryption Key (CEK) following the appropriate steps defined by the EncryptionHandler
     * JweAlgorithm.
     * <p>
     * See points 4, 5, 6 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @param key The key to use to encrypt the Content Encryption Key, if the EncryptionHandler JweAlgorithm requires.
     * @param contentEncryptionKey The Content Encryption Key (CEK).
     * @return A byte array of the JWE Encrypted Key.
     */
    byte[] generateJWEEncryptedKey(Key key, Key contentEncryptionKey);

    /**
     * Generates a random JWE Initialisation Vector of the correct size for the encryption algorithm, if the
     * EncryptionHandler JweAlgorithm does not required an initialisation vector then the initialisation vector will
     * be an empty octet sequence.
     * <p>
     * See points 9 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @return The Initialisation Vector.
     */
    byte[] generateInitialisationVector();

    /**
     * Encrypts the plaintext with the Content Encryption Key, using the initialisation vector and additional
     * authenticated data, following the steps defined by the EncryptionHandler JweAlgorithm.
     * <p>
     * See points 15, 16 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.1">
     *     Section 5.1</a> of the JWE Specification.
     *
     * @param contentEncryptionKey The Content Encryption Key.
     * @param initialisationVector The Initialisation Vector.
     * @param plaintext The plaintext to encrypt.
     * @param additionalAuthenticatedData An array of bytes representing the additional authenticated data.
     * @return The JweEncryption object containing the ciphertext and authentication tag.
     */
    JweEncryption encryptPlaintext(Key contentEncryptionKey, byte[] initialisationVector, byte[] plaintext,
            byte[] additionalAuthenticatedData);

    /**
     * Decrypts the Content Encryption Key (CEK) following the appropriate steps defined by the EncryptionHandler
     * JweAlgorithm.
     * <p>
     * See points 9, 10 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.2">
     *     Section 5.2</a> of the JWE Specification.
     *
     * @param key The private key pair to the public key that encrypted the JWT.
     * @param encryptedContentEncryptionKey The encrypted Content Encryption Key.
     * @return The decrypted Content Encryption Key.
     */
    Key decryptContentEncryptionKey(Key key, byte[] encryptedContentEncryptionKey);

    /**
     * Decrypts the ciphertext with the Content Encryption Key, using the initialisation vector and additional
     * authenticated data, following the steps defined by the EncryptionHandler JweAlgorithm.
     * <p>
     * See points 14, 15 in <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5.2">
     *     Section 5.2</a> of the JWE Specification.
     *
     * @param contentEncryptionKey The Content Encryption Key.
     * @param initialisationVector The Initialisation Vector.
     * @param ciphertext The ciphertext to decrypt.
     * @param authenticationTag The authentication tag.
     * @param additionalAuthenticatedData An array of bytes representing the additional authenticated data.
     * @return An array of bytes representing the decrypted ciphertext.
     */
    byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext,
            byte[] authenticationTag, byte[] additionalAuthenticatedData);
}
