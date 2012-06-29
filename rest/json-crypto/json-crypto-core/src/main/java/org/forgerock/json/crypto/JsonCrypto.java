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

package org.forgerock.json.crypto;

// Java Standard Edition
import java.util.HashMap;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * Represents a JSON {@code $crypto} object.
 *
 * @author Paul C. Bryan
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
     */
    public static boolean isJsonCrypto(JsonValue value) {
        boolean result = false;
        if (value.isDefined("$crypto")) { // avoid transformer endless loops
            JsonValue crypto = value.get("$crypto");
            result = (crypto.get("type").isString() && crypto.isDefined("value"));
        }
        return result;
    }

    /**
     * Returns the type of JSON cryptographic representation.
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
        JsonValue crypto = value.get("$crypto").required();
        this.type = crypto.get("type").required().asString();
        this.value = crypto.get("value").required();
    }

    /**
     * Returns this object as a {@code $crypto} JSON object value.
     */
    public JsonValue toJsonValue() {
        HashMap<String, Object> crypto = new HashMap<String, Object>();
        crypto.put("type", type);
        crypto.put("value", value == null ? null : value.getObject());
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("$crypto", crypto);
        return new JsonValue(result);
    }
}
