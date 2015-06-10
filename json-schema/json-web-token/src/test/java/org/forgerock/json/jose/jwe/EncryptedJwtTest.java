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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashSet;
import java.util.Set;

public class EncryptedJwtTest {
    private KeyPair keyPair;

    @BeforeClass
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @Test(dataProvider = "encryptionMethods")
    public void shouldNotLeakInformationAboutDecryptionFailures(EncryptionMethod encryptionMethod,
                                                                JweAlgorithm algorithm) {
        // Given
        JwtReconstruction reconstruction = new JwtReconstruction();
        JwtClaimsSet claims = new JwtBuilderFactory().claims().build();
        String encryptedJwt = new JwtBuilderFactory().jwe(keyPair.getPublic()).claims(claims)
                .headers().enc(encryptionMethod).alg(algorithm).done().build();

        String[] encryptedJwtParts = encryptedJwt.split("\\."); // header.key.iv.cipherText.tag
        byte[] initialisationVector = Base64url.decode(encryptedJwtParts[2]);
        byte[] cipherText = Base64url.decode(encryptedJwtParts[3]);
        assertThat(cipherText.length).isEqualTo(16); // Should be a single block to make this simple

        // When
        // Simulate a padding oracle attack against the last byte of the cipher text. This may generate different
        // error conditions depending on MAC vs padding errors. It should not be possible to distinguish these
        // cases, otherwise the attacker can easily break the encryption.
        int ivEnd = initialisationVector.length-1;
        byte pad = 0x01;
        final Set<String> errorMessages = new HashSet<>();
        final Set<String> stackTraces = new HashSet<>();
        for (int i = 0; i < 256; ++i) {
            try {
                // NB: we are assuming CBC-mode block cipher with IV...
                byte newLastIvByte = (byte) ((initialisationVector[ivEnd] ^ i ^ pad) & 0xff);
                initialisationVector[ivEnd] = newLastIvByte;
                encryptedJwtParts[2] = Base64url.encode(initialisationVector);

                reconstruction.reconstructJwt(join(encryptedJwtParts, '.'), EncryptedJwt.class)
                        .decrypt(keyPair.getPrivate());

            } catch (JweDecryptionException ex) {
                errorMessages.add(ex.getMessage());
                StringWriter buffer = new StringWriter();
                ex.printStackTrace(new PrintWriter(buffer));
                stackTraces.add(buffer.toString());
            }
        }

        // Then
        assertThat(errorMessages).as("Distinct error messages").hasSize(1);
        assertThat(stackTraces).as("Distinct stack traces").hasSize(1);
    }

    @DataProvider
    public Object[][] encryptionMethods() {
        return new Object[][] {
                { EncryptionMethod.A128CBC_HS256, JweAlgorithm.RSAES_PKCS1_V1_5 }
        };
    }

    private String join(String[] parts, char delim) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(delim);
            }
            sb.append(part);
        }
        return sb.toString();
    }
}