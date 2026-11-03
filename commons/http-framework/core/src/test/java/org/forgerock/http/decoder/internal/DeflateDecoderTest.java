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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.http.decoder.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.forgerock.http.header.ContentEncodingHeader;
import org.forgerock.http.protocol.Message;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class DeflateDecoderTest {
    private static final byte[] CONTENT = "A deflate HTTP entity: \u00e9\u00e8\u00e0".getBytes(UTF_8);

    @DataProvider
    public Object[][] compressedBodies() throws IOException {
        return new Object[][] {
            { compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION), CONTENT },
            { compress(CONTENT, false, Deflater.NO_COMPRESSION), CONTENT },
            { compress(CONTENT, false, Deflater.BEST_SPEED), CONTENT },
            { compress(CONTENT, false, Deflater.BEST_COMPRESSION), CONTENT },
            { compress(CONTENT, true, Deflater.DEFAULT_COMPRESSION), CONTENT },
            { compress(CONTENT, true, Deflater.NO_COMPRESSION), CONTENT },
            { compress(new byte[0], false, Deflater.DEFAULT_COMPRESSION), new byte[0] },
            { compress(new byte[0], true, Deflater.DEFAULT_COMPRESSION), new byte[0] },
            // RFC 1950 permits window sizes smaller than the usual 32 KiB (0x78).
            { new byte[] { 0x08, 0x1d, (byte) 0xcb, 0x48, (byte) 0xcd, (byte) 0xc9,
                (byte) 0xc9, 0x07, 0x00, 0x06, 0x2c, 0x02, 0x15 }, "hello".getBytes(UTF_8) }
        };
    }

    @Test(dataProvider = "compressedBodies")
    public void decodesZlibAndLegacyRawBodies(byte[] compressed, byte[] expected) throws IOException {
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(compressed))) {
            assertThat(decoded.readAllBytes()).isEqualTo(expected);
        }
    }

    @Test(dataProvider = "compressedBodies")
    public void decodesFragmentedStreamsWithoutMarkSupport(byte[] compressed, byte[] expected) throws IOException {
        try (InputStream input = new FilterInputStream(new ByteArrayInputStream(compressed)) {
            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                return super.read(bytes, offset, Math.min(length, 1));
            }

            @Override
            public boolean markSupported() {
                return false;
            }

            @Override
            public void reset() throws IOException {
                throw new IOException("Mark/reset is not supported");
            }
        }; InputStream decoded = new DeflateDecoder().decode(input)) {
            assertThat(decoded.readAllBytes()).isEqualTo(expected);
        }
    }

    @DataProvider
    public Object[][] messages() {
        return new Object[][] {
            { new Request(), false }, { new Response(), false },
            { new Request(), true }, { new Response(), true }
        };
    }

    @Test(dataProvider = "messages")
    public void decodesRequestAndResponseEntities(Message message, boolean raw) throws IOException {
        try (Message ignored = message) {
            message.setEntity(compress(CONTENT, raw, Deflater.DEFAULT_COMPRESSION));
            message.getHeaders().put(ContentEncodingHeader.NAME, "deflate");
            message.getHeaders().put("Content-Type", "text/plain; charset=UTF-8");
            assertThat(message.getEntity().getString()).isEqualTo(new String(CONTENT, UTF_8));
            try (InputStream decoded = message.getEntity().newDecodedContentInputStream()) {
                assertThat(decoded.readAllBytes()).isEqualTo(CONTENT);
            }
        }
    }

    @Test
    public void decodesStackedContentEncodings() throws IOException {
        ByteArrayOutputStream gzip = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(gzip)) {
            output.write(CONTENT);
        }
        byte[] compressed = compress(gzip.toByteArray(), false, Deflater.DEFAULT_COMPRESSION);
        try (InputStream decoded = ContentEncodingHeader.valueOf("gzip, deflate")
                .decode(new ByteArrayInputStream(compressed))) {
            assertThat(decoded.readAllBytes()).isEqualTo(CONTENT);
        }
    }

    @DataProvider
    public Object[][] malformedBodies() throws IOException {
        byte[] zlib = compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION);
        byte[] raw = compress(CONTENT, true, Deflater.DEFAULT_COMPRESSION);
        byte[] badChecksum = zlib.clone();
        badChecksum[badChecksum.length - 1] ^= 1;
        return new Object[][] {
            { new byte[0] }, { new byte[] { 0x78 } }, { new byte[] { 0x78, (byte) 0x9c } },
            { Arrays.copyOf(zlib, zlib.length - 1) }, { Arrays.copyOf(raw, raw.length - 1) },
            { badChecksum }, { new byte[] { 0x07, 0x00 } }
        };
    }

    @Test(dataProvider = "malformedBodies", expectedExceptions = IOException.class)
    public void rejectsMalformedBodies(byte[] compressed) throws IOException {
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(compressed))) {
            decoded.readAllBytes();
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void rejectsBodiesRequiringAPresetDictionary() throws IOException {
        Deflater deflater = new Deflater();
        deflater.setDictionary(CONTENT);
        byte[] compressed = compress(CONTENT, deflater);
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(compressed))) {
            decoded.readAllBytes();
        }
    }

    @Test
    public void closingDecodedStreamClosesSource() throws IOException {
        final boolean[] closed = { false };
        InputStream source = new ByteArrayInputStream(compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION)) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        new DeflateDecoder().decode(source).close();
        assertThat(closed[0]).isTrue();
    }

    private static byte[] compress(byte[] content, boolean raw, int level) throws IOException {
        return compress(content, new Deflater(level, raw));
    }

    private static byte[] compress(byte[] content, Deflater deflater) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream output = new DeflaterOutputStream(bytes, deflater)) {
            output.write(content);
        } finally {
            deflater.end();
        }
        return bytes.toByteArray();
    }
}
