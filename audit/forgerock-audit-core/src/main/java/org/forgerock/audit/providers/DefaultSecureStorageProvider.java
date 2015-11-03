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

import org.forgerock.audit.secure.SecureStorage;

/**
 * Default implementation of {@link SecureStorageProvider}.
 * <p>
 * Multiple SecureStorage instances can be provided, identified by a name.
 */
public class DefaultSecureStorageProvider implements SecureStorageProvider {

    /** Named secure storage instances availables. */
    private final Map<String, SecureStorage> secureStorages;

    /** Creates a initially empty provider. */
    public DefaultSecureStorageProvider() {
        this.secureStorages = new HashMap<>();
    }

    /**
     * Creates a provider with some storages instances.
     *
     * @param storages
     *          The storage instances to use in the provider.
     */
    public DefaultSecureStorageProvider(Map<String, SecureStorage> storages) {
        this.secureStorages = storages;
    }

    /**
     * Register a storage with the given name.
     *
     * @param name
     *          Name associated to the storage instance.
     * @param storage
     *          The storage instance.
     */
    public void registerSecureStorage(String name, SecureStorage storage) {
        secureStorages.put(name, storage);
    }

    @Override
    public SecureStorage getSecureStorage(String name) {
        return secureStorages.get(name);
    }

}
