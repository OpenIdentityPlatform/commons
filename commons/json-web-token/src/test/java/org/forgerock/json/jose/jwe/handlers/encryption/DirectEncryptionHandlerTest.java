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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DirectEncryptionHandlerTest {

    @Mock
    private ContentEncryptionHandler mockContentEncryptionHandler;

    private DirectEncryptionHandler directEncryptionHandler;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        directEncryptionHandler = new DirectEncryptionHandler(mockContentEncryptionHandler);
    }

    @Test
    public void shouldUseSharedKeyDirectly() {
        assertThat(directEncryptionHandler.getContentEncryptionKey()).isNull();
    }

    @Test
    public void shouldUseEmptyJweEncryptedKey() {
        // Given
        Key kek = new SecretKeySpec(new byte[16], "KEK");
        Key cek = new SecretKeySpec(new byte[16], "CEK");

        // When
        byte[] encryptedKey = directEncryptionHandler.generateJWEEncryptedKey(kek, cek);

        // Then
        assertThat(encryptedKey).isEmpty();
    }

    @Test
    public void shouldDelegateIVGenerationToContentEncryptionMethod() {
        directEncryptionHandler.generateInitialisationVector();
        verify(mockContentEncryptionHandler).generateInitialisationVector();
    }

    @Test
    public void shouldDelegateEncryptionDirectlyToContentEncryptionMethod() {
        // Given
        Key cek = new SecretKeySpec(new byte[16], "CEK");
        byte[] iv = "Initialisation Vector".getBytes(StandardCharsets.UTF_8);
        byte[] plainText = "Some test message".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "Additional authenticated data".getBytes(StandardCharsets.UTF_8);
        JweEncryption jweEncryption = new JweEncryption(null, null);
        given(mockContentEncryptionHandler.encrypt(cek, iv, plainText, aad)).willReturn(jweEncryption);

        // When
        JweEncryption result = directEncryptionHandler.encryptPlaintext(cek, iv, plainText, aad);

        // Then
        verify(mockContentEncryptionHandler).encrypt(cek, iv, plainText, aad);
        assertThat(result).isSameAs(jweEncryption);
    }

    @Test
    public void shouldUseSharedKeyForDecryption() {
        // Given
        Key sharedKey = new SecretKeySpec(new byte[16], "KEK");

        // When
        Key result = directEncryptionHandler.decryptContentEncryptionKey(sharedKey, new byte[0]);

        // Then
        assertThat(result).isSameAs(sharedKey);
    }

    @Test(expectedExceptions = JweDecryptionException.class)
    public void shouldRejectEncryptedJweKey() {
        Key sharedKey = new SecretKeySpec(new byte[16], "KEK");
        directEncryptionHandler.decryptContentEncryptionKey(sharedKey, new byte[1]);
    }

    @Test
    public void shouldDelegateDecryptionToContentEncryptionHandler() {
        // Given
        Key cek = new SecretKeySpec(new byte[16], "CEK");
        byte[] iv = "IV".getBytes(StandardCharsets.UTF_8);
        byte[] cipherText = "Secret Message".getBytes(StandardCharsets.UTF_8);
        byte[] tag = "Tag".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "Additional authenticated data".getBytes(StandardCharsets.UTF_8);
        byte[] plainText = "Plain Text".getBytes(StandardCharsets.UTF_8);
        given(mockContentEncryptionHandler.decrypt(cek, iv, new JweEncryption(cipherText, tag), aad))
                .willReturn(plainText);

        // When
        byte[] result = directEncryptionHandler.decryptCiphertext(cek, iv, cipherText, tag, aad);

        // Then
        verify(mockContentEncryptionHandler).decrypt(cek, iv, new JweEncryption(cipherText, tag), aad);
        assertThat(result).isSameAs(plainText);
    }
}