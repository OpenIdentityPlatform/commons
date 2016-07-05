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

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class A128CBCHS256Test {

    private AESCBCHMACSHA2ContentEncryptionHandler handler;
    private byte[] key;
    private byte[] plainText;
    private byte[] iv;
    private byte[] additionalData;
    private byte[] cipherText;
    private byte[] tag;

    @BeforeMethod
    public void createEncryptionHandler() {
        handler = new AESCBCHMACSHA2ContentEncryptionHandler(EncryptionMethod.A128CBC_HS256);

        // K - the combined key. First 16 bytes are MAC key, last 16 are encryption key
        key = hexToBytes("00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f\n"
                + "10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f");

        // P - the plain text
        plainText = hexToBytes("41 20 63 69 70 68 65 72 20 73 79 73 74 65 6d 20\n"
                + "6d 75 73 74 20 6e 6f 74 20 62 65 20 72 65 71 75\n"
                + "69 72 65 64 20 74 6f 20 62 65 20 73 65 63 72 65\n"
                + "74 2c 20 61 6e 64 20 69 74 20 6d 75 73 74 20 62\n"
                + "65 20 61 62 6c 65 20 74 6f 20 66 61 6c 6c 20 69\n"
                + "6e 74 6f 20 74 68 65 20 68 61 6e 64 73 20 6f 66\n"
                + "20 74 68 65 20 65 6e 65 6d 79 20 77 69 74 68 6f\n"
                + "75 74 20 69 6e 63 6f 6e 76 65 6e 69 65 6e 63 65");

        // IV - the initialisation vector
        iv = hexToBytes("1a f3 8c 2d c2 b9 6f fd d8 66 94 09 23 41 bc 04");

        // A - the additional data to sign (of AL bits = 336 bits in size)
        additionalData = hexToBytes("54 68 65 20 73 65 63 6f 6e 64 20 70 72 69 6e 63\n"
                + "69 70 6c 65 20 6f 66 20 41 75 67 75 73 74 65 20\n"
                + "4b 65 72 63 6b 68 6f 66 66 73");

        // E - the encrypted cipher text
        cipherText = hexToBytes("c8 0e df a3 2d df 39 d5 ef 00 c0 b4 68 83 42 79\n"
                + "a2 e4 6a 1b 80 49 f7 92 f7 6b fe 54 b9 03 a9 c9\n"
                + "a9 4a c9 b4 7a d2 65 5c 5f 10 f9 ae f7 14 27 e2\n"
                + "fc 6f 9b 3f 39 9a 22 14 89 f1 63 62 c7 03 23 36\n"
                + "09 d4 5a c6 98 64 e3 32 1c f8 29 35 ac 40 96 c8\n"
                + "6e 13 33 14 c5 40 19 e8 ca 79 80 df a4 b9 cf 1b\n"
                + "38 4c 48 6f 3a 54 c5 10 78 15 8e e5 d7 9d e5 9f\n"
                + "bd 34 d8 48 b3 d6 95 50 a6 76 46 34 44 27 ad e5\n"
                + "4b 88 51 ff b5 98 f7 f8 00 74 b9 47 3c 82 e2 db");

        // T - the truncated MAC tag (we do not have access to the full MAC, M).
        tag = hexToBytes("65 2c 3f a3 6b 0a 7c 5b 32 19 fa b3 a3 0b c1 c4");
    }

    @Test
    public void shouldEncryptCorrectly() {
        // Given
        final Key secretKey = new SecretKeySpec(key, "AES");

        // When
        final JweEncryption result = handler.encrypt(secretKey, iv, plainText, additionalData);

        // Then
        assertThat(result.getCiphertext()).as("ciphertext").isEqualTo(cipherText);
        assertThat(result.getAuthenticationTag()).as("MAC").isEqualTo(tag);
    }

    @Test
    public void shouldDecryptCorrectly() {
        // Given
        final Key secretKey = new SecretKeySpec(key, "AES");

        // When
        byte[] result = handler.decrypt(secretKey, iv, new JweEncryption(cipherText, tag), additionalData);

        // Then
        assertThat(result).as("decrypted plaintext").isEqualTo(plainText);
    }

    @Test(expectedExceptions = JweDecryptionException.class)
    public void shouldRejectIncorrectMac() {
        // Given
        final Key secretKey = new SecretKeySpec(key, "AES");
        tag[0] = (byte) -tag[0];

        // When
        handler.decrypt(secretKey, iv, new JweEncryption(cipherText, tag), additionalData);

        // Then - exception
    }

    private byte[] hexToBytes(String hex) {
        return DatatypeConverter.parseHexBinary(hex.replaceAll("\\s+", ""));
    }


}