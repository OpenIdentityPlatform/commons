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

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;

/**
 * Encrypts content using Galois Counter Mode (GCM), an efficient authenticated encryption with associated data
 * (AEAD) mode. Compared to {@link AESCBCHMACSHA2ContentEncryptionHandler} this is a more efficient combined AEAD
 * mode that does not require padding. On the flip side, if you ever repeat a key/IV pair then the security of the
 * cipher is totally compromised.
 */
final class AESGCMContentEncryptionHandler extends ContentEncryptionHandler {
    private static final Logger LOGGER = Logger.getLogger(AESGCMContentEncryptionHandler.class.getName());
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 96 / 8;

    private final EncryptionMethod encryptionMethod;

    AESGCMContentEncryptionHandler(final EncryptionMethod encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    @Override
    JweEncryption encrypt(final Key key, final byte[] iv, final byte[] plainText, final byte[] additionalData) {
        try {
            final Cipher cipher = Cipher.getInstance(encryptionMethod.getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            cipher.updateAAD(additionalData);
            final byte[] cipherText = cipher.doFinal(plainText);

            final int tagStart = cipherText.length - TAG_LENGTH / 8;
            return new JweEncryption(Arrays.copyOfRange(cipherText, 0, tagStart),
                    Arrays.copyOfRange(cipherText, tagStart, cipherText.length));
        } catch (GeneralSecurityException ex) {
            throw new JweEncryptionException(ex);
        }
    }

    @Override
    byte[] decrypt(final Key key, final byte[] iv, final JweEncryption cipherText, final byte[] additionalData) {
        try {
            final Cipher cipher = Cipher.getInstance(encryptionMethod.getTransformation());
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            cipher.updateAAD(additionalData);
            cipher.update(cipherText.getCiphertext());
            return cipher.doFinal(cipherText.getAuthenticationTag());
        } catch (GeneralSecurityException ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Decryption failed: " + ex, ex);
            }
            throw new JweDecryptionException();
        }
    }

    @Override
    Key generateEncryptionKey() {
        try {
            final KeyGenerator encKeyGenerator = KeyGenerator.getInstance(encryptionMethod.getEncryptionAlgorithm());
            encKeyGenerator.init(encryptionMethod.getKeySize());
            return encKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, "
                    + encryptionMethod.getEncryptionAlgorithm(), e);
        }
    }

    @Override
    int getIVByteLength() {
        return IV_LENGTH;
    }
}
