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
 */
package org.forgerock.audit.events.handlers.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

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
        private final MeteredStream stream;
        private final PrintWriter writer;

        /**
         * Creates a new text writer that will write to the provided output stream.
         *
         * @param outputStream
         *            The output stream to which
         */
        public Stream(OutputStream outputStream) {
            stream = new MeteredStream(outputStream, 0);
            writer = new PrintWriter(stream, true);
        }

        @Override
        public void write(String text) {
            writer.print(text);
        }

        @Override
        public void flush() {
            writer.flush();
        }

        @Override
        public void shutdown() {
            writer.close();
        }

        @Override
        public long getBytesWritten() {
            return stream.getBytesWritten();
        }
    }
}
