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

package org.forgerock.audit.providers;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.audit.secure.KeyStoreHandler;

/**
 * Default implementation of {@link KeyStoreHandlerProvider}.
 * <p>
 * Multiple KeyStoreHandler instances can be provided, identified by a name.
 */
public class DefaultKeyStoreHandlerProvider implements KeyStoreHandlerProvider {

    /** Named secure storage instances availables. */
    private final Map<String, KeyStoreHandler> keyStoreHandlers;

    /** Creates a initially empty provider. */
    public DefaultKeyStoreHandlerProvider() {
        this.keyStoreHandlers = new HashMap<>();
    }

    /**
     * Creates a provider with some {@link KeystoreHandler} instances.
     *
     * @param handlers
     *          The storage instances to use in the provider.
     */
    public DefaultKeyStoreHandlerProvider(Map<String, KeyStoreHandler> handlers) {
        this.keyStoreHandlers = handlers;
    }

    /**
     * Register a storage with the given name.
     *
     * @param name
     *          Name associated to the {@link KeystoreHandler} instance.
     * @param handler
     *          The storage instance.
     */
    public void registerKeyStoreHandler(String name, KeyStoreHandler handler) {
        keyStoreHandlers.put(name, handler);
    }

    @Override
    public KeyStoreHandler getKeystoreHandler(String name) {
        return keyStoreHandlers.get(name);
    }

}
