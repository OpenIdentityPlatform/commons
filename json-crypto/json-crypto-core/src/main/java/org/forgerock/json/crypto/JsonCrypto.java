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
 */

package org.forgerock.json.crypto;

import static org.forgerock.util.crypto.CryptoConstants.*;

import java.util.HashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.util.crypto.CryptoConstants;

/**
 * Represents a JSON {@code $crypto} object.
 *
 * For example:
 * <pre>
 * "$crypto":{
 *     "value":{
 *         "data":"wfoQJXXXXTa551pKTMjZ/Q==",
 *         "cipher":"AES/CBC/PKCS5Padding",
 *         "iv":"OXHdtVBURv6fAuRa88CDnA==",
 *         "key":"openidm-sym-default"
 *     },
 *     "type":"x-simple-encryption"
 * }
 * </pre>
 *
 * @see CryptoConstants for constants used to build the json.
 */
public class JsonCrypto {

    /** The type of JSON cryptographic representation. */
    private String type;

    /** The JSON cryptographic value. */
    private JsonValue value;

    /**
     * Constructs an empty JSON cryptographic object.
     */
    public JsonCrypto() {
        // empty
    }

    /**
     * Constructs a new JSON cryptographic object, initializing from a JSON value.
     *
     * @param value a JSON value containing a {@code $crypto} JSON object value.
     * @throws JsonValueException if the specified value is malformed.
     */
    public JsonCrypto(JsonValue value) throws JsonValueException {
        fromJsonValue(value);
    }

    /**
     * Constructs a new JSON cryptographic object, initializing with the specified type
     * and cryptographic value.
     *
     * @param type the type of JSON cryptographic representation.
     * @param value the JSON cryptographic value.
     */
    public JsonCrypto(String type, JsonValue value) {
        setType(type);
        setValue(value);
    }

    /**
     * Returns {@code true} if the specified JSON value contains a valid {@code $crypto}
     * JSON object structure.
     * <p>
     * Note: This method does not suppress transformers in the specified value. Consequently,
     * this method can return {@code false} if members are transformed, for example if a
     * {@link JsonCryptoTransformer} transforms the value as it is being inspected.
     *
     * @param value The JSON to check.
     * @return The result.
     */
    public static boolean isJsonCrypto(JsonValue value) {
        boolean result = false;
        if (value.isDefined(CRYPTO)) { // avoid transformer endless loops
            JsonValue crypto = value.get(CRYPTO);
            result = (crypto.get(CRYPTO_TYPE).isString() && crypto.isDefined(CRYPTO_VALUE));
        }
        return result;
    }

    /**
     * Returns the type of JSON cryptographic representation.
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of JSON cryptographic representation.
     *
     * @param type the type of JSON cryptographic representation.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the JSON cryptographic value.
     * @return The value.
     */
    public JsonValue getValue() {
        return value;
    }

    /**
     * Sets the JSON cryptographic value.
     *
     * @param value the JSON cryptographic value.
     */
    public void setValue(JsonValue value) {
        this.value = value;
    }

    /**
     * Initializes this object from the specified {@code $crypto} JSON object value.
     *
     * @param value a JSON value containing a {@code $crypto} JSON object value.
     * @throws JsonValueException if the specified value is malformed.
     */
    public void fromJsonValue(JsonValue value) throws JsonValueException {
        JsonValue crypto = value.get(CRYPTO).required();
        this.type = crypto.get(CRYPTO_TYPE).required().asString();
        this.value = crypto.get(CRYPTO_VALUE).required();
    }

    /**
     * Returns this object as a {@code $crypto} JSON object value.
     * @return The value.
     */
    public JsonValue toJsonValue() {
        HashMap<String, Object> crypto = new HashMap<>();
        crypto.put(CRYPTO_TYPE, type);
        crypto.put(CRYPTO_VALUE, value == null
                ? null
                : value.getObject());
        HashMap<String, Object> result = new HashMap<>();
        result.put(CRYPTO, crypto);
        return new JsonValue(result);
    }
}
