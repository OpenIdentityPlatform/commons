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
 */

package org.forgerock.json.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeySelector;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Paul C. Bryan
*/
public class JsonCryptoTest {

    private static final String SYMMETRIC_CIPHER = "AES/CBC/PKCS5Padding";

    private static final String ASYMMETRIC_CIPHER = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";

    private static final String PASSWORD = "P@55W0RD";

    private static final String PLAINTEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

    private SecretKey secretKey;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    private SimpleKeySelector selector = new SimpleKeySelector() {
        @Override public Key select(String key) {
            if (key.equals("secretKey")) {
                return secretKey;
            } else if (key.equals("privateKey")) {
                return privateKey;
            } else {
                return null;
            }
        }
    };

    // ----- initialization ----------

    @BeforeClass
    public void beforeClass() throws GeneralSecurityException {

        // generate AES 128-bit secret key
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128); // the Sun JRE out of the box restricts to 128-bit key length
        secretKey = kg.generateKey();

        // generate RSA 1024-bit key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();
    }

    // ----- happy path ----------

    @Test
    public void testSymmetricEncryption() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey").encrypt(value);
        assertThat(value.getObject()).isNotEqualTo(PLAINTEXT);
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    @Test
    public void testAsymmetricEncryption() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(ASYMMETRIC_CIPHER, publicKey, "privateKey").encrypt(value);
        assertThat(value.getObject()).isNotEqualTo(PLAINTEXT);
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    @Test
    public void testJsonCryptoTransformer() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        JsonEncryptor encryptor = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey");
        JsonValue crypto = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
        ArrayList<JsonTransformer> transformers = new ArrayList<>();
        transformers.add(new JsonCryptoTransformer(new SimpleDecryptor(selector)));
        value = new JsonValue(crypto.getObject(), null, transformers);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    @Test
    public void testDeepObjectEncryption() throws JsonCryptoException {
        SimpleEncryptor encryptor = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey");
        ArrayList<JsonTransformer> transformers = new ArrayList<>();
        transformers.add(new JsonCryptoTransformer(new SimpleDecryptor(selector)));

        // encrypt a simple value
        JsonValue value = new JsonValue(PASSWORD);
        value = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
        assertThat(value.getObject()).isNotEqualTo(PASSWORD);
        assertThat(JsonCrypto.isJsonCrypto(value)).isTrue();

        Map<String, Object> inner = new HashMap<>();
        inner.put("password", value.getObject());
        value = new JsonValue(new HashMap<>());
        value.put("user", inner);
        value.put("description", PLAINTEXT);

        // decrypt the deep object
        value.getTransformers().addAll(transformers);
        value = value.copy();
        assertThat(value.get(new JsonPointer("/user/password")).getObject()).isEqualTo(PASSWORD);

        // encrypt a complex object
        value = new JsonValue(value.getObject());
        value = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
        assertThat(JsonCrypto.isJsonCrypto(value)).isTrue();

        // decrypt the deep object
        value.getTransformers().addAll(transformers);
        value.applyTransformers();
        assertThat(value.get(new JsonPointer("/user/password")).getObject()).isEqualTo(PASSWORD);
        assertThat(value.get("description").getObject()).isEqualTo(PLAINTEXT);
    }

    // ----- exceptions ----------

    @Test(expectedExceptions=JsonCryptoException.class)
    public void testDroppedIV() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey").encrypt(value);
        value.remove("iv");
        new SimpleDecryptor(selector).decrypt(value);
    }

    @Test(expectedExceptions=JsonCryptoException.class)
    public void testUnknownKey() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey").encrypt(value);
        value.put("key", "somethingCompletelyDifferent");
        new SimpleDecryptor(selector).decrypt(value);
        new SimpleDecryptor(selector).decrypt(value);
    }
}
