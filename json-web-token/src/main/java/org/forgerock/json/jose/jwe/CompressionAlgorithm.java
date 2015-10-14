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

package org.forgerock.json.jose.jwe;

/**
 * An Enum of the possible compression algorithms that can be applied to the JWE payload plaintext.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-4.1.4">
 *     JWE Compression Algorithm</a>
 *
 * @since 2.0.0
 */
public enum CompressionAlgorithm {

    /** When no compression is applied. */
    NONE,
    /**
     * A lossless compressed data format that compresses data using a combination of the LZ77 algorithm and Huffman
     * coding.
     */
    DEF;

    /**
     * Parses the given algorithm string to find the matching CompressionAlgorithm enum constant.
     *
     * @param algorithm The compression algorithm.
     * @return The CompressionAlgorithm enum.
     */
    public static CompressionAlgorithm parseAlgorithm(String algorithm) {
        try {
            return CompressionAlgorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

}
