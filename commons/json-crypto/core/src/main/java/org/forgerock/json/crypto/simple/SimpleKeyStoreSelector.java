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

package org.forgerock.json.crypto.simple;

// Java Standard Edition
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

// JSON Crypto
import org.forgerock.json.crypto.JsonCryptoException;

/**
 * Simple implementation for selecting keys from a provided key store.
 */
public class SimpleKeyStoreSelector implements SimpleKeySelector {

    /** Key store to select keys from. */
    private KeyStore keyStore;

    /** Password to retrieve keys with. */
    private char[] password;

    /**
     * Constructs a simple key store selector.
     *
     * @param keyStore the key store to select keys from.
     * @param password the password to use to decrypt selected keys.
     */
    public SimpleKeyStoreSelector(KeyStore keyStore, String password) {
        this.keyStore = keyStore;
        this.password = password.toCharArray();
    }

    @Override
    public Key select(String key) throws JsonCryptoException {
        try {
            return keyStore.getKey(key, password);
        } catch (GeneralSecurityException gse) {
            throw new JsonCryptoException(gse);
        }
    }
}
