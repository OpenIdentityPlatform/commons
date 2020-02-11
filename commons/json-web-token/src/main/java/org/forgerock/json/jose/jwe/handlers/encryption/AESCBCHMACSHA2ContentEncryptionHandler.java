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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.json.jose.utils.Utils;

/**
 * Encrypts using AES in CBC mode with PKCS#5 padding and uses a separate HMAC-SHA2 tag to authenticate.
 */
final class AESCBCHMACSHA2ContentEncryptionHandler extends ContentEncryptionHandler {
    private static final Logger LOGGER = Logger.getLogger(AESCBCHMACSHA2ContentEncryptionHandler.class.getName());
    private static final String RAW_KEY_FORMAT = "RAW";
    private final EncryptionMethod method;

    AESCBCHMACSHA2ContentEncryptionHandler(final EncryptionMethod method) {
        this.method = method;
    }

    @Override
    public JweEncryption encrypt(final Key key, final byte[] iv, final byte[] plainText, final byte[] additionalData) {

        final Key macKey = macKey(key, method);
        final Key encryptionKey = encKey(key, method);

        try {
            final Cipher cipher = Cipher.getInstance(method.getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
            final byte[] cipherText = cipher.doFinal(plainText);

            long alLength = additionalData.length * 8L;
            byte[] al = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(alLength).array();

            final Mac mac = Mac.getInstance(method.getMacAlgorithm());
            mac.init(macKey);
            mac.update(additionalData);
            mac.update(iv);
            mac.update(cipherText);
            mac.update(al);

            byte[] authenticationTag = Arrays.copyOf(mac.doFinal(), method.getKeyOffset());

            return new JweEncryption(cipherText, authenticationTag);
        } catch (GeneralSecurityException e) {
            throw new JweEncryptionException(e);
        }
    }

    @Override
    public byte[] decrypt(final Key key, final byte[] iv, final JweEncryption cipherText, final byte[] additionalData) {
        final Key macKey = macKey(key, method);
        final Key encKey = encKey(key, method);

        long alLength = additionalData.length * 8L;
        byte[] al = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(alLength).array();

        try {
            final Mac mac = Mac.getInstance(method.getMacAlgorithm());
            mac.init(macKey);
            mac.update(additionalData);
            mac.update(iv);
            mac.update(cipherText.getCiphertext());
            mac.update(al);

            final byte[] tag = Arrays.copyOf(mac.doFinal(), method.getKeyOffset());

            final boolean macValid = Utils.constantEquals(tag, cipherText.getAuthenticationTag());

            final Cipher cipher = Cipher.getInstance(method.getTransformation());
            cipher.init(Cipher.DECRYPT_MODE, encKey, new IvParameterSpec(iv));
            final byte[] plainText = cipher.doFinal(cipherText.getCiphertext());

            if (!macValid) {
                throw new GeneralSecurityException("MAC verification failed");
            }

            return plainText;

        } catch (GeneralSecurityException ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Decryption failed: " + ex, ex);
            }
            throw new JweDecryptionException();
        }
    }

    @Override
    Key generateEncryptionKey() {
        // We need to generate a CEK sufficiently large to supply the key for the AES block cipher and the HMAC. As
        // the HMAC algorithms are all truncated to half of their output size, it is sufficient to generate a CEK
        // with size (AES key size) + (HMAC SHA Key Size / 2). For example, AES128HS256 will produce a CEK of length
        // 128 + (256/2) = 256 bits, while AES256HS512 will use 256 + (512/2) = 512 bits. In the latter case, we
        // cannot simply generate an "AES" key of 512 bits, as this is an invalid key size for AES (and will generate
        // an exception if we try). So instead, we generate separate keys for the MAC and the AES cipher and
        // concatenate them as CEK = MAC_KEY + ENC_KEY as per https://tools.ietf.org/html/rfc7518#section-5.2.2.1

        try {
            // The keyOffset field gives the size of the MAC key in octets
            final int macKeySize = method.getKeyOffset() * 8;
            final KeyGenerator macKeyGenerator = KeyGenerator.getInstance(method.getMacAlgorithm());
            macKeyGenerator.init(macKeySize);
            final Key macKey = macKeyGenerator.generateKey();
            if (!RAW_KEY_FORMAT.equals(macKey.getFormat())) {
                throw new IllegalStateException("HMAC KeyGenerator returned non-RAW key material!");
            }

            final int encKeySize = method.getKeySize() - macKeySize;
            final KeyGenerator encKeyGenerator = KeyGenerator.getInstance(method.getEncryptionAlgorithm());
            encKeyGenerator.init(encKeySize);
            final Key encKey = encKeyGenerator.generateKey();
            if (!RAW_KEY_FORMAT.equals(macKey.getFormat())) {
                throw new IllegalStateException("AES KeyGenerator returned non-RAW key material!");
            }

            final byte[] combinedKey = ByteBuffer.allocate(method.getKeySize() / 8)
                                                 .put(macKey.getEncoded())
                                                 .put(encKey.getEncoded())
                                                 .array();

            return new SecretKeySpec(combinedKey, method.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, "
                    + method.getEncryptionAlgorithm(), e);
        }

    }

    private static SecretKey macKey(final Key combinedKey, final EncryptionMethod method) {
        return new SecretKeySpec(combinedKey.getEncoded(), 0, method.getKeyOffset(), method.getMacAlgorithm());
    }

    private static SecretKey encKey(final Key combinedKey, final EncryptionMethod method) {
        return new SecretKeySpec(combinedKey.getEncoded(), method.getKeyOffset(), method.getKeyOffset(),
                method.getEncryptionAlgorithm());
    }
}
