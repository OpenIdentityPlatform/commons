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

package org.forgerock.json.jose.jwe;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JwsVerifyingException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SignedThenEncryptedJwtTest {
    private static final JwtReconstruction JWT_RECONSTRUCTION = new JwtReconstruction();

    private KeyPair rsaKeyPair;
    private JwtBuilderFactory jwtBuilderFactory;
    private SigningHandler signingHandler;

    @BeforeClass
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        rsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    @BeforeMethod
    public void createJwtBuilderFactory() {
        jwtBuilderFactory = new JwtBuilderFactory();
        signingHandler = new HmacSigningHandler(new byte[32]);
    }

    @Test
    public void shouldDecryptAndVerifyCorrectly() {
        // Given
        JwtClaimsSet claims = new JwtClaimsSet();
        claims.setExpirationTime(new Date());
        claims.setIssuedAtTime(new Date());
        claims.setIssuer("test");
        claims.addAudience("a");
        claims.addAudience("b");
        claims.addAudience("c");
        String jwtStr = jwtBuilderFactory.jws(signingHandler).headers().alg(JwsAlgorithm.HS256).done()
                                         .claims(claims)
                                         .encrypt(rsaKeyPair.getPublic())
                                         .headers()
                                            .enc(EncryptionMethod.A128CBC_HS256).alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                                         .done()
                                         .build();

        // When
        SignedThenEncryptedJwt jwt = JWT_RECONSTRUCTION.reconstructJwt(jwtStr, SignedThenEncryptedJwt.class);

        // Then
        assertThat(jwt.decryptAndVerify(rsaKeyPair.getPrivate(), signingHandler)).isTrue();
        assertThat(jwt.getClaimsSet().build()).isEqualTo(claims.build());
    }

    @Test(expectedExceptions = JwsVerifyingException.class)
    public void shouldNotVerifyIfNotDecrypted() {
        // Given
        JwtClaimsSet claims = new JwtClaimsSet();
        String jwtStr = jwtBuilderFactory.jws(signingHandler).headers().alg(JwsAlgorithm.HS256).done()
                                         .claims(claims)
                                         .encrypt(rsaKeyPair.getPublic())
                                         .headers()
                                         .enc(EncryptionMethod.A128CBC_HS256).alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                                         .done()
                                         .build();

        // When
        SignedThenEncryptedJwt jwt = JWT_RECONSTRUCTION.reconstructJwt(jwtStr, SignedThenEncryptedJwt.class);
        jwt.verify(signingHandler);

        // Then - exception
    }
}