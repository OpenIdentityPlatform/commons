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

package org.forgerock.json.jose.jws;

/**
 * An Enum for the JWS Header parameter names.
 * <p>
 * As described in the JWS specification, the reserved JWS header parameters are listed,
 * <ul>
 *     <li>"jku"</li>
 *     <li>"jwk"</li>
 *     <li>"x5u"</li>
 *     <li>"x5t"</li>
 *     <li>"x5c"</li>
 *     <li>"kid"</li>
 *     <li>"cty"</li>
 *     <li>"crit"</li>
 * </ul>
 * Any other header parameter name is deemed as a "custom" header parameter.
 *
 * @since 2.0.0
 */
public enum JwsHeaderKey {

    /**
     * JWK Set URL header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.2">
     *     JWS Specification JWK Set URL Header Parameter</a>
     */
    JKU,
    /**
     * JSON Web Key header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.3">
     *     JWS Specification JWK Web Key Header Parameter</a>
     */
    JWK,
    /**
     * X.509 URL header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.4">
     *     JWS Specification X.509 URL Header Parameter</a>
     */
    X5U,
    /**
     * X.509 Certificate Thumbprint header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.5">
     *     JWS Specification X.509 Certificate Thumbprint Header Parameter</a>
     **/
    X5T,  //Base64url
    /**
     * X.509 Certificate Chain header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.6">
     *     JWS Specification X.509 Certificate Chain Header Parameter</a>
     */
    X5C,   //List<Base64>
    /**
     * Key ID header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.7">
     *     JWS Specification Key ID Header Parameter</a>
     */
    KID,
    /**
     * Content Type header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.8">
     *     JWS Specification Content Type Header Parameter</a>
     */
    CTY,
    /**
     * Critical header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1.10">
     *     JWS Specification Critical Header Parameter</a>
     */
    CRIT,
    /**
     * Generic header key for a custom header parameter.
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.3">
     *     Private Header Parameter Names</a>
     */
    CUSTOM;

    /**
     * Returns a lowercase String of the JwsHeaderKey constant.
     *
     * @return Lowercase String representation of the constant.
     * @see #toString()
     */
    public String value() {
        return toString();
    }

    /**
     * Gets the JwsHeaderKey constant that matches the given String.
     * <p>
     * If the given String does not match any of the constants, then CUSTOM is returned.
     *
     * @param headerKey The String representation of a JwsHeaderKey.
     * @return The matching JwsHeaderKey.
     */
    public static JwsHeaderKey getHeaderKey(String headerKey) {
        try {
            return JwsHeaderKey.valueOf(headerKey);
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }

    /**
     * Turns the JwsHeaderKey constant into a lowercase String.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
