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
package org.forgerock.audit.events.handlers.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A buffered writer of text to an output stream, which fails every write and flush after the first one that fails.
 * <p>
 * After an {@link IOException}, the buffers of a {@link BufferedWriter} over an {@link OutputStreamWriter} are
 * unusable: the encoder is left with its byte buffer flipped, so a later flush writes nothing and returns, and a
 * close writes a part of a failed text longer than the buffers. Several threads write to the same writer, so the next
 * flush would report as written a text that the failure dropped. This writer fails it instead, with the first failure
 * as its cause, and closes the stream without writing what the buffers hold.
 */
final class FailFastWriter extends Writer {

    private final OutputStream stream;
    private final Writer writer;
    /** The first write or flush that failed. */
    private volatile IOException failure;

    /**
     * Creates a writer of text to an output stream.
     *
     * @param stream The output stream to write to.
     * @param charset The charset of the text in the output stream.
     */
    FailFastWriter(final OutputStream stream, final Charset charset) {
        this.stream = stream;
        this.writer = new BufferedWriter(new OutputStreamWriter(stream, charset));
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        checkNotFailed();
        try {
            writer.write(cbuf, off, len);
        } catch (IOException e) {
            throw fail(e);
        }
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        checkNotFailed();
        try {
            writer.write(str, off, len);
        } catch (IOException e) {
            throw fail(e);
        }
    }

    @Override
    public void flush() throws IOException {
        // Also fails the flush of a text written before the failure, which the failure may have dropped.
        checkNotFailed();
        try {
            writer.flush();
        } catch (IOException e) {
            throw fail(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (failure != null) {
            // What the buffers still hold is not written: the events of the failed texts are retried.
            stream.close();
        } else {
            writer.close();
        }
    }

    /**
     * Returns the first write or flush that failed.
     *
     * @return The first failure, or {@code null} if no write or flush failed.
     */
    IOException getFailure() {
        return failure;
    }

    private synchronized IOException fail(final IOException e) {
        if (failure == null) {
            failure = e;
        }
        return e;
    }

    private void checkNotFailed() throws IOException {
        final IOException firstFailure = failure;
        if (firstFailure != null) {
            throw new IOException("A previous write to the stream failed", firstFailure);
        }
    }
}
