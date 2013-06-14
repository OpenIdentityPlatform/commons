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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.util.encode;

/**
 * Makes use of the very fast and memory efficient Base64 class to encode and decode to and from BASE64 in full
 * accordance with RFC 2045. And then replaces + and / for - and _ respectively and removes the padding character =
 * to be in accordance with RFC 4648.
 */
public class Base64url {

    /**
     * Encodes the given byte array into a Base64url encoded String.
     *
     * @param content The byte array to encode.
     * @return The Base64url encoded byte array.
     */
    public static String encode(byte[] content) {
        String base64EncodedString = Base64.encode(content);

        return base64EncodedString.replaceAll("\\+", "-")
                .replaceAll("/", "_")
                .replaceAll("=", "");
    }

    /**
     * Decodes the given Base64url encoded String into a byte array.
     *
     * @param content The Base64url encoded String to decode.
     * @return The decoded byte[] array.
     */
    public static byte[] decode(String content) {

        content = content.replaceAll("-", "+")
                .replaceAll("_", "/");

        int modulus = content.length() % 4;
        int numberOfPaddingChars = 4 - modulus;
        if (modulus != 0) {
            for (int i = 0; i < numberOfPaddingChars; i++) {
                content += "=";
            }
        }

        return Base64.decode(content);
    }
}
