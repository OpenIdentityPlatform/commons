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

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtType;

/**
 * An implementation of a JwtBuilder that can build a JWT and encrypt it and nest it within another signed JWT,
 * resulting in an SignedEncryptedJwt object.
 *
 * @since 2.0.0
 */
public class EncryptedThenSignedJwtBuilder extends AbstractJwtBuilder implements SignedJwtBuilder {

    private final EncryptedJwtBuilder encryptedJwtBuilder;
    private final SigningHandler signingHandler;
    private final JwsAlgorithm jwsAlgorithm;
    private final EncryptedThenSignedJwtHeaderBuilder headerBuilder;

    /**
     * Constructs a new SignedEncryptedJwtBuilder that will use the given EncryptedJwtBuilder, to build the nested
     * Encrypted JWT, and the private key and JwsAlgorithm to sign the outer JWT.
     *
     * @param encryptedJwtBuilder The EncryptedJwtBuilder instance.
     * @param signingHandler The SigningHandler instance used to sign the JWS.
     * @param jwsAlgorithm The JwsAlgorithm to use when signing the JWT.
     */
    public EncryptedThenSignedJwtBuilder(EncryptedJwtBuilder encryptedJwtBuilder, SigningHandler signingHandler,
            JwsAlgorithm jwsAlgorithm) {
        this.encryptedJwtBuilder = encryptedJwtBuilder;
        this.signingHandler = signingHandler;
        this.jwsAlgorithm = jwsAlgorithm;
        this.headerBuilder = new EncryptedThenSignedJwtHeaderBuilder(this);
    }

    @Override
    public SignedJwt asJwt() {
        JwsHeader header = headerBuilder.alg(jwsAlgorithm).cty(JwtType.JWT.toString()).build();
        EncryptedJwt encryptedJwt = encryptedJwtBuilder.asJwt();

        return new EncryptedThenSignedJwt(header, encryptedJwt, signingHandler);
    }

    /**
     * Builds the JWS into a <code>String</code> by calling the <tt>build</tt> method on the JWS object.
     * <p>
     * @see EncryptedThenSignedJwt#build()
     *
     * @return The base64url encoded UTF-8 parts of the JWS.
     */
    @Override
    public String build() {
        return asJwt().build();
    }

    @Override
    public EncryptedThenSignedJwtHeaderBuilder headers() {
        return headerBuilder;
    }
}
