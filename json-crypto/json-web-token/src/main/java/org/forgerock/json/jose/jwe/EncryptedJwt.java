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

package org.forgerock.json.jose.jwe;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.jose.jwe.handlers.EncryptionHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtHeader;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.encode.Base64url;

import java.io.IOException;
import java.security.Key;
import java.util.Map;

public class EncryptedJwt implements Jwt {

    private final EncryptionManager encryptionManager = new EncryptionManager();

    private final JweHeader header;

    private JwtClaimsSet claimsSet;
    private final Key publicKey;

    private final byte[] encryptedContentEncryptionKey;
    private final byte[] initialisationVector;
    private final byte[] ciphertext;
    private final byte[] additionalAuthenticatedData;

    public EncryptedJwt(JweHeader header, JwtClaimsSet claimsSet, Key publicKey) {
        this.header = header;
        this.claimsSet = claimsSet;
        this.publicKey = publicKey;


        this.encryptedContentEncryptionKey = null;
        this.initialisationVector = null;
        this.ciphertext = null;
        this.additionalAuthenticatedData = null;
    }

    public EncryptedJwt(JweHeader header, byte[] encryptedContentEncryptionKey, byte[] initialisationVector,
            byte[] ciphertext, byte[] additionalAuthenticatedData) {
        this.header = header;
        this.encryptedContentEncryptionKey = encryptedContentEncryptionKey;
        this.initialisationVector = initialisationVector;
        this.ciphertext = ciphertext;
        this.additionalAuthenticatedData = additionalAuthenticatedData;


        this.publicKey = null;
    }

    @Override
    public JwtHeader getHeader() {
        return header;
    }

    @Override
    public JwtClaimsSet getClaimsSet() {
        return claimsSet;
    }

    @Override
    public String build() {

        EncryptionHandler encryptionHandler = encryptionManager.getEncryptionHandler(header);

        Key contentEncryptionKey = encryptionHandler.getContentEncryptionKey();
        byte[] encryptedContentEncryptionKey = encryptionHandler.encryptContentEncryptionKey(publicKey, contentEncryptionKey);
        String encodedEncryptedKey = Base64url.encode(encryptedContentEncryptionKey);


        byte[] initialisationVector = encryptionHandler.generateInitialisationVector();
        String encodedInitialisationVector = Base64url.encode(initialisationVector);


        String jweHeader = header.build();
        String encodedJweHeader = Base64url.encode(jweHeader.getBytes(Utils.CHARSET));
        byte[] plaintext = compressPlaintext(header.getCompressionAlgorithm(), claimsSet.build().getBytes(Utils.CHARSET));
        byte[] additionalAuthenticatedData = encodedJweHeader.getBytes(Utils.CHARSET);
        Object[] cipherTextAndAuthTag = encryptionHandler.encryptPlaintext(contentEncryptionKey, initialisationVector, plaintext, additionalAuthenticatedData);

        String encodedCiphertext = Base64url.encode((byte[]) cipherTextAndAuthTag[0]);
        String encodedAuthenticationTag = Base64url.encode((byte[]) cipherTextAndAuthTag[1]);


        return encodedJweHeader + "." + encodedEncryptedKey + "." + encodedInitialisationVector + "." + encodedCiphertext + "." + encodedAuthenticationTag;
    }

    private byte[] compressPlaintext(CompressionAlgorithm compressionAlgorithm, byte[] plaintext) {
        //TODO
        return plaintext;
    }

    public void decrypt(Key privateKey) {

        EncryptionHandler encryptionHandler = encryptionManager.getEncryptionHandler(header);

        Key contentEncryptionKey = encryptionHandler.decryptContentEncryptionKey(privateKey, encryptedContentEncryptionKey);


//        String jweHeader = header.build();
//        String encodedJweHeader = Base64url.encode(jweHeader.getBytes(Utils.CHARSET));
//        byte[] additionalAuthenticatedData = encodedJweHeader.getBytes(Utils.CHARSET);
        byte[] plaintext = encryptionHandler.decryptCiphertext(contentEncryptionKey, initialisationVector, ciphertext, additionalAuthenticatedData);


        String claimsSetString = new String(plaintext, Utils.CHARSET);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            claimsSet = new JwtClaimsSet(objectMapper.readValue(claimsSetString, Map.class));
        } catch (IOException e) {
            //TODO
            throw new JsonException(e);
        }
    }
}
