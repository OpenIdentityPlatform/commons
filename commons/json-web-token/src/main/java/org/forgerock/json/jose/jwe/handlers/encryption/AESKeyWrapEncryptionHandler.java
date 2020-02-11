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

import javax.crypto.Cipher;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;

/**
 * Provides JWE key encapsulation using the AES KeyWrap algorithm.
 */
public final class AESKeyWrapEncryptionHandler implements EncryptionHandler {
    private final ContentEncryptionHandler contentEncryptionHandler;
    private final EncryptionMethod encryptionMethod;

    /**
     * Constructs an AES KeyWrap encryption handler for the given underlying content encryption method.
     *
     * @param method the content encryption method.
     */
    public AESKeyWrapEncryptionHandler(final EncryptionMethod method) {
        this.contentEncryptionHandler = ContentEncryptionHandler.getInstance(method);
        this.encryptionMethod = method;
    }

    @Override
    public Key getContentEncryptionKey() {
        return contentEncryptionHandler.generateEncryptionKey();
    }

    @Override
    public byte[] generateJWEEncryptedKey(final Key key, final Key contentEncryptionKey) {
        try {
            final Cipher cipher = Cipher.getInstance("AESWrap");
            cipher.init(Cipher.WRAP_MODE, key);
            return cipher.wrap(contentEncryptionKey);
        } catch (GeneralSecurityException e) {
            throw new JweEncryptionException(e);
        }
    }

    @Override
    public byte[] generateInitialisationVector() {
        return contentEncryptionHandler.generateInitialisationVector();
    }

    @Override
    public JweEncryption encryptPlaintext(final Key contentEncryptionKey, final byte[] initialisationVector,
            final byte[] plaintext,
            final byte[] additionalAuthenticatedData) {
        return contentEncryptionHandler.encrypt(contentEncryptionKey, initialisationVector, plaintext,
                additionalAuthenticatedData);
    }

    @Override
    public Key decryptContentEncryptionKey(final Key key, final byte[] encryptedContentEncryptionKey) {
        try {
            final Cipher cipher = Cipher.getInstance("AESWrap");
            cipher.init(Cipher.UNWRAP_MODE, key);
            return cipher.unwrap(encryptedContentEncryptionKey, encryptionMethod.getEncryptionAlgorithm(),
                    Cipher.SECRET_KEY);
        } catch (GeneralSecurityException e) {
            throw new JweDecryptionException();
        }
    }

    @Override
    public byte[] decryptCiphertext(final Key contentEncryptionKey, final byte[] initialisationVector,
            final byte[] ciphertext,
            final byte[] authenticationTag, final byte[] additionalAuthenticatedData) {
        return contentEncryptionHandler.decrypt(contentEncryptionKey, initialisationVector,
                new JweEncryption(ciphertext, authenticationTag), additionalAuthenticatedData);
    }
}
