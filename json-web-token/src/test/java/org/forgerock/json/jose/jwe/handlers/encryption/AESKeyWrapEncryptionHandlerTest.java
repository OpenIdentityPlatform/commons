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

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.Test;

public class AESKeyWrapEncryptionHandlerTest {

    /**
     * <a href="https://tools.ietf.org/html/rfc7516#appendix-A.3">https://tools.ietf.org/html/rfc7516#appendix-A.3</a>
     */
    @Test
    public void shouldMatchJWESpecExample() {
        // Given
        byte[] plaintext = bytes(76, 105, 118, 101, 32, 108, 111, 110, 103, 32, 97, 110, 100, 32,
                112, 114, 111, 115, 112, 101, 114, 46);
        byte[] cek = bytes(4, 211, 31, 197, 84, 157, 252, 254, 11, 100, 157, 250, 63, 170, 106,
                206, 107, 124, 212, 45, 111, 107, 9, 219, 200, 177, 0, 240, 143, 156,
                44, 207);
        byte[] iv = bytes(3, 22, 60, 12, 43, 67, 104, 105, 108, 108, 105, 99, 111, 116, 104,
                101);
        byte[] aad = bytes(101, 121, 74, 104, 98, 71, 99, 105, 79, 105, 74, 66, 77, 84, 73, 52,
                83, 49, 99, 105, 76, 67, 74, 108, 98, 109, 77, 105, 79, 105, 74, 66,
                77, 84, 73, 52, 81, 48, 74, 68, 76, 85, 104, 84, 77, 106, 85, 50, 73,
                110, 48);
        Key kek = new SecretKeySpec(Base64url.decode("GawgguFyGrWKav7AX4VKUg"), "AES");
        AESKeyWrapEncryptionHandler encryptionHandler = new AESKeyWrapEncryptionHandler(EncryptionMethod.A128CBC_HS256);
        byte[] expectedEncryptedKey = bytes(232, 160, 123, 211, 183, 76, 245, 132, 200, 128, 123, 75, 190, 216,
                22, 67, 201, 138, 193, 186, 9, 91, 122, 31, 246, 90, 28, 139, 57, 3,
                76, 124, 193, 11, 98, 37, 173, 61, 104, 57);
        byte[] expectedCipherText = bytes(40, 57, 83, 181, 119, 33, 133, 148, 198, 185, 243, 24, 152, 230, 6,
                75, 129, 223, 127, 19, 210, 82, 183, 230, 168, 33, 215, 104, 143,
                112, 56, 102);
        byte[] expectedAuthTag = bytes(83, 73, 191, 98, 104, 205, 211, 128, 201, 189, 199, 133, 32, 38,
                194, 85);
        Key contentEncryptionKey = new SecretKeySpec(cek, "AES");

        // When
        final byte[] jweEncryptedKey = encryptionHandler.generateJWEEncryptedKey(kek, contentEncryptionKey);
        final JweEncryption result = encryptionHandler.encryptPlaintext(contentEncryptionKey, iv, plaintext, aad);

        // Then
        assertThat(jweEncryptedKey).as("Encrypted Content Encryption Key").isEqualTo(expectedEncryptedKey);
        assertThat(result.getCiphertext()).as("Encrypted ciphertext").isEqualTo(expectedCipherText);
        assertThat(result.getAuthenticationTag()).as("Authentication tag").isEqualTo(expectedAuthTag);
    }
}