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

package org.forgerock.caf.authentication.framework;

import javax.security.auth.message.MessageInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides methods that allow working with MessageInfo maps to be simplified.
 *
 * @since 1.3.0
 */
public class MessageInfoUtils {

    /**
     * <p>Removes the map with the given key from the root map in the MessageInfo.</p>
     *
     * <p>If the map does not exist in the root map of the MessageInfo, then {@code null} will be returned.</p>
     *
     * @param messageInfo The MessageInfo to remove the map from.
     * @param mapKey The key for the map.
     * @return The map that is contained in the MessageInfo map with the given key or {@code null} if it does not exist.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> removeMap(MessageInfo messageInfo, String mapKey) {
        return (Map<String, Object>) messageInfo.getMap().remove(mapKey);
    }

    /**
     * Gets the map with the given key from the root map in the MessageInfo.
     * <br/>
     * If the map does not exist in the root map of the MessageInfo, the a new {@code <String, Object>} map will
     * be created an inserted into it, with the given key.
     *
     * @param messageInfo The MessageInfo to get the map from.
     * @param mapKey The key for the map.
     * @return The map that is contained in the MessageInfo map with the given key.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(final MessageInfo messageInfo, final String mapKey) {
        return getMap(messageInfo.getMap(), mapKey);
    }

    /**
     * Gets the map with the given key from the given containing map.
     * <br/>
     * If the map does not exist, a new {@code <String, Object>} map will be created and inserted into the given
     * containing map with the given key.
     *
     * @param containingMap The map that will contain the map at the given key.
     * @param mapKey The key for the map.
     * @return The map that is contained in the given map with the given key.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(final Map<String, Object> containingMap, final String mapKey) {
        Map<String, Object> map = (Map<String, Object>) containingMap.get(mapKey);
        if (map == null) {
            map = new HashMap<String, Object>();
            containingMap.put(mapKey, map);
        }

        return map;
    }

    /**
     * Adds a new empty {@code <String, Object>} map in the root map in the MessageInfo.
     *
     * @param messageInfo The MessageInfo to add the map to.
     * @param key The key to use for the new map.
     */
    public void addMap(final MessageInfo messageInfo, final String key) {
        getMap(messageInfo, key);
    }
}
