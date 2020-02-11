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
package org.forgerock.audit.handlers.csv;

import static org.forgerock.util.Reject.checkNotNull;

import java.security.KeyStore;

import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Utils;

/**
 * In memory implementation of {@link KeyStoreHandler}
 */
public class MemoryKeyStoreHandler implements KeyStoreHandler {

    private KeyStore keyStore;
    private final String password = "password";

    public MemoryKeyStoreHandler() {
        this.keyStore = new KeyStoreBuilder()
                .withKeyStoreType(Utils.asEnum(CsvSecureConstants.KEYSTORE_TYPE, KeyStoreType.class))
                .withPassword(password)
                .withInputStream(null)
                .build();
    }

    @Override
    public KeyStore getStore() {
        return keyStore;
    }

    @Override
    public void setStore(KeyStore keystore) throws Exception {
        this.keyStore = checkNotNull(keystore);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getType() {
        return keyStore.getType();
    }

    @Override
    public void store() throws Exception {
        // Do nothing as we are in memory.
    }

}
