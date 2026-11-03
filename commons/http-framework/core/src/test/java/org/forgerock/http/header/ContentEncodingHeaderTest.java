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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.http.header.ContentEncodingHeader.*;
import static org.forgerock.util.Utils.closeSilently;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class is unit testing the content encoding class.
 * See <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>
 * §14.11
 * Header field example :
 * <pre>
 * Content-Encoding: gzip
 * </pre>
 */
@SuppressWarnings("javadoc")
public class ContentEncodingHeaderTest {

    private static final String STRING_TO_DECODE = "People have forgotten this truth, the fox said."
            + " But you mustn't forget it. You become responsible forever for what you've tamed.";

    /**
     * See <a href="http://en.wikipedia.org/wiki/HTTP_compression">List of content encoding</a>
     */
    @DataProvider
    private Object[][] contentEncodingHeaders() {
        return new Object[][] {
            { "gzip" },
            { "deflate" },
            { "identity" },
            { "pack200-gzip" },
            { "compress" } };
    }

    @Test(dataProvider = "nullOrEmptyDataProvider", dataProviderClass = StaticProvider.class)
    public void testContentEncodingHeaderAllowsNullOrEmptyString(final String cheader) {
        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(cheader);
        assertThat(ceh.getValues()).isNullOrEmpty();
    }

    @Test(dataProvider = "contentEncodingHeaders")
    public void testContentEncodingHeaderFromMessageResponse(final String cheader) throws Exception {
        final Response response = new Response();
        assertThat(response.getHeaders().get(NAME)).isNull();
        response.getHeaders().put(NAME, cheader);

        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(response);
        assertThat(ceh.getName()).isEqualTo(NAME);
        assertThat(ceh.getCodings().size()).isEqualTo(1);
        assertThat(ceh.getCodings().get(0)).isEqualTo(cheader);
    }

    @Test(dataProvider = "contentEncodingHeaders")
    public void testContentEncodingHeaderFromMessageRequest(final String cheader) {
        final Request request = new Request();
        assertThat(request.getHeaders().get(NAME)).isNull();
        request.getHeaders().put(NAME, cheader);

        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(request);
        assertThat(ceh.getCodings().size()).isEqualTo(1);
        assertThat(ceh.getCodings().get(0)).isEqualTo(cheader);
    }

    @Test
    public void testContentEncodingHeaderFromEmptyMessage() {
        final Response response = new Response();
        assertThat(response.getHeaders().get(NAME)).isNull();

        final ContentEncodingHeader ch = ContentEncodingHeader.valueOf(response);
        assertThat(ch.getCodings()).isEmpty();
        assertThat(ch.getCodings().size()).isEqualTo(0);
    }

    @Test(dataProvider = "contentEncodingHeaders")
    public void testContentEncodingHeaderToMessageRequest(final String cheader) {
        final Request request = new Request();
        assertThat(request.getHeaders().get(NAME)).isNull();
        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(cheader);
        request.getHeaders().add(ceh);

        assertThat(request.getHeaders().get(NAME).getValues()).containsOnly(cheader);
    }

    @Test(dataProvider = "nullOrEmptyDataProvider", dataProviderClass = StaticProvider.class)
    public void testContentEncodingHeaderToMessageNullOrEmptyDoesNothing(final String cheader) {
        final Response response = new Response();
        assertThat(response.getHeaders().get(NAME)).isNull();

        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(cheader);
        response.getHeaders().add(ceh);

        assertThat(response.getHeaders().get(NAME)).isNull();
    }

    @Test(dataProvider = "nullOrEmptyDataProvider", dataProviderClass = StaticProvider.class)
    public void testDecodeWithNullOrEmptyDecoderHasNoEffectOnInputStream(final String cheader) throws Exception {
        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf(cheader);
        try (InputStream in = new ByteArrayInputStream(getStringToDecodeToCompressedBytes());
             InputStream out = ceh.decode(in)) {
            // The decode has no effects.
            assertThat(in).isEqualTo(out);
        }
    }

