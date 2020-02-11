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

/**
 * Data from: https://tools.ietf.org/html/rfc7518#appendix-B.3
 */
public class A256HS512Test {

    private AESCBCHMACSHA2ContentEncryptionHandler handler;
    private byte[] key;
    private byte[] plainText;
    private byte[] iv;
    private byte[] additionalData;
    private byte[] cipherText;
    private byte[] tag;

    @BeforeMethod
    public void createEncryptionHandler() throws NoSuchAlgorithmException {
        if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
            throw new SkipException("Unable to test AES-256: Install JCE Unlimited Strength.");
        }

        handler = new AESCBCHMACSHA2ContentEncryptionHandler(EncryptionMethod.A256CBC_HS512);

        // K - the combined key. First 32 bytes are MAC key, last 32 are encryption key
        key = hexToBytes("00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f\n"
                + "10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f\n"
                + "20 21 22 23 24 25 26 27 28 29 2a 2b 2c 2d 2e 2f\n"
                + "30 31 32 33 34 35 36 37 38 39 3a 3b 3c 3d 3e 3f");

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
        cipherText = hexToBytes("4a ff aa ad b7 8c 31 c5 da 4b 1b 59 0d 10 ff bd\n"
                + "3d d8 d5 d3 02 42 35 26 91 2d a0 37 ec bc c7 bd\n"
                + "82 2c 30 1d d6 7c 37 3b cc b5 84 ad 3e 92 79 c2\n"
                + "e6 d1 2a 13 74 b7 7f 07 75 53 df 82 94 10 44 6b\n"
                + "36 eb d9 70 66 29 6a e6 42 7e a7 5c 2e 08 46 a1\n"
                + "1a 09 cc f5 37 0d c8 0b fe cb ad 28 c7 3f 09 b3\n"
                + "a3 b7 5e 66 2a 25 94 41 0a e4 96 b2 e2 e6 60 9e\n"
                + "31 e6 e0 2c c8 37 f0 53 d2 1f 37 ff 4f 51 95 0b\n"
                + "be 26 38 d0 9d d7 a4 93 09 30 80 6d 07 03 b1 f6");

        // T - the truncated MAC tag (we do not have access to the full MAC, M).
        tag = hexToBytes("4d d3 b4 c0 88 a7 f4 5c 21 68 39 64 5b 20 12 bf\n"
                + "2e 62 69 a8 c5 6a 81 6d bc 1b 26 77 61 95 5b c5");
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
