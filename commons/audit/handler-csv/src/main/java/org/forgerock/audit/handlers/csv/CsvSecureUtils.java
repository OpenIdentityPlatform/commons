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

package org.forgerock.audit.handlers.csv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;

/**
 * Holds the methods shared between the CsvSecure classes.
 */
final class CsvSecureUtils {

    /**
     * The first byte of the data that the HMAC of a row covers. UTF-8 never holds it, so these data never equal
     * {@link #concatenatedValues(Map, String...)}, the UTF-8 bytes of a row that a previous version signed: an HMAC
     * verifies a row in one format only.
     */
    private static final int DATA_TO_SIGN_MARKER = 0xFF;

    /**
     * A line break that the CSV writer writes as its end of line symbols: a CR, a CRLF, or a LF, which the CSV reader
     * reads back as a LF. The writer writes a CR followed by a LF as one line break.
     */
    private static final Pattern LINE_BREAK = Pattern.compile("\r\n?");

    /**
     * Obtain the data to sign.
     * @param lastSignature The last signature.
     * @param lastHMAC The last HMAC.
     * @return The data.
     */
    static byte[] dataToSign(byte[] lastSignature, String lastHMAC) {
        byte[] toSign;
        if (lastSignature == null) {
            // Only the last HMAC will be signed
            byte[] prevHMAC = Base64.decode(lastHMAC);
            toSign = Arrays.copyOf(prevHMAC, prevHMAC.length);
        } else {
            // Both the last HMAC and the last signature will be signed
            byte[] prevHMAC = Base64.decode(lastHMAC);
            toSign = concat(prevHMAC, lastSignature);
        }
        return toSign;
    }

    private static byte[] concat(byte[]... arrays) {
        int length;

        // Find the length of the result array
        length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];

        // Really concatenate all the arrays
        length = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, length, array.length);
            length += array.length;
        }

        return result;
    }

    /**
     * Get the data to sign: the byte 0xFF, then the name and the value of each column, as the CSV reader reads them
     * back, in UTF-8, each one after the number of its bytes as a big-endian int.
     * <p>
     * The CSV reader reads an empty name or value as {@code null}, so both are signed as empty, and a line break
     * as a LF, so a CR or a CRLF is signed as a LF.
     * @param logger the value of logger
     * @param values the value of values
     * @param nameMapping the value of nameMapping
     */
    static byte[] dataToSign(Logger logger, Map<String, ?> values, String... nameMapping) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(DATA_TO_SIGN_MARKER);
        for (String h : nameMapping) {
            final Object value = values.get(h);
            writeLengthAndBytes(data, readBack(h));
            writeLengthAndBytes(data, readBack(value != null ? value.toString() : null));
        }
        return data.toByteArray();
    }

    private static void writeLengthAndBytes(ByteArrayOutputStream data, String field) {
        final byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
        data.write(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array(), 0, Integer.BYTES);
        data.write(bytes, 0, bytes.length);
    }

    /**
     * Returns a name or a value as the CSV reader reads it back once written: empty if it is {@code null} or empty,
     * with each line break as a LF.
     * @param field the name or the value, may be {@code null}
     * @return the name or the value as the CSV reader reads it back, empty for {@code null}
     */
    static String readBack(String field) {
        return field != null ? LINE_BREAK.matcher(field).replaceAll("\n") : "";
    }

    /**
     * Get the data that the HMAC of a row covered before {@link #dataToSign(Logger, Map, String...)}: the values
     * concatenated, which do not tell where a value ends, nor its column.
     * @param values the value of values
     * @param nameMapping the value of nameMapping
     */
    static byte[] concatenatedValues(Map<String, ?> values, String... nameMapping) {
        StringBuilder tmp = new StringBuilder();
        for (String h : nameMapping) {
            final Object value = values.get(h);
            if (value != null) {
                tmp.append(value.toString());
            }
        }
        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the keystore that holds the keys of a tamper evident CSV file.
     * @param csvFile The CSV file, current or archived.
     * @return The keystore file of the CSV file.
     */
    static File keyStoreFile(File csvFile) {
        return new File(csvFile.getPath() + ".keystore");
    }

    private CsvSecureUtils() {
        // Prevent from instantiating
    }
}
