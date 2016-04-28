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

import org.forgerock.json.JsonValue;

/**
 * Decrypts an encrypted JSON value.
 */
public interface JsonDecryptor {

    /**
     * Returns the type of cryptographic representation that this JSON decryptor supports.
     * Expressed in the {@code type} property of a {@link JsonCrypto} object.
     * @return The type.
     */
    String getType();

    /**
     * Decrypts the specified value.
     *
     * @param value the JSON value to be decrypted.
     * @return the decrypted value.
     * @throws JsonCryptoException if the decryptor fails to decrypt the value.
     */
    JsonValue decrypt(JsonValue value) throws JsonCryptoException;
}
