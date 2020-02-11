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
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.http.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * A dynamically growing data buffer. Data can be read from any point within the buffer,
 * and written to the end of a buffer.
 */
public interface Buffer extends Closeable {

    /**
     * Reads a single byte of data from the buffer.
     *
     * @param pos the offset position, measured in bytes from the beginning of the buffer, at which to read the data.
     * @return byte at given offset position.
     * @throws IOException if an I/O exception occurs.
     * @throws IndexOutOfBoundsException if {@code pos} exceeds the buffer length.
     */
    byte read(int pos) throws IOException;

    /**
     * Reads up to {@code len} bytes of data from the buffer into an array of bytes. An
     * attempt is made to read as many as {@code len} bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * @param pos the offset position, measured in bytes from the beginning of the buffer, at which to read the data.
     * @param b the array of bytes to write the data to.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the number of bytes read into the array.
     * @throws IOException if an I/O exception occurs.
     */
    int read(int pos, byte[] b, int off, int len) throws IOException;

    /**
     * Appends a single byte to the end of the buffer.
     *
     * @param b the byte to append.
     * @throws IOException if an I/O exception occurs.
     * @throws OverflowException if appending a single byte to the buffer would exceed its limit.
     */
    void append(byte b) throws IOException;

    /**
     * Appends {@code len} bytes from the specified byte array starting at offset {@code off}
     * to the end of the buffer.
     *
     * @param b the array of bytes to read the data from.
     * @param off the start offset in the array at which the data is read.
     * @param len the number of bytes to be appended to the buffer.
     * @throws IOException if an I/O exception occurs.
     * @throws OverflowException if appending {@code len} bytes to the buffer would exceed its limit.
     */
    void append(byte[] b, int off, int len) throws IOException;

    /**
     * Returns the current length of the buffer.
     *
     * @return the length of the file, measured in bytes.
     * @throws IOException if an I/O exception occurs.
     */
    int length() throws IOException;

    /**
     * Closes the buffer and releases any system resources associated with it. A closed buffer
     * cannot perform input or output operations and cannot be reopened.
     *
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    void close() throws IOException;
}
