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

package org.forgerock.json.jose.jwe;

/**
 * An Enum for the additional JWE Header parameter names.
 * <p>
 * As described in the JWE specification, the reserved JWE header parameters are listed,
 * <ul>
 *     <li>"enc"</li>
 *     <li>"epk"</li>
 *     <li>"zip"</li>
 *     <li>"apu"</li>
 * </ul>
 * This list add upon the list in {@link org.forgerock.json.jose.jws.JwsHeaderKey}.
 * Any other header parameter name is deemed as a "custom" header parameter.
 *
 * @since 2.0.0
 */
public enum JweHeaderKey {

    /**
     * Encryption Method header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-4.1.2">
     *     JWE Specification Encryption Method Header Parameter</a>
     */
    ENC,
    /**
     * Ephemeral Public Key header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-4.1.3">
     *     JWE Specification Ephermeral Public Key Header Parameter</a>
     */
    EPK,
    /**
     * Compression Algorithm header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-4.1.4">
     *     JWW Specification Compression Algorithm Header Parameter</a>
     */
    ZIP,
    /**
     * Agreement PartyUInfo header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-4.1.13">
     *     JWW Specification Agreement PartyUInfo Header Parameter</a>
     **/
    APU,
    /**
     * Generic header key for a custom header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.3">
     *     Private Header Parameter Names</a>
     */
    CUSTOM;

    /**
     * Returns a lowercase String of the JweHeaderKey constant.
     *
     * @return Lowercase String representation of the constant.
     * @see #toString()
     */
    public String value() {
        return toString();
    }

    /**
     * Gets the JweHeaderKey constant that matches the given String.
     * <p>
     * If the given String does not match any of the constants, then CUSTOM is returned.
     *
     * @param headerKey The String representation of a JweHeaderKey.
     * @return The matching JweHeaderKey.
     */
    public static JweHeaderKey getHeaderKey(String headerKey) {
        try {
            return JweHeaderKey.valueOf(headerKey);
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }

    /**
     * Turns the JweHeaderKey constant into a lowercase String.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
