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
 * Portions copyright 2016 ForgeRock AS.
 */

package org.forgerock.audit.handlers.json;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Wraps a {@link ByteBuffer} to expose the {@link OutputStream} interface, and grows the underlying buffer as
 * data is added to it.
 */
class ByteBufferOutputStream extends OutputStream {

    /**
     * Buffer grows 30% when re-sized.
     */
    private static final double GROWTH_RATE = 1.33;

    private ByteBuffer buffer;

    /**
     * Creates a {@code ByteBufferOutputStream} with the given {@link ByteBuffer}.
     *
     * @param buffer Initial buffer instance
     */
    public ByteBufferOutputStream(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Gets the underlying buffer.
     *
     * @return Underlying buffer
     */
    public ByteBuffer byteBuffer() {
        return buffer;
    }

    /**
     * Clears the buffer, but does not erase the underlying data, making it ready to be reused to accept new data.
     */
    public void clear() {
        buffer.clear();
    }

    /**
     * Replaces the existing buffer with a new buffer having increased size.
     *
     * @param newCapacity New capacity
     */
    private void growTo(final int newCapacity) {
        final ByteBuffer oldBuffer = buffer;
        if (buffer.isDirect()) {
            buffer = ByteBuffer.allocateDirect(newCapacity);
        } else {
            buffer = ByteBuffer.allocate(newCapacity);
        }
        // copy data
        oldBuffer.flip();
        buffer.put(oldBuffer);
    }

    @Override
    public void write(final int b) {
        if (buffer.remaining() == 0) {
            growTo((int) (buffer.capacity() * GROWTH_RATE));
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(final byte[] bytes) {
        if (buffer.remaining() < bytes.length) {
            final int newSize = Math.max((int) (buffer.capacity() * GROWTH_RATE),
                    buffer.position() + bytes.length);
            growTo(newSize);
        }
        buffer.put(bytes);
    }

    @Override
    public void write(final byte[] bytes, final int off, final int len) {
        if (len < 0) {
            throw new IllegalArgumentException("len must be positive");
        }
        if (buffer.remaining() < len) {
            final int newSize = Math.max((int) (buffer.capacity() * GROWTH_RATE), buffer.position() + len);
            growTo(newSize);
        }
        buffer.put(bytes, off, len);
    }
}