    @Test
    public void testDecodeSucceedEncodingGzipHeader() throws Exception {
        byte[] compressedBytes = getStringToDecodeToCompressedBytes();

        final ContentEncodingHeader ceh = ContentEncodingHeader.valueOf("gzip");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ceh.decode(new ByteArrayInputStream(compressedBytes))))) {
            String line;
            while ((line = br.readLine()) != null) {
                assertThat(line).isEqualTo(STRING_TO_DECODE);
            }
        }
    }

    private byte[] getStringToDecodeToCompressedBytes() throws Exception {
        final ByteArrayOutputStream baostream = new ByteArrayOutputStream();
        final OutputStream outStream = new GZIPOutputStream(baostream);
        try {
            outStream.write(STRING_TO_DECODE.getBytes("UTF-8"));
            outStream.close();
            return baostream.toByteArray();
        } finally {
            closeSilently(baostream, outStream);
        }
    }

    @Test(expectedExceptions = UnsupportedEncodingException.class)
    public void testDecodeDoesNotSupportCompressContentEncoding() throws Exception {

        final String toDecode = "The 'compress' content encoding header is not supported.";
        final InputStream is = new ByteArrayInputStream(toDecode.getBytes());
        ContentEncodingHeader.valueOf("compress").decode(is);
    }

    @DataProvider
    public Object[][] failingDecoderChains() throws IOException {
        byte[] plain = "not a gzip stream".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new Object[][] {
            { "gzip, gzip", gzip(plain) },
            { "gzip, gzip, gzip", gzip(gzip(plain)) },
            { "unsupported, gzip", gzip(plain) },
            { "gzip, deflate", deflate(plain) },
            { "unsupported, deflate", deflate(plain) }
        };
    }

    @Test(dataProvider = "failingDecoderChains")
    public void failedDecodeClosesConstructedWrappers(String codings, byte[] body) throws IOException {
        CloseTrackingInputStream source = new CloseTrackingInputStream(body);
        assertThatThrownBy(() -> valueOf(codings).decode(source)).isInstanceOf(IOException.class);
        assertThat(source.closes).isEqualTo(1);
    }

    @DataProvider
    public Object[][] failuresBeforeAnyWrapper() {
        return new Object[][] { { "gzip" }, { "unsupported" } };
    }

    @Test(dataProvider = "failuresBeforeAnyWrapper")
    public void failedDecodeWithoutWrapperLeavesSourceOpen(String coding) {
        CloseTrackingInputStream source = new CloseTrackingInputStream(new byte[0]);
        assertThatThrownBy(() -> valueOf(coding).decode(source)).isInstanceOf(IOException.class);
        assertThat(source.closes).isZero();
    }

    @DataProvider
    public Object[][] decodingAndClosingFailures() {
        return new Object[][] {
            { false, false, false }, { false, true, false }, { true, false, false }, { true, true, false },
            { false, false, true }, { true, true, true }
        };
    }

    @Test(dataProvider = "decodingAndClosingFailures")
    public void cleanupPreservesOriginalFailure(boolean runtimeRead, boolean runtimeClose, boolean sameFailure)
            throws IOException {
        Exception readFailure = runtimeRead ? new IllegalStateException("read failed") : new IOException("read failed");
        Exception closeFailure = sameFailure ? readFailure : runtimeClose ? new IllegalStateException("close failed")
                : new IOException("close failed");
        CloseTrackingInputStream source = new CloseTrackingInputStream(gzip(new byte[0])) {
            private int headerRemaining = 10;

            @Override
            public int read() throws IOException {
                int next = super.read();
                if (next != -1 && headerRemaining > 0) {
                    headerRemaining--;
                }
                return next;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                // Let the first decoder consume its fixed header before failing in its payload.
                if (headerRemaining > 0) {
                    int count = super.read(bytes, offset, Math.min(length, headerRemaining));
                    headerRemaining -= count;
                    return count;
                }
                throwFailure(readFailure);
                return -1;
            }

            @Override
            public void close() throws IOException {
                super.close();
                throwFailure(closeFailure);
            }
        };
        if (sameFailure) {
            assertThatThrownBy(() -> valueOf("gzip, gzip").decode(source)).isSameAs(readFailure)
                    .hasNoSuppressedExceptions();
        } else {
            assertThatThrownBy(() -> valueOf("gzip, gzip").decode(source)).isSameAs(readFailure)
                    .hasSuppressedException(closeFailure);
        }
        assertThat(source.closes).isEqualTo(1);
    }

    @Test
    public void successfulDecodeTransfersOwnershipToCaller() throws IOException {
        CloseTrackingInputStream source = new CloseTrackingInputStream(gzip(new byte[0]));
        try (InputStream decoded = valueOf("gzip").decode(source)) {
            assertThat(source.closes).isZero();
            assertThat(decoded.read()).isEqualTo(-1);
            assertThat(source.closes).isZero();
        }
        assertThat(source.closes).isEqualTo(1);
    }

    @DataProvider
    public Object[][] invalidInnerDeflateHeaders() {
        return new Object[][] { { new byte[] { 0x78 } }, { new byte[] { 0x78, 0x20 } } };
    }

    @Test(dataProvider = "invalidInnerDeflateHeaders")
    public void failedLazyDeflateReadStillClosesGzip(byte[] header) throws IOException {
        CloseTrackingInputStream source = new CloseTrackingInputStream(gzip(header));
        try (InputStream decoded = valueOf("deflate, gzip").decode(source)) {
            assertThat(source.closes).isZero();
            assertThatThrownBy(decoded::read).isInstanceOf(IOException.class);
        }
        assertThat(source.closes).isEqualTo(1);
    }

    private static byte[] gzip(byte[] body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(body);
        }
        return bytes.toByteArray();
    }

    private static byte[] deflate(byte[] body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(bytes)) {
            out.write(body);
        }
        return bytes.toByteArray();
    }

    private static void throwFailure(Exception failure) throws IOException {
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        throw (RuntimeException) failure;
    }

    private static class CloseTrackingInputStream extends FilterInputStream {
        int closes;

        CloseTrackingInputStream(byte[] body) {
            super(new ByteArrayInputStream(body));
        }

        @Override
        public void close() throws IOException {
            closes++;
            super.close();
        }
    }
}
