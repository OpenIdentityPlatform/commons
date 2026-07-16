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

package org.forgerock.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Produces a short, one-way hash of a sensitive value so it can be correlated across log lines
 * without exposing the value itself (CWE-532: insertion of sensitive information into log file).
 * <p>
 * The result is intended solely for log correlation, not as a cryptographic commitment: only the
 * first 8 bytes of the SHA-256 digest are returned (16 hex characters). The input is never logged.
 */
public final class SecretHash {

    private SecretHash() {
        // Utility class.
    }

    /**
     * Returns a short, one-way SHA-256 hash (first 8 bytes, 16 hex characters) of the given
     * sensitive value, safe to write to logs for correlation purposes.
     *
     * @param value the sensitive value to hash; may be {@code null}
     * @return {@code "null"} if {@code value} is {@code null}, {@code "unavailable"} if SHA-256 is
     *         not available, otherwise the 16-character lower-case hex hash
     */
    public static String hash(String value) {
        if (value == null) {
            return "null";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8 && i < digest.length; i++) {
                hex.append(Character.forDigit((digest[i] >> 4) & 0xF, 16));
                hex.append(Character.forDigit(digest[i] & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }
}
