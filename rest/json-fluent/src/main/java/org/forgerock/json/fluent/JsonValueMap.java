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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.fluent;

// Java SE
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A map view of a JSON object.
 */
public class JsonValueMap implements JsonValueWrapper, Map<String, Object> {
    private JsonValue jsonValue;

    /**
     * Create a new map view for the provided JSON object.
     *
     * @param jsonValue
     *            The JSON object value.
     * @throws JsonValueException
     *             if the {@code jsonValue} is not a map.
     */
    public JsonValueMap(JsonValue jsonValue) {
        this.jsonValue = jsonValue.expect(Map.class);
    }

    public JsonValue unwrap() {
        return jsonValue;
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    public int size() {
        return jsonValue.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return (jsonValue.size() == 0);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified key.
     */
    public boolean containsKey(Object key) {
        return (key != null && key instanceof String && jsonValue.isDefined((String)key));
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the specified value.
     */
    public boolean containsValue(Object value) {
        return jsonValue.contains(value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map
     * contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null}.
     */
    public Object get(Object key) {
        Object result = null;
        if (key != null && key instanceof String) {
            result = jsonValue.get((String)key).getWrappedObject();
        }
        return result;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return the previous value associated with key, or {@code null} if there was no mapping for key.
     */
    public Object put(String key, Object value) {
        Object result = get(key);
        jsonValue.put(key, value);
        return result;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return the previous value associated with key, or {@code null} if there was no mapping for key.
     */
    public Object remove(Object key) {
        Object result = get(key);
        if (key instanceof String) {
            jsonValue.remove((String)key);
        }
        return result;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @param m mappings to be stored in this map.
     */
    public void putAll(Map<? extends String, ? extends Object> m) {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
            jsonValue.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        jsonValue.clear();
    }

    /**
     * Returns a {@code Set} view of the keys contained in this map.
     */
    public Set<String> keySet() {
        return jsonValue.keys();
    }

    /**
     * Returns a Collection view of the values contained in this map.
     */
    public Collection<Object> values() {
        ArrayList<Object> result = new ArrayList<Object>(size());
        for (JsonValue jv : jsonValue) {
            result.add(jv.getObject());
        }
        return result;
    }

    /**
     * Returns a {@code Set} view of the mappings contained in this map.
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        HashSet<Map.Entry<String, Object>> result = new HashSet<Map.Entry<String, Object>>(size());
        for (String key : jsonValue.keys()) {
            result.add(new AbstractMap.SimpleEntry<String, Object>(key, get(key)));
        }
        return result;
    }

    /**
     * Compares the specified object with this map for equality.
     *
     * @param o object to be compared for equality with this map.
     * @return {@code true} if the specified object is equal to this map.
     */
    @Override
    public boolean equals(Object o) {
        return jsonValue.getObject().equals(o);
    }

    /**
     * Returns the hash code value for this map.
     */
    @Override
    public int hashCode() {
        return jsonValue.getObject().hashCode();
    }
}
