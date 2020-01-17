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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

/**
 * An Enum for the possible types of JWTs.
 * <p>
 * There are only three possible types of JWTs, plaintext, signed or encrypted. For non-nested JWTs then the "JWT" type
 * is RECOMMENDED to be used but it is OPTIONAL to set the "typ" property in the JWT header. For nested signed or
 * encrypted JWTs the JWT type MUST be "JWS" and "JWE" respectively and the "typ" property in the JWT header MUST be
 * set.
 *
 * @since 2.0.0
 */
public enum JwtType {

    /** Used for plaintext, non-nested signed or non-nested encrypted JWTs. */
    JWT,
    /** Used when signing a nested JWT. */
    JWS,
    /** Used when encrypting a nested JWT. */
    JWE;

    /**
     * Finds the value of the String representation of the given JWT type.
     *
     * @param jwtType The JWT type.
     * @return The JWT type enum.
     */
    public static JwtType jwtType(String jwtType) {
        return valueOf(jwtType.toUpperCase());
    }
}
