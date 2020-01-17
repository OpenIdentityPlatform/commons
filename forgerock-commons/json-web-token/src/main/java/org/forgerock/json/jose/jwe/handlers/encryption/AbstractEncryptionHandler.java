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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe.handlers.encryption;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;

/**
 * A base implementation of an EncryptionHandler that provides common encryption and decryption methods for all
 * concrete EncryptionHandler implementations.
 *
 * @since 2.0.0
 * @deprecated Use {@link ContentEncryptionHandler} instead.
 */
@Deprecated
public abstract class AbstractEncryptionHandler implements EncryptionHandler {
    private static final Logger LOGGER = Logger.getLogger(AbstractEncryptionHandler.class.getName());

    /**
     * Encrypts the given plaintext using the specified key with the specified encryption algorithm.
     *
     * @param algorithm The Java Cryptographic encryption algorithm.
     * @param key The encryption key.
     * @param data The data to encrypt.
     * @return An array of bytes representing the encrypted data.
     */
    protected byte[] encrypt(String algorithm, Key key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, " + algorithm, e);
        } catch (IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | BadPaddingException e) {
            throw new JweEncryptionException(e);
        }
    }

    /**
     * Encrypts the given plaintext using the specified key and initialisation vector with the specified encryption
     * algorithm.
     *
     * @param algorithm The Java Cryptographic encryption algorithm.
     * @param key The encryption key.
     * @param initialisationVector The initialisation vector.
     * @param data The data to encrypt.
     * @return An array of bytes representing the encrypted data.
     */
    protected byte[] encrypt(String algorithm, Key key, byte[] initialisationVector, byte[] data) {

        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), key.getAlgorithm());
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initialisationVector);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Encryption Algorithm, " + algorithm, e);
        } catch (IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | BadPaddingException
                | InvalidAlgorithmParameterException e) {
            throw new JweEncryptionException(e);
        }
    }

    /**
     * Decrypts the given ciphertext using the private key and with the same encryption algorithm that was used in the
     * encryption.
     *
     * @param algorithm The Java Cryptographic encryption algorithm.
     * @param privateKey The private key pair to the public key used in the encryption.
     * @param data The ciphertext to decrypt.
     * @return An array of bytes representing the decrypted data.
     */
    public byte[] decrypt(String algorithm, Key privateKey, byte[] data) {

        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            logDecryptionFailure(e);
            throw new JweDecryptionException();
        }
    }

    /**
     * Decrypts the given ciphertext using the private key and initialisation vector with the same encryption algorithm
     * that was used in the encryption.
     *
     * @param algorithm The Java Cryptographic encryption algorithm.
     * @param key The private key pair to the public key used in the encryption.
     * @param initialisationVector The same initialisation vector that was used in the encryption.
     * @param data The ciphertext to decrypt.
     * @return An array of bytes representing the decrypted data.
     */
    protected byte[] decrypt(String algorithm, Key key, byte[] initialisationVector, byte[] data) {

        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), key.getAlgorithm());
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initialisationVector);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            logDecryptionFailure(e);
            throw new JweDecryptionException();
        }
    }

    /**
     * Log the root cause of any decryption error before throwing a generic exception.
     */
    private void logDecryptionFailure(Throwable cause) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Decryption failed: " + cause, cause);
        }
    }
}
