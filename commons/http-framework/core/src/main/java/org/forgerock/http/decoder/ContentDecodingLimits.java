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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.http.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Resource limits for HTTP content decoding. By default, at most four coding layers
 * are allowed, with 64 MiB of output per layer and 1 MiB when decoding a request body
 * or reading request JSON.
 * Override these limits with positive values for {@code org.openidentityplatform.http.maxContentEncodingLayers},
 * {@code org.openidentityplatform.http.maxDecodedContentBytes}, and {@code org.openidentityplatform.http.maxRequestContentBytes},
 * respectively. Invalid values use the defaults.
 */
public final class ContentDecodingLimits {
    private static final int DEFAULT_MAX_LAYERS = 4;
    private static final long DEFAULT_MAX_DECODED_BYTES = 64L * 1024 * 1024;
    private static final long DEFAULT_MAX_REQUEST_BYTES = 1024L * 1024;

    private ContentDecodingLimits() {
    }

    /**
     * Checks the number of content codings before opening any decoder.
     *
     * @param layers the number of content codings
     * @throws IOException if the number of layers exceeds the configured limit
     */
    public static void checkLayerCount(long layers) throws IOException {
        int limit = Integer.getInteger("org.openidentityplatform.http.maxContentEncodingLayers", DEFAULT_MAX_LAYERS);
        if (limit <= 0) {
            limit = DEFAULT_MAX_LAYERS;
        }
        if (layers > limit) {
            throw new IOException("Too many content encodings. Maximum is " + limit);
        }
    }

    /**
     * Limits the bytes returned by one decoder, including intermediate layers.
     *
     * @param decoded stream exposing the output of the decoder
     * @return a stream that enforces the byte limit and owns the decoded stream
     */
    public static InputStream limitDecodedStream(InputStream decoded) {
        long limit = Long.getLong("org.openidentityplatform.http.maxDecodedContentBytes", DEFAULT_MAX_DECODED_BYTES);
        return new LimitedContentStream(decoded, limit > 0 ? limit : DEFAULT_MAX_DECODED_BYTES, "Decoded content");
    }

    /**
     * Limits the bytes read from the final request body, including bodies without content coding.
     *
     * @param content stream exposing the request body after any decoding
     * @return a stream that enforces the request byte limit and owns the content stream
     */
    public static InputStream limitRequestContent(InputStream content) {
        long limit = Long.getLong("org.openidentityplatform.http.maxRequestContentBytes", DEFAULT_MAX_REQUEST_BYTES);
        return new LimitedContentStream(content, limit > 0 ? limit : DEFAULT_MAX_REQUEST_BYTES, "Request content");
    }

    private static final class LimitedContentStream extends InputStream {
        private final InputStream content;
        private final long limit;
        private final String description;
        private long remaining;
        private boolean limitExceeded;
        private long markedRemaining;
        private boolean markedLimitExceeded;

        LimitedContentStream(InputStream content, long limit, String description) {
            this.content = content;
            this.limit = limit;
            this.description = description;
            this.remaining = limit;
            this.markedRemaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) {
                // Allow EOF exactly at the limit, without returning any excess bytes.
                if (limitExceeded || content.read() != -1) {
                    limitExceeded = true;
                    throw new IOException(description + " exceeds " + limit + " bytes");
                }
                return -1;
            }

            int value = content.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            if (remaining == 0) {
                return read();
            }

            int count = content.read(bytes, offset, (int) Math.min(length, remaining));
            if (count > 0) {
                remaining -= count;
            }
            return count;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(remaining, content.available());
        }

        @Override
        public boolean markSupported() {
            return content.markSupported();
        }

        @Override
        public synchronized void mark(int readLimit) {
            content.mark(readLimit);
            markedRemaining = remaining;
            markedLimitExceeded = limitExceeded;
        }

        @Override
        public synchronized void reset() throws IOException {
            content.reset();
            remaining = markedRemaining;
            limitExceeded = markedLimitExceeded;
        }

        @Override
        public void close() throws IOException {
            content.close();
        }
    }
}
