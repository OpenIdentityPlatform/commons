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

package com.forgerock.api.beans;

import java.util.regex.Pattern;

/**
 * API Descriptor model-validation utilities.
 */
final class ValidationUtil {

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
}
