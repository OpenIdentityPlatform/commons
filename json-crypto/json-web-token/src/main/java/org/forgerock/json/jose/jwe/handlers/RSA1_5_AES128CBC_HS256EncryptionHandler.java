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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jose.jwe.handlers;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class RSA1_5_AES128CBC_HS256EncryptionHandler extends AbstractEncryptionHandler {

    private static final JweAlgorithm ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;

    private final SigningManager signingManager;

    public RSA1_5_AES128CBC_HS256EncryptionHandler(SigningManager signingManager) {
        this.signingManager = signingManager;
    }

    @Override
    public Key getContentEncryptionKey() {

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_METHOD.getEncryptionAlgorithm());//TODO add provider?? BouncyCastle
            keyGenerator.init(ENCRYPTION_METHOD.getKeySize());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encryptContentEncryptionKey(Key key, Key contentEncryptionKey) {
        return encrypt(ALGORITHM.getAlgorithm(), key, contentEncryptionKey.getEncoded());
    }

    @Override
    public byte[] generateInitialisationVector() {
        try {
            final int IV_BIT_LENGTH = 128;
            SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");

            byte[] bytes = new byte[IV_BIT_LENGTH / 8];
            randomGen.nextBytes(bytes);
            return bytes;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object[] encryptPlaintext(Key contentEncryptionKey, byte[] initialisationVector, byte[] plaintext, byte[] additionalAuthenticatedData) {

        int keyOffset = ENCRYPTION_METHOD.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset, ENCRYPTION_METHOD.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset, ENCRYPTION_METHOD.getEncryptionAlgorithm());

        byte[] ciphertext = encrypt(ENCRYPTION_METHOD.getTransformation(), encryptionKey, initialisationVector, plaintext);


        int alLength = additionalAuthenticatedData.length * 8;
        byte[] al = ByteBuffer.allocate(8).putInt(alLength).array();


        int authenticationTagInputLength = additionalAuthenticatedData.length + initialisationVector.length + ciphertext.length + al.length;
        byte[] dataBytes = ByteBuffer.allocate(authenticationTagInputLength).put(additionalAuthenticatedData).put(initialisationVector).put(ciphertext).put(al).array();
        byte[] hmac = signingManager.signWithHMAC(macKey.getAlgorithm(), macKey, dataBytes);

        byte[] authenticationTag = Arrays.copyOf(hmac, ENCRYPTION_METHOD.getKeyOffset());

        return new Object[]{ciphertext, authenticationTag};
    }

    @Override
    public Key decryptContentEncryptionKey(Key key, byte[] encryptedContentEncryptionKey) {

        byte[] contentEncryptionKey = decrypt(ALGORITHM.getAlgorithm(), key, encryptedContentEncryptionKey);

        return new SecretKeySpec(contentEncryptionKey, ENCRYPTION_METHOD.getEncryptionAlgorithm());
    }

    @Override
    public byte[] decryptCiphertext(Key contentEncryptionKey, byte[] initialisationVector, byte[] ciphertext, byte[] additionalAuthenticatedData) {

        int keyOffset = ENCRYPTION_METHOD.getKeyOffset();

        Key macKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), 0, keyOffset, ENCRYPTION_METHOD.getMacAlgorithm());
        Key encryptionKey = new SecretKeySpec(contentEncryptionKey.getEncoded(), keyOffset, keyOffset, ENCRYPTION_METHOD.getEncryptionAlgorithm());


        int alLength = additionalAuthenticatedData.length * 8;
        byte[] al = ByteBuffer.allocate(8).putInt(alLength).array();

        int authenticationTagInputLength = additionalAuthenticatedData.length + initialisationVector.length + ciphertext.length + al.length;
        byte[] dataBytes = ByteBuffer.allocate(authenticationTagInputLength).put(additionalAuthenticatedData).put(initialisationVector).put(ciphertext).put(al).array();
        byte[] hmac = signingManager.signWithHMAC(macKey.getAlgorithm(), macKey, dataBytes);

        byte[] expectedAuthenticationTag = Arrays.copyOf(hmac, ENCRYPTION_METHOD.getKeyOffset());


        byte[] plaintext = decrypt(ENCRYPTION_METHOD.getTransformation(), encryptionKey, initialisationVector, ciphertext);

        //TODO some check around timing attacks??

        return plaintext;
    }
}
