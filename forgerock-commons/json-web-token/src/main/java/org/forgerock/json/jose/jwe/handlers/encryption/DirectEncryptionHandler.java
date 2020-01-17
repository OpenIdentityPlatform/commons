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

import java.security.Key;

import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Supports direct encryption using a shared symmetric key.
 */
public final class DirectEncryptionHandler implements EncryptionHandler {
    private final ContentEncryptionHandler contentEncryptionHandler;

    /**
     * Constructs the direct encryption handler for the given content encryption method.
     *
     * @param encryptionMethod the content encryption method.
     */
    public DirectEncryptionHandler(final EncryptionMethod encryptionMethod) {
        this(ContentEncryptionHandler.getInstance(encryptionMethod));
    }

    @VisibleForTesting
    DirectEncryptionHandler(ContentEncryptionHandler contentEncryptionHandler) {
        this.contentEncryptionHandler = contentEncryptionHandler;
    }

    @Override
    public Key getContentEncryptionKey() {
        return null;
    }

    @Override
    public byte[] generateJWEEncryptedKey(final Key key, final Key contentEncryptionKey) {
        // As per https://tools.ietf.org/html/rfc7518#section-4.5 an empty octet sequence is used as the JWE
        // Encrypted Key value when using direct encryption.
        return new byte[0];
    }

    @Override
    public byte[] generateInitialisationVector() {
        return contentEncryptionHandler.generateInitialisationVector();
    }

    @Override
    public JweEncryption encryptPlaintext(final Key contentEncryptionKey, final byte[] initialisationVector,
            final byte[] plaintext, final byte[] additionalAuthenticatedData) {
        return contentEncryptionHandler.encrypt(contentEncryptionKey, initialisationVector, plaintext,
                additionalAuthenticatedData);
    }

    @Override
    public Key decryptContentEncryptionKey(final Key key, final byte[] encryptedContentEncryptionKey) {
        if (encryptedContentEncryptionKey != null && encryptedContentEncryptionKey.length != 0) {
            throw new JweDecryptionException();
        }
        return key;
    }

    @Override
    public byte[] decryptCiphertext(final Key contentEncryptionKey, final byte[] initialisationVector,
            final byte[] ciphertext,
            final byte[] authenticationTag, final byte[] additionalAuthenticatedData) {
        return contentEncryptionHandler.decrypt(contentEncryptionKey, initialisationVector,
                new JweEncryption(ciphertext, authenticationTag), additionalAuthenticatedData);
    }
}
