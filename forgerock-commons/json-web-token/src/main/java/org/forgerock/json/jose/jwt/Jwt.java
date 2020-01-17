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
 * The interface for all types of JSON Web Tokens (JWTs).
 * <p>
 * JSON Web Token (JWT) is a means of representing claims to be transferred between two parties.  The claims in a JWT
 * are encoded as a JSON object that is digitally signed or MACed using JSON Web Signature (JWS) and/or encrypted using
 * JSON Web Encryption (JWE).
 * <p>
 * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10">JSON Web Token Specification</a>
 *
 * @since 2.0.0
 */
public interface Jwt {

    /**
     * Gets the header object for the JWT, which contains properties which describe the cryptographic operations
     * applied to the JWT, among other properties.
     * <p>
     * When the JWT is digitally signed or MACed, the JWT Header is a JWS Header.  When the JWT is encrypted, the JWT
     * Header is a JWE Header.
     *
     * @return The JWTs Header.
     */
    JwtHeader getHeader();

    /**
     * Gets the claims set object for the Jwt, which contains all of the claims (name value pairs) conveyed by the JWT.
     *
     * @return The JWTs Claims Set.
     */
    JwtClaimsSet getClaimsSet();

    /**
     * Builds the JWT into a <code>String</code> by following the steps specified in the relevant specification
     * according to whether the JWT is being signed and/or encrypted.
     * <p>
     * @see <a href="http://tools.ietf.org/html/draft-jones-json-web-token-10#section-7">
     *     JSON Web Token Specification</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-5">
     *     JSON Web Signature Specification</a>
     * @see <a href="http://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-11#section-5">
     *     JSON Web Encryption Specification</a>
     *
     * @return The base64url encoded UTF-8 parts of the JWT.
     */
    String build();
}
