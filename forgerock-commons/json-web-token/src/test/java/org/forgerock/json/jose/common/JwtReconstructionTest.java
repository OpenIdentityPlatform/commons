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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.helper.KeysHelper;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.SignedThenEncryptedJwt;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JwtReconstructionTest {

    private static final String HEADER_KEY = "header-key";
    private static final String HEADER_VALUE = "header-value";
    private static final String CLAIM_KEY = "claim-key";
    private static final String CLAIM_VALUE = "claim-value";

    private JwtBuilderFactory jwtBuilderFactory;

    @BeforeClass
    public void setUp() {
        jwtBuilderFactory = new JwtBuilderFactory();
    }

    @Test
    public void canReconstructPlaintextJwt() {
        // Given
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jwt()
                .headers()
                    .header(HEADER_KEY, HEADER_VALUE)
                .done()
                .claims(jwtClaimsSet)
                .build();

        // When
        Jwt jwt = jwtBuilderFactory.reconstruct(jwtString, Jwt.class);

        // Then
        assertThat(jwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(jwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);
    }

    @Test
    public void canReconstructSignedPlaintextJwt() {
        // Given
        SigningHandler signingHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPrivateKey());
        SigningHandler verificationHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPublicKey());
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jws(signingHandler)
                .headers()
                    .alg(JwsAlgorithm.RS256)
                    .header(HEADER_KEY, HEADER_VALUE)
                .done()
                .claims(jwtClaimsSet)
                .build();

        // When
        SignedJwt signedJwt = jwtBuilderFactory.reconstruct(jwtString, SignedJwt.class);
        boolean signatureVerified = signedJwt.verify(verificationHandler);

        // Then
        assertThat(signatureVerified).isTrue();
        assertThat(signedJwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(signedJwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);
    }

    @Test
    public void canReconstructEncryptedJwt() {
        // Given
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jwe(KeysHelper.getRSAPublicKey())
                .headers()
                    .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                    .enc(EncryptionMethod.A128CBC_HS256)
                    .header(HEADER_KEY, HEADER_VALUE)
                .done()
                .claims(jwtClaimsSet)
                .build();

        // When
        EncryptedJwt encryptedJwt = jwtBuilderFactory.reconstruct(jwtString, EncryptedJwt.class);
        encryptedJwt.decrypt(KeysHelper.getRSAPrivateKey());

        // Then
        assertThat(encryptedJwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(encryptedJwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);
    }

    @Test
    public void canReconstructSignedEncryptedJwt() {
        // Given
        SigningHandler signingHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPrivateKey());
        SigningHandler verificationHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPublicKey());
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jwe(KeysHelper.getRSAPublicKey())
                .headers()
                    .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                    .enc(EncryptionMethod.A128CBC_HS256)
                    .header(HEADER_KEY, HEADER_VALUE)
                .done()
                .claims(jwtClaimsSet)
                .signedWith(signingHandler, JwsAlgorithm.RS256)
                .build();

        // When
        EncryptedThenSignedJwt signedEncryptedJwt = jwtBuilderFactory.reconstruct(jwtString,
                EncryptedThenSignedJwt.class);
        signedEncryptedJwt.decrypt(KeysHelper.getRSAPrivateKey());
        boolean signatureVerified = signedEncryptedJwt.verify(verificationHandler);

        // Then
        assertThat(signatureVerified).isTrue();
        assertThat(signedEncryptedJwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(signedEncryptedJwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);
    }

    @Test
    public void canReconstructOldStyleSignedEncryptedJwt() {
        SigningHandler signingHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPrivateKey());
        SigningHandler verificationHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPublicKey());
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jwe(KeysHelper.getRSAPublicKey())
                .headers()
                    .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                    .enc(EncryptionMethod.A128CBC_HS256)
                    .header(HEADER_KEY, HEADER_VALUE)
                .done()
                .claims(jwtClaimsSet)
                .sign(signingHandler, JwsAlgorithm.RS256)
                .build();

        // When
        SignedEncryptedJwt signedEncryptedJwt = jwtBuilderFactory.reconstruct(jwtString,
                SignedEncryptedJwt.class);
        signedEncryptedJwt.decrypt(KeysHelper.getRSAPrivateKey());
        boolean signatureVerified = signedEncryptedJwt.verify(verificationHandler);

        // Then
        assertThat(signatureVerified).isTrue();
        assertThat(signedEncryptedJwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(signedEncryptedJwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);

    }

    @Test
    public void canReconstructEncryptedSignedJwt() {
        // Given
        SigningHandler signingHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPrivateKey());
        SigningHandler verificationHandler = new SigningManager().newRsaSigningHandler(KeysHelper.getRSAPublicKey());
        JwtClaimsSet jwtClaimsSet = jwtBuilderFactory.claims().claim(CLAIM_KEY, CLAIM_VALUE).build();

        String jwtString = jwtBuilderFactory.jws(signingHandler)
                                            .headers()
                                                .alg(JwsAlgorithm.RS256)
                                            .done()
                                            .encrypt(KeysHelper.getRSAPublicKey())
                                            .headers()
                                                .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                                                .enc(EncryptionMethod.A128CBC_HS256)
                                                .header(HEADER_KEY, HEADER_VALUE)
                                            .done()
                                            .claims(jwtClaimsSet)
                                            .build();

        // When
        SignedThenEncryptedJwt encryptedSignedJwt = jwtBuilderFactory.reconstruct(jwtString,
                SignedThenEncryptedJwt.class);
        encryptedSignedJwt.decrypt(KeysHelper.getRSAPrivateKey());
        boolean signatureVerified = encryptedSignedJwt.verify(verificationHandler);

        // Then
        assertThat(signatureVerified).isTrue();
        assertThat(encryptedSignedJwt.getHeader().getParameter(HEADER_KEY)).isEqualTo(HEADER_VALUE);
        assertThat(encryptedSignedJwt.getClaimsSet().getClaim(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);

    }
}
