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
 * Copyright 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2020-2026 3A Systems, LLC
 */

package org.forgerock.json.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
import org.forgerock.json.JsonPointer;
//import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JsonCryptoTest {

    private static final String SYMMETRIC_CIPHER = "AES/CBC/PKCS5Padding";

    private static final String SYMMETRIC_GCM_CIPHER = "AES/GCM/NoPadding";

    private static final String ASYMMETRIC_CIPHER = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";

    private static final String PASSWORD = "P@55W0RD";

    private static final String PLAINTEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

    /**
     * Fixed AES key for the checked-in legacy values below; unlike the random
     * per-run keys, it lets ciphertexts produced by pre-GCM builds be kept in
     * the test as backward-compatibility fixtures.
     */
    private static final SecretKey COMPAT_KEY =
            new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.US_ASCII), "AES");

    /** {@link #PLAINTEXT} encrypted with {@link #COMPAT_KEY} using legacy AES/ECB/PKCS5Padding (no IV). */
    private static final String LEGACY_ECB_DATA =
            "8sDxsiLqBe63ul0Pup+Bv3CREJeNI+bpMydH8hnajinHixZAmDwzoNGBDpyFApurpvwOZa6bF153jD2Rk0Jwfg==";

    /** {@link #PLAINTEXT} encrypted with {@link #COMPAT_KEY} and {@link #LEGACY_CBC_IV} using AES/CBC/PKCS5Padding. */
    private static final String LEGACY_CBC_DATA =
            "kgxfhkMPxKtmhfmxQt/p8bCj4bIZPH1s/AwAmVANAAuYLGRosaLsNrfclIosTJYKWAAGygDF/dkzs+2ye2+1tw==";

    private static final String LEGACY_CBC_IV = "ZmVkY2JhOTg3NjU0MzIxMA==";

    private SecretKey secretKey;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    private SimpleKeySelector selector = new SimpleKeySelector() {
        @Override public Key select(String key) {
            if (key.equals("secretKey")) {
                return secretKey;
            } else if (key.equals("privateKey")) {
                return privateKey;
            } else if (key.equals("compatKey")) {
                return COMPAT_KEY;
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

        // generate RSA 2048-bit key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
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
        assertThat(((Map<?, ?>) value.getObject()).get("cipher")).isEqualTo(SYMMETRIC_GCM_CIPHER);
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    @Test
    public void testSymmetricGcmEncryption() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_GCM_CIPHER, secretKey, "secretKey").encrypt(value);
        assertThat(value.getObject()).isNotEqualTo(PLAINTEXT);
        assertThat(((Map<?, ?>) value.getObject()).get("cipher")).isEqualTo(SYMMETRIC_GCM_CIPHER);
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    // ----- backward compatibility ----------

    @Test
    public void testDecryptLegacyEcbValue() throws JsonCryptoException {
        JsonValue value = new JsonValue(legacyValue("AES/ECB/PKCS5Padding", LEGACY_ECB_DATA, null));
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    @Test
    public void testDecryptLegacyCbcValue() throws JsonCryptoException {
        JsonValue value = new JsonValue(legacyValue(SYMMETRIC_CIPHER, LEGACY_CBC_DATA, LEGACY_CBC_IV));
        value = new SimpleDecryptor(selector).decrypt(value);
        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
    }

    private static Map<String, Object> legacyValue(String cipher, String data, String iv) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("cipher", cipher);
        result.put("key", "compatKey");
        result.put("data", data);
        if (iv != null) {
            result.put("iv", iv);
        }
        return result;
    }

//    @Test
//    public void testJsonCryptoTransformer() throws JsonCryptoException {
//        JsonValue value = new JsonValue(PLAINTEXT);
//        JsonEncryptor encryptor = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey");
//        JsonValue crypto = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
//        ArrayList<JsonTransformer> transformers = new ArrayList<>();
//        transformers.add(new JsonCryptoTransformer(new SimpleDecryptor(selector)));
//        value = new JsonValue(crypto.getObject(), null, transformers);
//        assertThat(value.getObject()).isEqualTo(PLAINTEXT);
//    }

//    @Test
//    public void testDeepObjectEncryption() throws JsonCryptoException {
//        SimpleEncryptor encryptor = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey");
//        ArrayList<JsonTransformer> transformers = new ArrayList<>();
//        transformers.add(new JsonCryptoTransformer(new SimpleDecryptor(selector)));
//
//        // encrypt a simple value
//        JsonValue value = new JsonValue(PASSWORD);
//        value = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
//        assertThat(value.getObject()).isNotEqualTo(PASSWORD);
//        assertThat(JsonCrypto.isJsonCrypto(value)).isTrue();
//
//        Map<String, Object> inner = new HashMap<>();
//        inner.put("password", value.getObject());
//        value = new JsonValue(new HashMap<>());
//        value.put("user", inner);
//        value.put("description", PLAINTEXT);
//
//        // decrypt the deep object
//        value.getTransformers().addAll(transformers);
//        value = value.copy();
//        assertThat(value.get(new JsonPointer("/user/password")).getObject()).isEqualTo(PASSWORD);
//
//        // encrypt a complex object
//        value = new JsonValue(value.getObject());
//        value = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
//        assertThat(JsonCrypto.isJsonCrypto(value)).isTrue();
//
//        // decrypt the deep object
//        value.getTransformers().addAll(transformers);
//        value.applyTransformers();
//        assertThat(value.get(new JsonPointer("/user/password")).getObject()).isEqualTo(PASSWORD);
//        assertThat(value.get("description").getObject()).isEqualTo(PLAINTEXT);
//    }

    // ----- exceptions ----------

    @Test(expectedExceptions = JsonCryptoException.class)
    public void testDroppedIV() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey").encrypt(value);
        value.remove("iv");
        new SimpleDecryptor(selector).decrypt(value);
    }

    @Test(expectedExceptions = JsonCryptoException.class)
    public void testUnknownKey() throws JsonCryptoException {
        JsonValue value = new JsonValue(PLAINTEXT);
        value = new SimpleEncryptor(SYMMETRIC_CIPHER, secretKey, "secretKey").encrypt(value);
        value.put("key", "somethingCompletelyDifferent");
        new SimpleDecryptor(selector).decrypt(value);
        new SimpleDecryptor(selector).decrypt(value);
    }
}
