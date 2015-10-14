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
 * The interface represents the body of a JWT.
 * <p>
 * When the JWT is digitally signed or MACed, the bytes of the UTF-8 representation of the Payload are base64url
 * encoded to create the Encoded JWS Payload. When the JWT is encrypted, the bytes of the UTF-8 representation of the
 * Payload are used as the JWE Plaintext.
 *
 * @since 2.0.0
 */
public interface Payload {

    /**
     * Builds the JWTs Payload into a <code>String</code> by following the steps specified in the relevant specification
     * according to whether the JWT is being signed and/or encrypted.
     * <p>
     * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10#section-7">
     *     JSON Web Token Specification</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-5">
     *     JSON Web Signature Specification</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5">
     *     JSON Web Encryption Specification</a>
     *
     * @return The base64url encoded UTF-8 representation of the JWTs Payload.
     */
    String build();
}
