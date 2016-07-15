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
package org.forgerock.util.crypto;

/**
 * Constants for Crypto Algorithms and Json Crypto Json pointer keys.
 */
public final class CryptoConstants {

    /**
     * key for the crypto json object.
     */
    public static final String CRYPTO = "$crypto";

    /**
     * key for crypt type used to generate the crypt value.
     */
    public static final String CRYPTO_TYPE = "type";

    /**
     * key for the crypt value, holding the crypt meta-data.
     */
    public static final String CRYPTO_VALUE = "value";

    /**
     * Key for the crypto algorithm used to crypt the data.
     */
    public static final String CRYPTO_ALGORITHM = "algorithm";

    /**
     * key for the password data within crypto json.
     */
    public static final String CRYPTO_DATA = "data";

    /**
     * key for the name of the key-store alias used to crypt the data.
     */
    public static final String CRYPTO_KEY = "key";

    /**
     * key for the cipher used to crypt the data.
     */
    public static final String CRYPTO_CIPHER = "cipher";

    /**
     * A cipher value for the AES/CBC/PKCS5Padding algorithm.
     */
    public static final String CIPHER_AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";

    /**
     * key for the Initialization Vector (a.k.a. salt) used to crypt the data.
     */
    public static final String CRYPTO_IV = "iv";

    /**
     * A salted hash encryption storage type.
     */
    public static final String STORAGE_TYPE_HASH = "salted-hash";

    /**
     * The name of the message digest algorithm that should be used to generate MD5 hashes.
     */
    public static final String ALGORITHM_MD5 = "MD5";

    /**
     * The name of the message digest algorithm that should be used to generate SHA-1 hashes.
     */
    public static final String ALGORITHM_SHA_1 = "SHA-1";

    /**
     * The name of the message digest algorithm that should be used to generate 256-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_256 = "SHA-256";

    /**
     * The name of the message digest algorithm that should be used to generate 384-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_384 = "SHA-384";

    /**
     * The name of the message digest algorithm that should be used to generate 512-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_512 = "SHA-512";

    private CryptoConstants() {
        throw new UnsupportedOperationException();
    }
}
