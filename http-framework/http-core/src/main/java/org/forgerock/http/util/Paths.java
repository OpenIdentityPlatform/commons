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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilities for manipulating paths.
 */
public final class Paths {
    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");

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
        return Uris.urlPathDecode(value.toString());
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
        return Uris.urlPathEncode(value.toString());
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
