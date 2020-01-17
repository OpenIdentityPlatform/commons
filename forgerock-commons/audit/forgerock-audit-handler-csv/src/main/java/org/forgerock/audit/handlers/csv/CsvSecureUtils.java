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

package org.forgerock.audit.handlers.csv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;

/**
 * Holds the methods shared between the CsvSecure classes.
 */
final class CsvSecureUtils {

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
     * Get the data to sign.
     * @param logger the value of logger
     * @param values the value of values
     * @param nameMapping the value of nameMapping
     */
    static byte[] dataToSign(Logger logger, Map<String, ?> values, String... nameMapping) {
        StringBuilder tmp = new StringBuilder();
        for (String h : nameMapping) {
            final Object value = values.get(h);
            if (value != null) {
                tmp.append(value.toString());
            }
        }
        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }

    private CsvSecureUtils() {
        // Prevent from instantiating
    }
}
