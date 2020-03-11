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

package org.forgerock.json.jose.jwe;

import java.util.Locale;

import org.forgerock.json.jose.exceptions.JweException;

/**
 * An Enum of the possible encryption methods that can be used when encrypting a JWT.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-11#section-4.2">
 *     JWE Encryption Methods</a>
 *
 * @since 2.0.0
 */
public enum EncryptionMethod {

    /**
     * AES encryption in CBC mode with PKCS5 Padding and a 128 bit length, AES encryption for CEK, HMAC using SHA-256
     * hash algorithm for authentication tag.
     */
    A128CBC_HS256("AES_128_CBC_HMAC_SHA_256", "AES/CBC/PKCS5Padding", "HMACSHA256", "AES", 16, 256),
    /**
     * AES encryption in CBC mode with PKCS5 Padding and a 192 bit length, AES encryption for CEK, HMAC using SHA-384
     * hash algorithm for the authentication tag.
     */
    A192CBC_HS384("AES_192_CBC_HMAC_SHA_384", "AES/CBC/PKCS5Padding", "HMACSHA384", "AES", 24, 384),
    /**
     * AES encryption in CBC mode with PKCS5 Padding and a 256 bit length, AES encryption for CEK, HMAC using SHA-256
     * hash algorithm for authentication tag.
     */
    A256CBC_HS512("AES_256_CBC_HMAC_SHA_512", "AES/CBC/PKCS5Padding", "HMACSHA512", "AES", 32, 512),
    /**
     * AES encryption in Galois Counter Mode (GCM) with a 128 bit key length.
     */
    A128GCM("AES_128_GCM", "AES/GCM/NoPadding", null, "AES", 16, 128),
    /**
     * AES encryption in Galois Counter Mode (GCM) with a 192 bit key length.
     */
    A192GCM("AES_192_GCM", "AES/GCM/NoPadding", null, "AES", 24, 192),
    /**
     * AES encryption in Galois Counter Mode (GCM) with a 256 bit key length.
     */
    A256GCM("AES_256_GCM", "AES/GCM/NoPadding", null, "AES", 32, 256);

    private final String name;
    private final String transformation;
    private final String macAlgorithm;
    private final String encryptionAlgorithm;
    private final int keyOffset;
    private final int keySize;

    /**
     * Constructs a new EncryptionMethod with the given cryptographic parameters.
     *
     * @param name The full name of the encryption algorithm.
     * @param transformation The Java Cryptographic algorithm name for the algorithm that will be used to encrypt the
     *                       plaintext.
     * @param macAlgorithm The Java Cryptographic algorithm name for the algorithm that will generate the MAC key.
     * @param encryptionAlgorithm The Java Cryptographic algorithm name for the algorithm that will create the Content
     *                            Encryption Key (CEK).
     * @param keyOffset The number of octets in each of the CEK and MAC key.
     * @param keySize The bit length of the Content Encryption Key (CEK).
     */
    EncryptionMethod(String name, String transformation, String macAlgorithm, String encryptionAlgorithm,
            int keyOffset, int keySize) {
        this.name = name;
        this.transformation = transformation;
        this.macAlgorithm = macAlgorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.keyOffset = keyOffset;
        this.keySize = keySize;
    }

    /**
     * Gets the full name of the encryption method.
     *
     * @return The name of the encryption method.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Java Cryptographic algorithm name for the algorithm that will eb used to encrypt the plaintext.
     *
     * @return The transformation algorithm.
     */
    public String getTransformation() {
        return transformation;
    }

    /**
     * Gets the Java Cryptographic algorithm name for the algorithm that will generate the MAC key.
     *
     * @return The mac algorithm.
     */
    public String getMacAlgorithm() {
        return macAlgorithm;
    }

    /**
     * Gets the Java Cryptographic algorithm name for the algorithm that will create the Content Encryption Key (CEK).
     *
     * @return The encryption algorithm.
     */
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    /**
     * Gets the number of octets in each of the CEK and MAC key.
     *
     * @return The Key Offset.
     */
    public int getKeyOffset() {
        return keyOffset;
    }

    /**
     * Gets the bit length of the Content Encryption Key (CEK).
     *
     * @return The key size.
     */
    public int getKeySize() {
        return keySize;
    }

    /**
     * Parses the given algorithm string to find the matching EncryptionMethod enum constant.
     *
     * @param method The encryption method.
     * @return The EncryptionMethod enum.
     */
    public static EncryptionMethod parseMethod(String method) {
        try {
            return EncryptionMethod.valueOf(method.toUpperCase(Locale.ROOT).replaceAll("-", "_"));
        } catch (IllegalArgumentException e) {
            for (EncryptionMethod encryptionMethod : EncryptionMethod.values()) {
                if (encryptionMethod.getName().equalsIgnoreCase(method)) {
                    return encryptionMethod;
                }
            }
        }

        throw new JweException("Unknown Encryption Method, " + method);
    }

    /**
     * Turns the EncryptionMethod constant into a JSON value string.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString().replaceAll("_", "-");
    }
}
