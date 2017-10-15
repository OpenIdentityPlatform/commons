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
 * Portions Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.io;

import static org.forgerock.http.io.IO.newBranchingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.util.Factory;

/**
 * Represents a pipe for transferring bytes from an {@link java.io.OutputStream} to a
 * {@link org.forgerock.http.io.BranchingInputStream}.
 * This class is not thread-safe : the buffer has to be fully filled before reading from it : if the consumers reads
 * faster than the producer writes into it, then the consumer will get to the end of the buffer and that will be
 * interpreted an end-of-stream.
 */
public final class PipeBufferedStream {
    private final OutputStream outputStream;
    private final BranchingInputStream inputStream;
    /** The buffer will be closed once both the input and output stream are closed. */
    private final AtomicInteger bufferRefCount = new AtomicInteger(2);
    private final Buffer buffer;
    private int position = 0;

    /**
     * Constructs a new {@link PipeBufferedStream} with a default {@link Factory<Buffer>}.
     */
    public PipeBufferedStream() {
        this(IO.newTemporaryStorage());
    }

    /**
     * Constructs a new {@link PipeBufferedStream} with the given {@link Factory<Buffer>}.
     *
     * @param bufferFactory The buffer factory to use to create the {@link BranchingInputStream}
     */
    public PipeBufferedStream(final Factory<Buffer> bufferFactory) {
        outputStream = new PipeOutputStream();
        inputStream = newBranchingInputStream(new PipeInputStream(), bufferFactory);
        this.buffer = bufferFactory.newInstance();
    }

    /**
     * Returns the output stream which writes to the pipe.
     *
     * @return The output stream.
     */
    public OutputStream getIn() {
        return outputStream;
    }

    /**
     * Returns the input stream which reads from the pipe.
     *
     * @return The input stream.
     */
    public BranchingInputStream getOut() {
        return inputStream;
    }

    private void closeBufferIfNeeded() throws IOException {
        if (bufferRefCount.decrementAndGet() == 0) {
            buffer.close();
        }
    }

    private class PipeOutputStream extends OutputStream {
        @Override
        public void write(final int i) throws IOException {
            buffer.append((byte) i);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            buffer.append(b, off, len);
        }

        @Override
        public void close() throws IOException {
            closeBufferIfNeeded();
        }
    }

    private class PipeInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            return position < buffer.length() ? buffer.read(position++) : -1;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (position < buffer.length()) {
                final int readLength = buffer.read(position, b, off, len);
                position += readLength;
                return readLength;
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            closeBufferIfNeeded();
        }
    }
}
