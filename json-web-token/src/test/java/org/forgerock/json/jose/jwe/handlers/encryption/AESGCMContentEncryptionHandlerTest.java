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

package org.forgerock.json.jose.jwe.handlers.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.jose.jwe.handlers.encryption.JWETestUtils.bytes;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class AESGCMContentEncryptionHandlerTest {

    /**
     * Test based on the AES-GCM parts of https://tools.ietf.org/html/rfc7516#appendix-A.1 (ignoring the RSA layer).
     */
    @Test
    public void shouldMatchJWEExample() throws Exception {
        if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
            throw new SkipException("Please install JCE Unlimited Strength to test AES-256");
        }

        // Given
        AESGCMContentEncryptionHandler handler = new AESGCMContentEncryptionHandler(EncryptionMethod.A256GCM);
        byte[] plaintext = bytes(
                84, 104, 101, 32, 116, 114, 117, 101, 32, 115, 105, 103, 110, 32,
                111, 102, 32, 105, 110, 116, 101, 108, 108, 105, 103, 101, 110, 99,
                101, 32, 105, 115, 32, 110, 111, 116, 32, 107, 110, 111, 119, 108,
                101, 100, 103, 101, 32, 98, 117, 116, 32, 105, 109, 97, 103, 105,
                110, 97, 116, 105, 111, 110, 46);
        Key key = new SecretKeySpec(bytes(177, 161, 244, 128, 84, 143, 225, 115, 63, 180, 3, 255, 107, 154,
                212, 246, 138, 7, 110, 91, 112, 46, 34, 105, 47, 130, 203, 46, 122,
                234, 64, 252), "AES");
        byte[] iv = bytes(227, 197, 117, 252, 2, 219, 233, 68, 180, 225, 77, 219);
        byte[] aad = bytes(101, 121, 74, 104, 98, 71, 99, 105, 79, 105, 74, 83, 85, 48, 69,
                116, 84, 48, 70, 70, 85, 67, 73, 115, 73, 109, 86, 117, 89, 121, 73,
                54, 73, 107, 69, 121, 78, 84, 90, 72, 81, 48, 48, 105, 102, 81);

        byte[] expectedCipherText = bytes(229, 236, 166, 241, 53, 191, 115, 196, 174, 43, 73, 109, 39, 122,
                233, 96, 140, 206, 120, 52, 51, 237, 48, 11, 190, 219, 186, 80, 111,
                104, 50, 142, 47, 167, 59, 61, 181, 127, 196, 21, 40, 82, 242, 32,
                123, 143, 168, 226, 73, 216, 176, 144, 138, 247, 106, 60, 16, 205,
                160, 109, 64, 63, 192);
        byte[] expectedAuthTag = bytes(92, 80, 104, 49, 133, 25, 161, 215, 173, 101, 219, 211, 136, 91,
                210, 145);

        // When
        final JweEncryption result = handler.encrypt(key, iv, plaintext, aad);

        // Then
        assertThat(result.getCiphertext()).isEqualTo(expectedCipherText);
        assertThat(result.getAuthenticationTag()).isEqualTo(expectedAuthTag);
    }


}