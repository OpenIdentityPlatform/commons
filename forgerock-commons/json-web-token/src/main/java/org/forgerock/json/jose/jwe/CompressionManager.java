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

import org.forgerock.json.jose.exceptions.JweException;
import org.forgerock.json.jose.jwe.handlers.compression.CompressionHandler;
import org.forgerock.json.jose.jwe.handlers.compression.DeflateCompressionHandler;
import org.forgerock.json.jose.jwe.handlers.compression.NOPCompressionHandler;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

/**
 * A service to get the appropriate CompressionHandler for a specified Compression algorithm.
 * <p>
 * For details of all supported algorithms see {@link CompressionAlgorithm}.
 *
 * @since 2.0.0
 */
public class CompressionManager {

    /**
     * Gets the appropriate CompressionHandler that can perform the required compression using the given
     * compression algorithm.
     *
     * @param algorithm The Compression algorithm.
     * @return The CompressionHandler.
     */
    public CompressionHandler getCompressionHandler(CompressionAlgorithm algorithm) {

        switch (algorithm) {
        case NONE: {
            return new NOPCompressionHandler();
        }
        case DEF: {
            return new DeflateCompressionHandler();
        }
        default: {
            throw new JweException("No Compression Handler for unknown compression algorithm, "
                    + algorithm + ".");
        }
        }
    }

    /**
     * Convenience method equivalent to
     * {@code Base64url.encode(getCompressionHandler(compressionAlgorithm).compress(data.getBytes(Utils.CHARSET)))}.
     *
     * @param compressionAlgorithm the compression algorithm to use.
     * @param data the data to compress.
     * @return the base64url-encoded compressed data.
     */
    public String compress(CompressionAlgorithm compressionAlgorithm, String data) {
        return Base64url.encode(getCompressionHandler(compressionAlgorithm).compress(data.getBytes(Utils.CHARSET)));
    }

    /**
     * Convenience method equivalent to
     * {@code getCompressionHandler(compressionAlgorithm).decompress(Base64url.decode(data))}.
     *
     * @param compressionAlgorithm the compression algorithm to use.
     * @param data the base64url-encoded data to decompress.
     * @return the decompressed data.
     */
    public byte[] decompress(CompressionAlgorithm compressionAlgorithm, String data) {
        return getCompressionHandler(compressionAlgorithm).decompress(Base64url.decode(data));
    }
}
