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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.jose.utils;

import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.forgerock.util.Reject;

/**
 * Utility methods for reading and writing DER-encoded values. This is just the absolute minimum needed to decode and
 * encode ECDSA signatures to ES256 format.
 */
public final class DerUtils {
    /**
     * DER tag for integer values.
     */
    public static final byte INTEGER_TAG = 0x02;
    /**
     * DER tag for sequence values.
     */
    public static final byte SEQUENCE_TAG = 0x30;

    private DerUtils() {
        // Utility class
    }

    /**
     * Reads an unsigned integer value into the given byte array. The output will be in big-endian format and aligned
     * to take up exactly {@code length} bytes (leaving untouched any unused leading bytes).
     *
     * @param input the input DER-encoded byte buffer.
     * @param output the output byte array.
     * @param offset the offset into the byte array to start writing the integer value.
     * @param length the maximum length of the byte value (excluding any leading sign byte).
     * @throws BufferOverflowException if the integer does not fit in the given output buffer slice.
     */
    public static void readUnsignedInteger(ByteBuffer input, byte[] output, int offset, int length) {
        Reject.ifFalse(input.get() == INTEGER_TAG, "Not an integer");
        int len = readLength(input);
        if (len > length) {
            // See if it fits if we assume the first byte is a redundant zero sign byte
            if (--len > length || input.get() != 0) {
                throw new BufferOverflowException();
            }
        }
        input.get(output, offset + (length - len), len);
    }

    /**
     * Writes an integer value in DER format to the given buffer.
     *
     * @param buffer the buffer to write the value to
     * @param data the integer value (in big-endian format) to write
     */
    public static void writeInteger(final ByteBuffer buffer, final byte[] data) {
        buffer.put(INTEGER_TAG);
        writeLength(buffer, data.length);
        buffer.put(data);
    }

    /**
     * Reads a DER-encoded length field from the given byte buffer.
     *
     * @param buffer the buffer to read a length field from.
     * @return the length field.
     */
    public static int readLength(final ByteBuffer buffer) {
        int b = buffer.get();
        // See http://luca.ntop.org/Teaching/Appunti/asn1.html. If the high-bit of the first byte is 0 then this byte
        // is the length. Otherwise the first byte (masking off the high bit) gives the number of following bytes
        // that encode the length (in big-endian unsigned integer format).
        if ((b & 0x80) == 0) {
            return b & 0xFF;
        } else {
            int numBytes = b & 0x7F;
            Reject.ifFalse(numBytes > 0 && numBytes < 4, "Unsupported DER length field");

            int len = 0;
            for (int i = 0; i < numBytes; ++i) {
                len = (len << 8) + (buffer.get() & 0xFF);
            }
            return len;
        }
    }

    /**
     * Writes a length field to the output.
     *
     * @param output the output buffer.
     * @param len the length to write.
     */
    public static void writeLength(final ByteBuffer output, final int len) {
        if (len < 128) {
            output.put((byte) len);
        } else if (len < (1 << 8)) {
            output.put((byte) 0x081);
            output.put((byte) len);
        } else if (len < (1 << 16)) {
            output.put((byte) 0x82);
            output.put((byte) (len >> 8));
            output.put((byte) len);
        } else if (len < (1 << 24)) {
            output.put((byte) 0x83);
            output.put((byte) (len >> 16));
            output.put((byte) (len >> 8));
            output.put((byte) len);
        } else {
            output.put((byte) 0x84);
            output.put((byte) (len >> 24));
            output.put((byte) (len >> 16));
            output.put((byte) (len >> 8));
            output.put((byte) len);
        }
    }
}