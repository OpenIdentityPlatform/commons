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
 * An Enum for the JWT Claims Set names.
 * <p>
 * As described in the JWT specification, this Enum class represents all the reserved JWT Claim Names, any other Claim
 * name is deemed as a "custom" Claim name.
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10#section-4.1">Reserved Claim Names</a>
 *
 * @since 2.0.0
 */
public enum JwtClaimsSetKey {

    /**
     * Type Claim.
     * <p>
     * Used to declare a type for the contents of this JWT Claims Set.
     * <p>
     * The values used for the "typ" claim SHOULD come from the same value space as the "typ" header parameter, with
     * the same rules applying.
     */
    TYP,
    /**
     * JWT ID Claim.
     * <p>
     * Provides a unique identifier for the JWT.
     */
    JTI,
    /**
     * Issuer Claim.
     * <p>
     * Identifies the principal that issued the JWT.
     */
    ISS,
    /**
     * Subject Claim.
     * <p>
     * Identifies the subject of the JWT.
     */
    SUB,
    /**
     * Audience Claim.
     * <p>
     * Identifies the audience that the JWT is intended for.
     */
    AUD,
    /**
     * Issued At Claim.
     * <p>
     * Identifies the time at which the JWT was issued. This claim can be used to determine the age of the token.
     */
    IAT,
    /**
     * Not Before Claim.
     * <p>
     * Identifies the time before which the token MUST NOT be accepted for processing.
     */
    NBF,
    /**
     * Expiration Time Claim.
     * <p>
     * Identifies the expiration time on or after which the token MUST NOT be accepted for processing.
     */
    EXP,
    /**
     * Custom (private) Claim.
     * <p>
     * Represents any claim not registered in the JWT spec.
     */
    CUSTOM;

    /**
     * Read-only {@code Map} of {@code JwtClaimsSetKey} values as lower-case {@code String}s, for fast lookup.
     */
    private static final Map<String, JwtClaimsSetKey> NAME_MAP;

    static {
        final Map<String, JwtClaimsSetKey> temp = new HashMap<>();
        for (final JwtClaimsSetKey key : values()) {
            temp.put(key.lowerCaseName, key);
        }
        NAME_MAP = Collections.unmodifiableMap(temp);
    }

    private final String lowerCaseName;

    /**
     * Creates a {@code JwtClaimsSetKey} with pre-allocated lower-case {@code String} representation, as a
     * performance optimization, because this {@code enum} is often converted to a {@code String}.
     */
    JwtClaimsSetKey() {
        this.lowerCaseName = name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a lowercase String of the {@code JwtClaimsSetKey} constant.
     *
     * @return Lowercase String representation of the constant.
     * @see #toString()
     */
    public String value() {
        return toString();
    }

    /**
     * Gets the {@code JwtClaimsSetKey} constant that matches the given {@code String} (case-insensitive).
     *
     * @param claimSetKey The case-insensitive {@code String} representation of a {@code JwtClaimsSetKey}.
     * @return The matching {@code JwtClaimsSetKey} or {@link #CUSTOM} for keys not in the JWT spec.
     */
    public static JwtClaimsSetKey getClaimSetKey(final String claimSetKey) {
        if (claimSetKey != null && !claimSetKey.isEmpty()) {
            final JwtClaimsSetKey value = NAME_MAP.get(claimSetKey.toLowerCase(Locale.ROOT));
            if (value != null) {
                return value;
            }
        }
        return CUSTOM;
    }

    /**
     * Turns the {@code JwtClaimsSetKey} constant into a lowercase {@code String}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return lowerCaseName;
    }
}
