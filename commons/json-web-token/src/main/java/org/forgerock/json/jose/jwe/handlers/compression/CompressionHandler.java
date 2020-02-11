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

package org.forgerock.json.jose.jwe.handlers.compression;

/**
 * The interface for CompressionHandlers for all the different compression algorithms.
 * <p>
 * Provides methods for compressing and decompression byte arrays.
 *
 * @since 2.0.0
 */
public interface CompressionHandler {

    /**
     * Applies the compression algorithm to compress the given array of bytes.
     *
     * @param bytes The array of bytes to compress.
     * @return The compressed representation of the byte array.
     */
    byte[] compress(byte[] bytes);

    /**
     * Applies the compression algorithm to decompress the given array of bytes.
     *
     * @param bytes The array of bytes to decompress.
     * @return The decompressed representation of the byte array.
     */
    byte[] decompress(byte[] bytes);
}
