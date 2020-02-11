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

package org.forgerock.selfservice.example;

import org.forgerock.json.JsonValue;
import org.forgerock.selfservice.core.ProcessStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple local stores in memory.
 *
 * @since 0.1.0
 */
public final class SimpleInMemoryStore implements ProcessStore {

    private final Map<String, JsonValue> localStore;

    /**
     * Creates a new basic local storage instance.
     */
    public SimpleInMemoryStore() {
        localStore = new ConcurrentHashMap<>();
    }

    @Override
    public JsonValue remove(String key) {
        return localStore.remove(key);
    }

    @Override
    public void add(String key, JsonValue state) {
        localStore.put(key, state);
    }

}
