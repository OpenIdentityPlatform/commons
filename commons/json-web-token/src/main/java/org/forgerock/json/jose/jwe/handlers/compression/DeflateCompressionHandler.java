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
 * <p>
 * @see <a href="http://tools.ietf.org/html/rfc1951">DEFLATE Compressed Data Format Specification version 1.3</a>
 *
 */
public class DeflateCompressionHandler implements CompressionHandler {

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
     */
    @Override
    public byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        try (InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(bytes),
                new Inflater(true));
             ByteArrayOutputStream out = byteOutputStream) {
            byte[] buffer = new byte[1024];
            int l;
            while ((l = inflaterInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, l);
            }
        } catch (IOException e) {
            throw new JweCompressionException("Failed to apply de-compression algorithm.", e);
        }

        return byteOutputStream.toByteArray();
    }
}
