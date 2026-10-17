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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.events.handlers.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TextWriter provides a character-based stream which can be queried for number of bytes written.
 */
public interface TextWriter {
    /**
     * Writes some text to the output stream.
     *
     * @param text
     *            The text to write
     * @throws IOException
     *             If a problem occurs.
     */
    void write(String text) throws IOException;

    /**
     * Flushes any buffered contents of the output stream.
     *
     * @throws IOException
     *             If a problem occurs.
     */
    void flush() throws IOException;

    /**
     * Releases any resources held by the writer.
     */
    void shutdown();

    /**
     * Retrieves the number of bytes written by this writer.
     *
     * @return the number of bytes written by this writer.
     */
    long getBytesWritten();

    /**
     * A TextWriter implementation which writes to a given output stream.
     */
    public class Stream implements TextWriter {
        private static final Logger logger = LoggerFactory.getLogger(Stream.class);

        private final MeteredStream stream;
        private final Writer writer;
        /**
         * Set by the first write or flush that fails. The buffers of the writer are unusable after it: a later flush
         * may write nothing and return, or write a part of the failed text.
         */
        private volatile boolean failed;

        /**
         * Creates a new text writer that will write to the provided output stream.
         *
         * @param outputStream
         *            The output stream to which
         */
        public Stream(OutputStream outputStream) {
            stream = new MeteredStream(outputStream, 0);
            // Not a PrintWriter, which sets an error flag instead of throwing: a write that fails must fail the event.
            writer = new BufferedWriter(new OutputStreamWriter(stream));
        }

        @Override
        public void write(String text) throws IOException {
            checkNotFailed();
            try {
                writer.write(text);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void flush() throws IOException {
            // Also fails the flush of a text written before the failure, which the failure may have dropped.
            checkNotFailed();
            try {
                writer.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void shutdown() {
            try {
                if (failed) {
                    // Close the stream without writing what the buffers still hold: the failed events are retried.
                    stream.close();
                } else {
                    writer.close();
                }
            } catch (IOException e) {
                logger.error("Error when performing shutdown", e);
            }
        }

        private void checkNotFailed() throws IOException {
            if (failed) {
                throw new IOException("A previous write to the stream failed");
            }
        }

        @Override
        public long getBytesWritten() {
            return stream.getBytesWritten();
        }
    }
}
