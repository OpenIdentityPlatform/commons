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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.json.jose.jwe.handlers.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.forgerock.json.jose.exceptions.JweCompressionException;

/**
 * An implementation of the CompressionHandler for DEFLATE Compressed Data Format Specification.
 * @see <a href="http://tools.ietf.org/html/rfc1951">DEFLATE Compressed Data Format Specification version 1.3</a>
 *
 */
public class DeflateCompressionHandler implements CompressionHandler {

    private static final int DEFAULT_MAX_DECOMPRESSED_BYTES = 1024 * 1024;
    private static final int MAX_DECOMPRESSED_BYTES = getMaxDecompressedBytes();

    private static int getMaxDecompressedBytes() {
        try {
            int limit = Integer.getInteger("org.openidentityplatform.jwt.maxDecompressedBytes",
                    DEFAULT_MAX_DECOMPRESSED_BYTES);
            return limit > 0 ? limit : DEFAULT_MAX_DECOMPRESSED_BYTES;
        } catch (SecurityException e) {
            return DEFAULT_MAX_DECOMPRESSED_BYTES;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] compress(byte[] bytes) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteOutputStream,
                     new Deflater(Deflater.DEFLATED, true))) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new JweCompressionException("Failed to apply compression algorithm.", e);
        }

        return byteOutputStream.toByteArray();
    }

    /**
     * {@inheritDoc}
     *
     * @throws JweCompressionException If decompression fails or the output exceeds the configured byte limit.
     */
    @Override
    public byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Inflater inflater = new Inflater(true);
        try (InflaterInputStream inflaterInputStream =
                     new InflaterInputStream(new ByteArrayInputStream(bytes), inflater)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inflaterInputStream.read(buffer)) != -1) {
                if (length > MAX_DECOMPRESSED_BYTES - out.size()) {
                    throw new JweCompressionException("Decompressed data exceeds the maximum size of "
                            + MAX_DECOMPRESSED_BYTES + " bytes.");
                }
                out.write(buffer, 0, length);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new JweCompressionException("Failed to apply de-compression algorithm.", e);
        } finally {
            inflater.end();
        }
    }
}
