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
import java.security.Key;

// JSON Crypto library
import org.forgerock.json.crypto.JsonCryptoException;

/**
 * Interface to select keys from a key store.
 */
public interface SimpleKeySelector {

    /**
     * Selects a key for the specified key alias.
     *
     * @param key the alias of the key to select.
     * @return the selected key.
     * @throws JsonCryptoException if the specified key could not be selected.
     */
    Key select(String key) throws JsonCryptoException;
}
