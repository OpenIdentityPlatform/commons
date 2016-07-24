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

package org.forgerock.json.jose.jws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SignedJwtTest {
    private static final Logger logger = LoggerFactory.getLogger(SignedJwtTest.class);
    private static final JwsAlgorithm JWS_ALGORITHM = JwsAlgorithm.HS256;

    private JwtBuilderFactory jwtBuilderFactory;
    private JwtReconstruction jwtReconstruction;
    private SigningHandler signingHandler;

    @BeforeMethod
    public void init() {
        jwtBuilderFactory = new JwtBuilderFactory();
        jwtReconstruction = new JwtReconstruction();
        signingHandler = new HmacSigningHandler(new byte[32]);
    }

    @Test
    public void shouldVerifyCorrectlyWithCompression() {
        // Given
        JwtClaimsSet claims = new JwtClaimsSet();
        claims.setClaim("sub", "demo");
        claims.setIssuedAtTime(new Date());

        // When
        String jwt = signedCompressedJwt(CompressionAlgorithm.DEF, claims);
        SignedJwt signedJwt = jwtReconstruction.reconstructJwt(jwt, SignedJwt.class);

        // Then
        assertThat(signedJwt.verify(signingHandler)).isTrue();
    }

    @Test
    public void shouldCompressWhenAsked() {
        // Given
        JwtClaimsSet claims = new JwtClaimsSet();
        // Repetitive claim values should compress well
        for (int i = 0; i < 20; ++i) {
            claims.setClaim(Integer.toString(i), "aaaaaaaaaaaaaaaaaaaa");
        }

        // When
        String compressedJwt = signedCompressedJwt(CompressionAlgorithm.DEF, claims);
        String uncompressedJwt = signedCompressedJwt(CompressionAlgorithm.NONE, claims);

        // Then
        logger.info("Uncompressed size = {} bytes, Compressed size = {} bytes", uncompressedJwt.length(),
                compressedJwt.length());
        assertThat(compressedJwt.length()).isLessThan(uncompressedJwt.length());
    }

    private String signedCompressedJwt(CompressionAlgorithm compressionAlgorithm, JwtClaimsSet claims) {
        return jwtBuilderFactory.jws(signingHandler).headers().alg(JWS_ALGORITHM).zip(compressionAlgorithm).done()
                .claims(claims).build();
    }
}