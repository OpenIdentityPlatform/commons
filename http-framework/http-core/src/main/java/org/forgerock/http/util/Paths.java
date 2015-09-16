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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.util;

import static java.util.Collections.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilities for manipulating paths.
 */
public final class Paths {
    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");

    /**
     * Non-safe characters are escaped as UTF-8 octets using "%" HEXDIG HEXDIG
     * production.
     */
    private static final char URL_ESCAPE_CHAR = '%';

    /**
     * Look up table for characters which do not need URL encoding.
     */
    private static final BitSet SAFE_URL_CHARS = new BitSet(128);

    static {
        /*
         * These characters do not need encoding.
         */
        for (char c : "-._~!$&'()*+,;=:@".toCharArray()) {
            SAFE_URL_CHARS.set(c);
        }

        /*
         * ASCII alphanumeric characters are ok as well.
         */
        SAFE_URL_CHARS.set('0', '9' + 1);
        SAFE_URL_CHARS.set('a', 'z' + 1);
        SAFE_URL_CHARS.set('A', 'Z' + 1);
    }

    /**
     * Returns the URL path decoding of the provided object's string
     * representation.
     *
     * @param value
     *            The value to be URL path decoded.
     * @return The URL path decoding of the provided object's string
     *         representation.
     */
    public static String urlDecode(final Object value) {
        // First try fast-path decode of simple ASCII.
        final String s = value.toString();
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            if (isUrlEscapeChar(s.charAt(i))) {
                // Slow path.
                return urlDecode0(s);
            }
        }
        return s;
    }

    private static String urlDecode0(String s) {
        final StringBuilder builder = new StringBuilder(s.length());
        final int size = s.length();
        final byte[] buffer = new byte[size / 3];
        for (int i = 0; i < size;) {
            final char c = s.charAt(i);
            if (!isUrlEscapeChar(c)) {
                builder.append(c);
                i++;
            } else {
                int bufferPos = 0;
                for (; i < size && isUrlEscapeChar(s.charAt(i)); i += 3) {
                    if ((i + 2) >= size) {
                        throw new IllegalArgumentException(
                                "Path contains an incomplete percent encoding");
                    }
                    final String hexPair = s.substring(i + 1, i + 3);
                    try {
                        final int octet = Integer.parseInt(hexPair, 16);
                        if (octet < 0) {
                            throw new IllegalArgumentException(
                                    "Path contains an invalid percent encoding '" + hexPair + "'");
                        }
                        buffer[bufferPos++] = (byte) octet;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Path contains an invalid percent encoding '" + hexPair + "'");
                    }
                }
                builder.append(new String(buffer, 0, bufferPos, StandardCharsets.UTF_8));
            }
        }
        return builder.toString();
    }

    /**
     * Returns the URL path encoding of the provided object's string
     * representation.
     *
     * @param value
     *            The value to be URL path encoded.
     * @return The URL path encoding of the provided object's string
     *         representation.
     */
    public static String urlEncode(final Object value) {
        // First try fast-path encode of simple ASCII.
        final String s = value.toString();
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            final int c = s.charAt(i);
            if (!SAFE_URL_CHARS.get(c)) {
                // Slow path.
                return urlEncode0(s);
            }
        }
        return s;
    }

    /**
     * Fast lookup for encoding octets as hex.
     */
    private static final String[] BYTE_TO_HEX = new String[256];
    static {
        for (int i = 0; i < BYTE_TO_HEX.length; i++) {
            BYTE_TO_HEX[i] = String.format(Locale.ROOT, "%02X", i);
        }
    }

    private static String urlEncode0(String s) {
        final byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        final int size = utf8.length;
        final StringBuilder builder = new StringBuilder(size + 16);
        for (int i = 0; i < size; i++) {
            final int octet = utf8[i] & 0xff;
            if (SAFE_URL_CHARS.get(octet)) {
                builder.append((char) octet);
            } else {
                builder.append(URL_ESCAPE_CHAR);
                builder.append(BYTE_TO_HEX[octet]);
            }
        }
        return builder.toString();
    }

    private static boolean isUrlEscapeChar(final char c) {
        return c == URL_ESCAPE_CHAR;
    }

    /**
     * Converts a path into a list of URL-decoded path elements. If the leading path element
     * is empty it is dropped, meaning that {@code null}, {@code ""} and {@code "/"} will
     * all return an empty list, and {@code "//"} will return a list with two elements, both
     * empty strings, as all intermediate and trailing empty paths are retained.
     *
     * @param rawPath The raw, URL-encoded path string.
     * @return An immutable list of the path elements.
     */
    public static List<String> getPathElements(String rawPath) {
        String[] pathElements = null;
        if (rawPath != null) {
            if (rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            pathElements = PATH_SPLIT_PATTERN.split(rawPath, -1);
            if (pathElements.length == 1 && pathElements[0].isEmpty()) {
                pathElements = null;
            }
        }

        List<String> elements;
        if (pathElements == null) {
            elements = emptyList();
        } else {
            List<String> decodedElements = new ArrayList<>(pathElements.length);
            for (String element : pathElements) {
                decodedElements.add(Paths.urlDecode(element));
            }
            elements = decodedElements;
        }
        return unmodifiableList(elements);
    }

    /**
     * Joins a list of URL-decoded path elements into a url-encoded path.
     * @param elements The list of (URL-decoded) elements.
     * @return The raw path.
     */
    public static String joinPath(List<String> elements) {
        if (elements == null) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        for (String element : elements) {
            if (s.length() > 0) {
                s.append("/");
            }
            s.append(urlEncode(element));
        }
        return s.toString();
    }

    private Paths() {
        // utilities only.
    }
}
