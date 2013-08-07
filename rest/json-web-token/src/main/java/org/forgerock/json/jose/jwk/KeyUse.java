/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.json.jose.jwk;

import org.forgerock.json.fluent.JsonException;

/**
 * Represents the Possible KeyUse values.
 */
public enum KeyUse {
    /**
     * Use Key as a signature key.
     */
    SIG,

    /**
     * Use Key as a encryption key.
     */
    ENC;

    /**
     * Construct a KeyUse.
     */
    private KeyUse() {

    }

    /**
     * Get the Value of the KeyUse.
     * @return the KeyUse value.
     */
    public String value() {
        return toString();
    }

    /**
     * Get the KeyUse.
     * @param keyUse the string representing the KeyUse to get
     * @return a KeyUse, or null if keyUse is null or empty
     */
    public static KeyUse getKeyUse(String keyUse) {
        if (keyUse == null || keyUse.isEmpty()) {
            return null;
        }
        try {
            return KeyUse.valueOf(keyUse.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonException("Invalid key use");
        }
    }

    /**
     * Prints the KeyUse value.
     * @return the KeyUse in lowercase
     */
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
