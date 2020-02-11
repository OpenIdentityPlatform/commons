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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.forgerock.json.jose.exceptions.JweEncryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweEncryption;
import org.forgerock.util.Reject;

/**
 * Handles the underlying {@link EncryptionMethod}.
 */
abstract class ContentEncryptionHandler {
    private static final String INITIALISATION_VECTOR_ALGORITHM = "SHA1PRNG";

    /**
     * Returns an appropriate content encryption handler for the given encryption method.
     *
     * @param method the encryption method.
     * @return an appropriate handler for the given encryption method.
     */
    static ContentEncryptionHandler getInstance(EncryptionMethod method) {
        Reject.ifNull(method, "EncryptionMethod cannot be null");
        switch (method) {
        case A128CBC_HS256:
        case A192CBC_HS384:
        case A256CBC_HS512:
            return new AESCBCHMACSHA2ContentEncryptionHandler(method);
        case A128GCM:
        case A192GCM:
        case A256GCM:
            return new AESGCMContentEncryptionHandler(method);
        default:
            throw new UnsupportedOperationException("Unsupported encryption method: " + method);
        }
    }

    abstract JweEncryption encrypt(Key key, byte[] iv, byte[] plainText, byte[] additionalData);

    abstract byte[] decrypt(Key key, byte[] iv, JweEncryption cipherText, byte[] additionalData);

    abstract Key generateEncryptionKey();

    byte[] generateInitialisationVector() {
        try {
            final int ivByteLength = getIVByteLength();
            SecureRandom randomGen = SecureRandom.getInstance(INITIALISATION_VECTOR_ALGORITHM);

            byte[] bytes = new byte[ivByteLength];
            randomGen.nextBytes(bytes);
            return bytes;
        } catch (NoSuchAlgorithmException e) {
            throw new JweEncryptionException("Unsupported Algorithm, " + INITIALISATION_VECTOR_ALGORITHM, e);
        }
    }

    int getIVByteLength() {
        return 128 / 8;
    }
}
