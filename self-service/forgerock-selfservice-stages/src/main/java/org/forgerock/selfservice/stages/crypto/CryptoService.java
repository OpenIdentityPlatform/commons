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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.crypto;

import static org.forgerock.json.JsonValue.*;

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
                field("$crypto", object(
                        field("value", object(
                                field("algorithm", algorithm),
                                field("data", encodedField))),
                        field("type", CryptoConstants.STORAGE_TYPE_HASH)))));
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
        JsonValue cryptoValue = value.get("$crypto").get("value");
        String algorithm = cryptoValue.get("algorithm").asString();
        final FieldStorageScheme fieldStorageScheme = getFieldStorageScheme(algorithm);
        return fieldStorageScheme.fieldMatches(plainTextValue, cryptoValue.get("data").asString());
    }

    private FieldStorageScheme getFieldStorageScheme(String algorithm) throws JsonCryptoException {
        try {
            if (algorithm.equals(CryptoConstants.ALGORITHM_SHA_256)) {
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
