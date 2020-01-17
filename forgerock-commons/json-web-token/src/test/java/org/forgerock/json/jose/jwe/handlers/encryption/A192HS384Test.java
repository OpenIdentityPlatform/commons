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
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class A192HS384Test {

    private AESCBCHMACSHA2ContentEncryptionHandler handler;
    private byte[] key;
    private byte[] plainText;
    private byte[] iv;
    private byte[] additionalData;
    private byte[] cipherText;
    private byte[] tag;

    @BeforeMethod
    public void createEncryptionHandler() throws NoSuchAlgorithmException {
        if (Cipher.getMaxAllowedKeyLength("AES") < 192) {
            throw new SkipException("Unable to test AES-192: Install JCE Unlimited Strength.");
        }

        handler = new AESCBCHMACSHA2ContentEncryptionHandler(EncryptionMethod.A192CBC_HS384);

        // K - the combined key. First 16 bytes are MAC key, last 16 are encryption key
        key = hexToBytes("00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f\n"
                + "10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f\n"
                + "20 21 22 23 24 25 26 27 28 29 2a 2b 2c 2d 2e 2f");

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
        cipherText = hexToBytes("ea 65 da 6b 59 e6 1e db 41 9b e6 2d 19 71 2a e5\n"
                + "d3 03 ee b5 00 52 d0 df d6 69 7f 77 22 4c 8e db\n"
                + "00 0d 27 9b dc 14 c1 07 26 54 bd 30 94 42 30 c6\n"
                + "57 be d4 ca 0c 9f 4a 84 66 f2 2b 22 6d 17 46 21\n"
                + "4b f8 cf c2 40 0a dd 9f 51 26 e4 79 66 3f c9 0b\n"
                + "3b ed 78 7a 2f 0f fc bf 39 04 be 2a 64 1d 5c 21\n"
                + "05 bf e5 91 ba e2 3b 1d 74 49 e5 32 ee f6 0a 9a\n"
                + "c8 bb 6c 6b 01 d3 5d 49 78 7b cd 57 ef 48 49 27\n"
                + "f2 80 ad c9 1a c0 c4 e7 9c 7b 11 ef c6 00 54 e3");

        // T - the truncated MAC tag (we do not have access to the full MAC, M).
        tag = hexToBytes("84 90 ac 0e 58 94 9b fe 51 87 5d 73 3f 93 ac 20 75 16 80 39 cc c7 33 d7");
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