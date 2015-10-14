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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.jose.jwk;

import org.forgerock.json.JsonException;

/**
 * Enum representing the possible KeyTypes.
 */
public enum KeyType {
    /**
     * RSA key.
     */
    RSA("RSA"),

    /**
     * Elliptical Curve Key.
     */
    EC("EC"),

    /**
     * Octet Key.
     */
    OCT("oct");

    /**
     * The value of the KeyType.
     */
    private String value = null;

    /**
     * Construct a KeyType.
     * @param value value to give that keytype
     */
    private KeyType(String value) {
        this.value = value;
    }

    /**
     * Get the value of the KeyType.
     * @return the value of the KeyType
     */
    public String value() {
        return toString();
    }

    /**
     * Get the KeyType given a string.
     * @param keyType string representing the KeyType
     * @return a KeyType or null if given null KeyType
     */
    public static KeyType getKeyType(String keyType) {
        if (keyType == null || keyType.isEmpty()) {
            return null;
        }
        try {
            return KeyType.valueOf(keyType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonException("Invalid key type");
        }
    }

    /**
     * Gets the value of the KeyType.
     * @return value of the KeyType
     */
    @Override
    public String toString() {
        return value;
    }
}
