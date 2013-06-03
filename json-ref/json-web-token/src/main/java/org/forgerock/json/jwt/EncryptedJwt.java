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

package org.forgerock.json.jwt;

import org.forgerock.util.encode.Base64url;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Represents a JWT which has been encrypted.
 *
 * Not yet supported.
 */
public class EncryptedJwt implements Jwt {

    /**
     * Signs the Plaintext Jwt, resulting in a SignedEncryptedJwt.
     *
     * @param algorithm The Jwt Algorithm used to perform the signing.
     * @param privateKey The private key to use to sign with.
     * @return A SignedEncryptedJwt.
     * @throws JWTBuilderException If there is a problem creating the SignedJwt.
     */
    public SignedEncryptedJwt sign(JwsAlgorithm algorithm, PrivateKey privateKey) throws JWTBuilderException {
        return new SignedEncryptedJwt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build() {

//        Object contentEncryptionKey = getContentEncryptionKey();
//
//        String encryptedKey = getEncryptedKey();
//
//        String encodedEncryptedKey = Base64url.encode(encryptedKey.getBytes(Charset.forName("UTF-8")));
//
//        String initialisationVector = generateInitialisationVector(256);
//
//        String encodedInitialisationVector = Base64url.encode(initialisationVector.getBytes(Charset.forName("UTF-8")));
//
//        String plaintext = null;
//        if (isZip()) {
//            plaintext = compressPlaintext(plaintext);
//        }
//
//        byte[] M = plaintext.getBytes(Charset.forName("UTF-8"));
//
//        String jweHeader = getJweHeader();
//
//        String encodedJweHeader = Base64url.encode(jweHeader.getBytes(Charset.forName("UTF-8")));
//
//        byte[] additionalAuthenticatedData = encodedJweHeader.getBytes(Charset.forName("UTF-8"));
//
//        AuthResult authResult = encryptPlaintext(M, contentEncryptionKey, initialisationVector, additionalAuthenticatedData);
//
//        String encodedCipherText = Base64url.encode(authResult.getCiphertext());
//
//        String encodedAuthenticationTag = Base64url.encode(authResult.getAuthenticationTag());
//
//
//        String jwt = encodedJweHeader + "." + encodedEncryptedKey + "." + encodedInitialisationVector + "." + encodedCipherText + "." + encodedAuthenticationTag;
//
//        return jwt;
        return null;
    }
//
//    private Object getContentEncryptionKey() {
//
//    }
//
//    private String getEncryptedKey() {
//
//    }
//
//    private String generateInitialisationVector(int size) {
//
//    }
//
//    private String compressPlaintext(String plaintext) {
//
//    }
//
//    private boolean isZip() {
//
//    }
//
//    private String getJweHeader() {
//
//    }

//    private AuthResult encryptPlaintext(byte[] plaintext, Object contentEncryptionKey, String initialisationVector, byte[] additionalAuthenticatedData) {
//
//        try {
//
//            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//
//
//            Key key = null;
//            AlgorithmParameterSpec algorithmParameterSpec;
//
//
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//
//            cipher.up
//
//            cipher.get
//
//
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//
//    }
}
//
//class AuthResult {
//
//    private final byte[] ciphertext;
//    private final byte[] authenticationTag;
//
//    public AuthResult(byte[] ciphertext, byte[] authenticationTag) {
//        this.ciphertext = ciphertext;
//        this.authenticationTag = authenticationTag;
//    }
//
//    public byte[] getCiphertext() {
//        return ciphertext;
//    }
//
//    public byte[] getAuthenticationTag() {
//        return authenticationTag;
//    }
//}
