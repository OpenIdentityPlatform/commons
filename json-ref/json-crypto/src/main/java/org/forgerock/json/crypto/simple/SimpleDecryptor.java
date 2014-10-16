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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.crypto.simple;

// Java Standard Edition
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


// Apache Commons Codec
import org.apache.commons.codec.binary.Base64;

// Jackson
import com.fasterxml.jackson.databind.ObjectMapper;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// JSON Crypto
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonDecryptor;


/**
 * Decrypts a {@code $crypto} JSON object value encrypted with the
 * {@code x-simple-encryption} type.
 *
 * @author Paul C. Bryan
 */
public class SimpleDecryptor implements JsonDecryptor {

    /** The type of cryptographic representation that this decryptor supports. */
    public static final String TYPE = "x-simple-encryption";

    /** Converts between JSON constructs and Java objects. */
    private final ObjectMapper mapper = new ObjectMapper();

    /** TODO: Description. */
    private final SimpleKeySelector selector;

    /**
     * TODO: Description.
     *
     * @param selector TODO.
     */
    public SimpleDecryptor(SimpleKeySelector selector) {
        this.selector = selector;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    private Key select(String alias) throws JsonCryptoException {
        Key result = selector.select(alias);
        if (result == null) {
            throw new JsonCryptoException("key not found: " + alias);
        }
        return result;
    }

    @Override
    public JsonValue decrypt(JsonValue value) throws JsonCryptoException {
        try {
            JsonValue key = value.get("key").required();
            String cipher = value.get("cipher").required().asString();
            Key symmetricKey;
            if (key.isString()) {
                symmetricKey = select(key.asString());
            } else {
                Key privateKey = select(key.get("key").required().asString());
                Cipher asymmetric = Cipher.getInstance(key.get("cipher").required().asString());
                asymmetric.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] ciphertext = Base64.decodeBase64(key.get("data").required().asString());
                symmetricKey = new SecretKeySpec(asymmetric.doFinal(ciphertext), cipher.split("/", 2)[0]);
            }
            Cipher symmetric = Cipher.getInstance(cipher);
            String iv = value.get("iv").asString();
            IvParameterSpec ivps = (iv == null ? null : new IvParameterSpec(Base64.decodeBase64(iv)));
            symmetric.init(Cipher.DECRYPT_MODE, symmetricKey, ivps);
            byte[] plaintext = symmetric.doFinal(Base64.decodeBase64(value.get("data").required().asString()));
            return new JsonValue(mapper.readValue(plaintext, Object.class));
        } catch (GeneralSecurityException gse) { // Java Cryptography Extension
            throw new JsonCryptoException(gse);
        } catch (IOException ioe) { // Jackson
            throw new JsonCryptoException(ioe);
        } catch (JsonValueException jne) { // JSON Fluent
            throw new JsonCryptoException(jne);
        }
    }

}
