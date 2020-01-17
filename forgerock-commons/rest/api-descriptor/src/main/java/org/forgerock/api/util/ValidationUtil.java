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

package org.forgerock.api.util;

import java.util.regex.Pattern;

/**
 * API Descriptor model-validation utilities.
 */
public final class ValidationUtil {

    private static final Pattern FIND_WHITESPACE_PATTERN = Pattern.compile("\\s");

    private ValidationUtil() {
        // empty
    }

    /**
     * Checks a {@code String} for whitespace.
     *
     * @param s {@code String} to validate
     * @return {@code true} if contains whitespace and {@code false} otherwise
     */
    public static boolean containsWhitespace(final String s) {
        return FIND_WHITESPACE_PATTERN.matcher(s).find();
    }

    /**
     * Determines if a {@code String} is {@code null}, or empty, or only contains whitespace.
     *
     * @param s {@code String} to validate
     * @return {@code true} if {@code null} or empty, or only contains whitespace, and {@code false} otherwise
     */
    public static boolean isEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Determines if an array is {@code null} or zero-length.
     *
     * @param a Array to validate
     * @return {@code true} if {@code null} or empty, and {@code false} otherwise
     */
    public static boolean isEmpty(final Object[] a) {
        return a == null || a.length == 0;
    }

    /**
     * Checks that there is only a single single non-{@code null} argument.
     *
     * @param args Arguments
     * @return {@code true} if there is a single non-{@code null} argument, and {@code false} otherwise
     */
    public static boolean isSingleNonNull(final Object... args) {
        boolean found = false;
        if (args != null) {
            for (final Object o : args) {
                if (o != null) {
                    if (found) {
                        // there is more than one non-null argument
                        return false;
                    }
                    found = true;
                }
            }
        }
        return found;
    }

    /**
     * Returns false if the given Boolean parameter is {@code null}.
     *
     * @param boolVal boolean parameter to check
     * @return {@code false} if the parameter is null, and the parameter itself otherwise
     */
    public static boolean nullToFalse(Boolean boolVal) {
        return (boolVal == null) ? false : boolVal;
    }
}
