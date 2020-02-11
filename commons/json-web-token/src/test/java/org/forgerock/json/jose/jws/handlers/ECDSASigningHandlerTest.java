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

package org.forgerock.json.jose.jws.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ECDSASigningHandlerTest {

    private static final EcJWK ES256JWK = new EcJWK(KeyUse.SIG, "ES256", "a",
            "APDGe8liKjUbraHWg73BuxrNTnITYrjML2MyHDAd5xsu",
            "ALtEuaDb0Sdb-w7nyW5BSaKRrtXptpSLsMaSnBcViUDc",
            "OVjBNuRhdpE2e5SRAeppuNxjPvsprI8phN2qxUI9bJ4", "P-256", null, null, null);

    private static final EcJWK ES384JWK = new EcJWK(KeyUse.SIG, "ES384", "b",
            "AJN7UXZj58Tm7IJaFeBYJLQak_78DD_kzxFgSKl-RjImX2RGwvhkeFwDXAXQMAZSbA",
            "ALqPlaFJP_9D9Wtnd8lomb64-GLF2TgAK5rQ6yTzgt78UVFziHjhKfvrBZ3A7F2e5A",
            "APVinLblGFYSytH5vtf6f6OH3kp_6AEHWsHkdjuBn766p4UN66YJ6809p2i7OhtOeA", "P-384", null, null, null);

    private static final EcJWK ES512JWK = new EcJWK(KeyUse.SIG, "ES512", "c",
            "AXGpIM94ezzlE2yhxAn0Bkg65AUI-Yd25wpQG-dCGUcB_My8TQC5p8goB-hYN4UzBL8_oJYXXygIsyPWf-TLUGWx",
            "AXzjO_aKti7iKbSy2DyiyUlmogUQZa30zh8peTvMxosRniHEp6-CwkVhof30dv6rSfzVHx-Gt9IO1k6iPOpG176D",
            "AaaZspgGTudhK1ohxCqfGac81L2bSnncnqYm4BN6ZjBJ504-rjLvuT352jND2U8c3CvJcraavOUZrvE5jJMZRaWW", "P-521",
            null, null, null);


    @Test(dataProvider = "supportedCurves")
    public void shouldEncodeSignatureCorrectly(JwsAlgorithm algorithm, EcJWK jwk, int expectedSize) throws Exception {
        // Given
        ECDSASigningHandler signingHandler = new ECDSASigningHandler(jwk.toECPrivateKey());
        final byte[] data = "Sample Message".getBytes(StandardCharsets.UTF_8);

        // When
        final byte[] signature = signingHandler.sign(algorithm, data);

        // Then
        assertThat(signature).hasSize(expectedSize);
    }

    @Test(dataProvider = "supportedCurves")
    public void shouldVerifyCorrectly(JwsAlgorithm algorithm, EcJWK jwk, int expectedSize) throws Exception {
        // Given
        ECDSASigningHandler signingHandler = new ECDSASigningHandler(jwk.toECPrivateKey());
        ECDSASigningHandler verificationHandler = new ECDSASigningHandler(jwk.toECPublicKey());
        final byte[] data = "Sample Message".getBytes(StandardCharsets.UTF_8);
        final byte[] signature = signingHandler.sign(algorithm, data);

        // When
        boolean valid = verificationHandler.verify(algorithm, data, signature);

        // Then
        assertThat(valid).isTrue();
    }

    @Test(dataProvider = "supportedCurves")
    public void shouldDetectTampering(JwsAlgorithm algorithm, EcJWK jwk, int expectedSize) throws Exception {
        // Given
        ECDSASigningHandler signingHandler = new ECDSASigningHandler(jwk.toECPrivateKey());
        ECDSASigningHandler verificationHandler = new ECDSASigningHandler(jwk.toECPublicKey());
        final byte[] data = "Sample Message".getBytes(StandardCharsets.UTF_8);
        final byte[] signature = signingHandler.sign(algorithm, data);

        data[0] = 0; // Make a change to the data

        // When
        boolean valid = verificationHandler.verify(algorithm, data, signature);

        // Then
        assertThat(valid).isFalse();
    }

    @DataProvider
    public static Object[][] supportedCurves() {
        return new Object[][] {
                { JwsAlgorithm.ES256, ES256JWK, 64 },
                { JwsAlgorithm.ES384, ES384JWK, 96 },
                { JwsAlgorithm.ES512, ES512JWK, 132 }
        };
    }
}