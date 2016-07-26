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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An Enum for the JWT Header parameter names.
 * <p>
 * As described in the JWT specification, the reserved JWT header parameters are listed,
 * <ul>
 *     <li>"typ"</li>
 *     <li>"alg"</li>
 * </ul>
 * Any other header parameter name is deemed as a "custom" header parameter.
 *
 * @since 2.0.0
 */
public enum JwtHeaderKey {

    /** Type JWT header parameter.. */
    TYP,
    /** Algorithm JWT header parameter. */
    ALG,
    /** Generic header key for a custom header parameter. */
    CUSTOM;

    /**
     * Read-only {@code Map} of {@code JwtHeaderKey} values as lower-case {@code String}s, for fast lookup.
     */
    private static final Map<String, JwtHeaderKey> NAME_MAP;

    static {
        final Map<String, JwtHeaderKey> temp = new HashMap<>();
        for (final JwtHeaderKey key : values()) {
            temp.put(key.lowerCaseName, key);
        }
        NAME_MAP = Collections.unmodifiableMap(temp);
    }

    private final String lowerCaseName;

    /**
     * Creates a {@code JwtHeaderKey} with pre-allocated lower-case {@code String} representation, as a
     * performance optimization, because this {@code enum} is often converted to a {@code String}.
     */
    JwtHeaderKey() {
        this.lowerCaseName = name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a lowercase String of the JwtHeaderKey constant.
     *
     * @return Lowercase String representation of the constant.
     * @see #toString()
     */
    public String value() {
        return toString();
    }

    /**
     * Gets the JwtHeaderKey constant that matches the given String.
     * <p>
     * If the given String does not match any of the constants, then CUSTOM is returned.
     *
     * @param headerKey The String representation of a JwtHeaderKey.
     * @return The matching JwtHeaderKey.
     */
    public static JwtHeaderKey getHeaderKey(final String headerKey) {
        if (headerKey != null && !headerKey.isEmpty()) {
            final JwtHeaderKey value = NAME_MAP.get(headerKey.toLowerCase(Locale.ROOT));
            if (value != null) {
                return value;
            }
        }
        return CUSTOM;
    }

    /**
     * Turns the JwtHeaderKey constant into a lowercase String.
     *
     * @return Lowercase String representation of the constant.
     */
    @Override
    public String toString() {
        return lowerCaseName;
    }
}
