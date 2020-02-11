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

package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;

/**
 * Process store is used to persist state throughout a given flow cycle.
 *
 * @since 0.1.0
 */
public interface ProcessStore {

    /**
     * Adds the state into the store against the key.
     *
     * @param key
     *         key used to index the state
     * @param state
     *         the state
     */
    void add(String key, JsonValue state);

    /**
     * Removes the state out of the store.
     *
     * @param key
     *         key used to index the state
     *
     * @return the state
     */
    JsonValue remove(String key);

}
