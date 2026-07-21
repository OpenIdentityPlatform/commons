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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 * Portions Copyrighted 2020-2026 3A Systems, LLC
 */

package org.forgerock.json.crypto.simple;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.JsonValue;
import org.forgerock.util.encode.Base64;

/**
 * Encrypts a JSON value into an {@code x-simple-encryption} type {@code $crypto} JSON object.
 */
public class SimpleEncryptor implements JsonEncryptor {

    /** The type of cryptographic representation that this encryptor supports. */
    public static final String TYPE = "x-simple-encryption";

    /** GCM authentication tag length, in bits. {@link SimpleDecryptor} assumes the same value. */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** GCM nonce length, in bytes, per NIST SP 800-38D recommendation. */
    private static final int GCM_NONCE_LENGTH_BYTES = 12;

    /** Generates random GCM nonces. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Converts between Java objects and JSON constructs. */
    private final ObjectMapper mapper = new ObjectMapper();

    /** The cipher to encrypt with. */
    private String cipher;

    /** The key to encrypt with. */
    private Key key;

    /** The key alias to list in the encrypted object. */
    private String alias;

    /**
     * Constructs a new simple encryptor for the specified cipher, key and alias.
     *
     * @param cipher the cipher to encrypt with.
     * @param key the key to encrypt with.
     * @param alias the key alias to list in the encrypted object.
     */
    public SimpleEncryptor(String cipher, Key key, String alias) {
        this.cipher = cipher;
        this.key = key;
        this.alias = alias;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Encrypts with a symmetric cipher.
     *
     * @param object the value to be encrypted.
     * @return the encrypted value.
     * @throws GeneralSecurityException if a cryptographic operation failed.
     * @throws IOException if an I/O exception occurred.
     */
    private Object symmetric(Object object) throws GeneralSecurityException, IOException {
        Cipher symmetric = Cipher.getInstance(cipher);
        if (isGcm(cipher)) {
            // Pin the 96-bit nonce and 128-bit tag instead of relying on the
            // provider's GCM defaults, which the JCE leaves provider-specific;
            // SimpleDecryptor assumes a 128-bit tag. The key supplied to this
            // encryptor is caller-managed and typically long-lived, so with
            // random nonces NIST SP 800-38D section 8.3 bounds GCM to at most
            // 2^32 encryptions under the same key.
            symmetric.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, randomNonce()));
        } else {
            symmetric.init(Cipher.ENCRYPT_MODE, key);
        }
        String data = Base64.encode(symmetric.doFinal(mapper.writeValueAsBytes(object)));
        byte[] iv = symmetric.getIV();
        HashMap<String, Object> result = new HashMap<>();
        result.put("cipher", this.cipher);
        result.put("key", this.alias);
        result.put("data", data);
        if (iv != null) {
            result.put("iv", Base64.encode(iv));
        }
        return result;
    }

    /**
     * Encrypts using an asymmetric cipher.
     *
     * @param object the value to be encrypted.
     * @return the encrypted value.
     * @throws GeneralSecurityException if a cryptographic operation failed.
     * @throws IOException if an I/O exception occurred.
     */
    private Object asymmetric(Object object) throws GeneralSecurityException, IOException {
        // Use GCM (authenticated encryption) with a random 96-bit nonce rather
        // than CBC or ECB. GCM provides integrity and is not vulnerable to the
        // padding-oracle attacks that affect CBC/PKCS#5, nor does it leak block
        // patterns like ECB (CWE-327). A fresh session key is generated for every
        // message, so the randomly generated nonce is never reused under the same
        // key. The nonce and tag length are passed explicitly because the JCE
        // leaves them provider-specific and SimpleDecryptor assumes a 128-bit
        // tag. The nonce is stored alongside the data and the self-describing
        // "cipher" field keeps this backward compatible with values previously
        // written using CBC or ECB.
        String symmetricCipher = "AES/GCM/NoPadding";
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        SecretKey sessionKey = generator.generateKey();
        Cipher symmetric = Cipher.getInstance(symmetricCipher);
        symmetric.init(Cipher.ENCRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, randomNonce()));
        String data = Base64.encode(symmetric.doFinal(mapper.writeValueAsBytes(object)));
        byte[] iv = symmetric.getIV();
        Cipher asymmetric = Cipher.getInstance(cipher);
        asymmetric.init(Cipher.ENCRYPT_MODE, key);
        HashMap<String, Object> keyObject = new HashMap<>();
        keyObject.put("cipher", this.cipher);
        keyObject.put("key", this.alias);
        keyObject.put("data", Base64.encode(asymmetric.doFinal(sessionKey.getEncoded())));
        HashMap<String, Object> result = new HashMap<>();
        result.put("cipher", symmetricCipher);
        result.put("key", keyObject);
        result.put("data", data);
        if (iv != null) {
            result.put("iv", Base64.encode(iv));
        }
        return result;
    }

    private static byte[] randomNonce() {
        byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTES];
        RANDOM.nextBytes(nonce);
        return nonce;
    }

    private static boolean isGcm(String cipher) {
        String[] parts = cipher.split("/");
        return parts.length > 1 && parts[1].equalsIgnoreCase("GCM");
    }

    @Override
    public JsonValue encrypt(JsonValue value) throws JsonCryptoException {
        Object object = value.getObject();
        try {
            return new JsonValue((key instanceof SecretKey ? symmetric(object) : asymmetric(object)));
        } catch (GeneralSecurityException | IOException e) {
            throw new JsonCryptoException(e);
        }
    }
}
