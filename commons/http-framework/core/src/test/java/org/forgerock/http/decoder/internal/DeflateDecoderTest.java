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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

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

    @Test
    public void openingAndClosingWithoutReadingDoesNotReadTheSource() throws IOException {
        final boolean[] closed = { false };
        InputStream source = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("The source must not be read yet");
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };
        InputStream decoded = new DeflateDecoder().decode(source);
        assertThat(decoded.available()).isZero();
        assertThat(decoded.read(new byte[0])).isZero();
        assertThat(decoded.skip(0)).isZero();
        assertThatThrownBy(() -> decoded.read(new byte[1], 1, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> decoded.read(null, 0, 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> decoded.skip(-1)).isInstanceOf(IllegalArgumentException.class);
        decoded.close();
        decoded.close();
        assertThat(closed[0]).isTrue();
        assertThatThrownBy(decoded::read).isInstanceOf(IOException.class);
        assertThatThrownBy(decoded::available).isInstanceOf(IOException.class);
    }

    @DataProvider
    public Object[][] truncatedHeaders() {
        return new Object[][] { { new byte[0] }, { new byte[] { 0x78 } } };
    }

    @Test(dataProvider = "truncatedHeaders")
    public void entityReportsTruncatedHeaderOnRead(byte[] compressed) throws IOException {
        try (Response response = new Response()) {
            response.setEntity(compressed);
            response.getHeaders().put(ContentEncodingHeader.NAME, "deflate");
            try (InputStream decoded = response.getEntity().newDecodedContentInputStream()) {
                assertThatThrownBy(decoded::read).isInstanceOf(EOFException.class)
                        .hasMessage("Truncated deflate header");
            }
            try (Reader decoded = response.getEntity().newDecodedContentReader(UTF_8)) {
                assertThatThrownBy(decoded::read).isInstanceOf(EOFException.class)
                        .hasMessage("Truncated deflate header");
            }
        }
    }

    @Test
    public void initializationFailureCannotBeBypassedByReadingAgain() throws IOException {
        // A valid zlib header with FDICT, followed by a valid independent zlib stream.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(new byte[] { 0x78, 0x20 });
        bytes.write(compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION));
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(bytes.toByteArray()))) {
            for (int attempt = 0; attempt < 2; attempt++) {
                assertThatThrownBy(decoded::read).isInstanceOf(ZipException.class)
                        .hasMessage("Preset dictionaries are not supported for deflate content encoding");
            }
        }
    }

    @Test
    public void skipConsumesDecodedBytes() throws IOException {
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(
                compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION)))) {
            assertThat(decoded.skip(2)).isEqualTo(2);
            assertThat(decoded.readAllBytes()).isEqualTo(Arrays.copyOfRange(CONTENT, 2, CONTENT.length));
            assertThat(decoded.available()).isZero();
        }
    }

    @Test
    public void reportsZlibErrorsWithoutRawRetry() throws IOException {
        byte[] zlib = compress(CONTENT, false, Deflater.DEFAULT_COMPRESSION);
        byte[] badChecksum = zlib.clone();
        badChecksum[badChecksum.length - 1] ^= 1;
        assertThatThrownBy(() -> decodeFully(badChecksum)).isInstanceOf(ZipException.class)
                .hasMessage("incorrect data check");
        assertThatThrownBy(() -> decodeFully(Arrays.copyOf(zlib, zlib.length - 1)))
                .isInstanceOf(EOFException.class).hasMessage("Unexpected end of ZLIB input stream");
    }

    @DataProvider
    public Object[][] rawStreamsWithNonZeroStoredBlockPadding() {
        return new Object[][] {
            { 0x08, "hello".getBytes(UTF_8) }, // CM 8 and CINFO 0, but FCHECK fails.
            { 0x88, Arrays.copyOf(CONTENT, 28) } // CM 8 and valid FCHECK, but CINFO 8.
        };
    }

    @Test(dataProvider = "rawStreamsWithNonZeroStoredBlockPadding")
    public void decodesRawStreamsThatOnlyResembleZlib(int first, byte[] content) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        raw.write(first); // Non-final stored block; padding bits need not be zero.
        raw.write(content.length);
        raw.write(0);
        raw.write(~content.length);
        raw.write(0xff);
        raw.write(content);
        raw.write(new byte[] { 0x03, 0x00 }); // Final empty fixed-Huffman block.
        assertThat(decodeFully(raw.toByteArray())).isEqualTo(content);
    }

    @DataProvider
    public Object[][] inflaterLifecycles() {
        return new Object[][] { { false, false }, { true, false }, { false, true }, { true, true } };
    }

    @Test(dataProvider = "inflaterLifecycles")
    @SuppressWarnings("try") // Explicit close calls exercise failure and repeated-close behavior.
    public void closingDecodedStreamEndsInflater(boolean raw, boolean closeFails) throws IOException {
        final int[] ends = { 0 };
        Inflater inflater = new Inflater(raw) {
            @Override
            public void end() {
                ends[0]++;
                super.end();
            }
        };
        DeflateDecoder decoder = new DeflateDecoder() {
            @Override
            Inflater newInflater(boolean nowrap) {
                assertThat(nowrap).isEqualTo(raw);
                return inflater;
            }
        };
        IOException closeFailure = new IOException("source close failed");
        InputStream source = new ByteArrayInputStream(compress(CONTENT, raw, Deflater.DEFAULT_COMPRESSION)) {
            @Override
            public void close() throws IOException {
                if (closeFails) {
                    throw closeFailure;
                }
            }
        };
        try (InputStream decoded = decoder.decode(source)) {
            assertThat(decoded.read()).isEqualTo(CONTENT[0]);
            assertThat(ends[0]).isZero();
            if (closeFails) {
                assertThatThrownBy(decoded::close).isSameAs(closeFailure);
            } else {
                decoded.close();
            }
            decoded.close();
            assertThat(ends[0]).isEqualTo(1);
            assertThatThrownBy(decoded::read).isInstanceOf(IOException.class);
        } finally {
            inflater.end();
        }
    }

    private static byte[] decodeFully(byte[] compressed) throws IOException {
        try (InputStream decoded = new DeflateDecoder().decode(new ByteArrayInputStream(compressed))) {
            return decoded.readAllBytes();
        }
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
