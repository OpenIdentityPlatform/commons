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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.selfservice.core.crypto;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.crypto.CryptoConstants.*;

import org.forgerock.json.JsonValue;

/**
 * Cryptography Service for the user self service project.
 *
 * @since 0.2.0
 */
public class CryptoService {

    /**
     * Hashes a string value. Generates a new salt value.
     *
     * @param plainTextValue
     *            the string value to be hashed.
     * @param algorithm
     *            the hashing algorithm to use.
     * @return a copy of the value, hashed with the specified algorithm and salt.
     * @throws JsonCryptoException
     *            if an exception occurred while hashing
     */
    public JsonValue hash(String plainTextValue, String algorithm) throws JsonCryptoException {
        final FieldStorageScheme fieldStorageScheme = getFieldStorageScheme(algorithm);
        final String encodedField = fieldStorageScheme.hashField(plainTextValue);
        return json(object(
                field(CRYPTO, object(
                        field(CRYPTO_VALUE, object(
                                field(CRYPTO_ALGORITHM, algorithm),
                                field(CRYPTO_DATA, encodedField))),
                        field(CRYPTO_TYPE, STORAGE_TYPE_HASH)))));
    }

    /**
     * Detects if a String is hashed.
     *
     * @param value the JSON value to check.
     * @return true if hashed, false otherwise.
     */
    public boolean isHashed(JsonValue value) {
        return value != null
                && value.isNotNull()
                && value.isDefined(CRYPTO)
                && value.get(CRYPTO).get(CRYPTO_TYPE).isString()
                && value.get(CRYPTO).isDefined(CRYPTO_VALUE)
                && value.get(CRYPTO).get(CRYPTO_VALUE).isDefined(CRYPTO_ALGORITHM)
                && value.get(CRYPTO).get(CRYPTO_VALUE).get(CRYPTO_ALGORITHM).isString();
    }

    /**
     * Returns true if the supplied plain text value of a field matches the supplied
     * hashed value.
     *
     * @param plainTextValue
     *            a {@link String} representing the plain text value of a field
     * @param value
     *            a {@link JsonValue} representing the hashed and encoded value of a field
     * @return true
     *            if the fields values match, false otherwise.
     * @throws JsonCryptoException
     *            if an exception occurred while matching
     */
    public boolean matches(String plainTextValue, JsonValue value) throws JsonCryptoException {
        JsonValue cryptoValue = value.get(CRYPTO).get(CRYPTO_VALUE);
        String algorithm = cryptoValue.get(CRYPTO_ALGORITHM).asString();
        final FieldStorageScheme fieldStorageScheme = getFieldStorageScheme(algorithm);
        return fieldStorageScheme.fieldMatches(plainTextValue, cryptoValue.get(CRYPTO_DATA).asString());
    }

    private FieldStorageScheme getFieldStorageScheme(String algorithm) throws JsonCryptoException {
        try {
            if (algorithm.equals(ALGORITHM_SHA_256)) {
                return new SaltedSHA256FieldStorageScheme();
            } else {
                throw new JsonCryptoException("Unsupported field storage algorithm " + algorithm);
            }
        } catch (JsonCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonCryptoException(e.getMessage(), e);
        }
    }

}
