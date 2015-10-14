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

package org.forgerock.json.jose.builders;

import java.security.Key;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;

/**
 * A factory for getting builders for plaintext, signed and encrypted JWTs and reconstructing JWT strings back into
 * their relevant JWT objects.
 *
 * @since 2.0.0
 */
public class JwtBuilderFactory {

    /**
     * Creates a builder for building a plaintext JWT into base64url UTF-8 encoded JWT string.
     *
     * @return The JwtBuilder instance that will build the plaintext JWT.
     */
    public SignedJwtBuilderImpl jwt() {
        return new SignedJwtBuilderImpl(new NOPSigningHandler());
    }

    /**
     * Creates a builder for building a signed JWT into a base64url UTF-8 encoded JWT string.
     *
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     * @return The JwtBuilder instance that will build the signed JWT.
     */
    public SignedJwtBuilderImpl jws(SigningHandler signingHandler) {
        return new SignedJwtBuilderImpl(signingHandler);
    }

    /**
     * Creates a builder for building an encrypted JWT into a base64url UTF-8 encoded JWT string.
     *
     * @param publicKey The public key that will be used to encrypted the JWT.
     * @return The JwtBuilder instance that will build the encrypted JWT.
     */
    public EncryptedJwtBuilder jwe(Key publicKey) {
        return new EncryptedJwtBuilder(publicKey);
    }

    /**
     * Creates a builder for building a JWT Claims Set to be used in the building of JWTs.
     *
     * @return The JwtClaimsSetBuilder instance that will build the claims set.
     */
    public JwtClaimsSetBuilder claims() {
        return new JwtClaimsSetBuilder();
    }

    /**
     * Reconstructs the given JWT string into a JWT object of the specified type.
     *
     * @param jwtString The JWT string.
     * @param jwtClass The JWT class to reconstruct the JWT string to.
     * @param <T> The type of JWT the JWT string represents.
     * @return The reconstructed JWT object.
     */
    public <T extends Jwt> T reconstruct(String jwtString, Class<T> jwtClass) {
        return new JwtReconstruction().reconstructJwt(jwtString, jwtClass);
    }
}
