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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.util;

// Java SE
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Wraps another map. All methods simply call through to the wrapped map.
 * Subclasses are expected to override one or more of these methods.
 *
 * @param <K>
 *            The type of key.
 * @param <V>
 *            The type of value.
 */
public class MapDecorator<K, V> implements Map<K, V> {

    /** The map wrapped by this decorator. */
    protected final Map<K, V> map;

    /**
     * Constructs a new map decorator, wrapping the specified map.
     *
     * @param map
     *            the map to wrap with the decorator.
     * @throws NullPointerException
     *             if {@code map} is {@code null}.
     */
    public MapDecorator(Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        this.map = map;
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if the map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key
     *            the key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified
     *         key.
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if the map maps one or more keys to the specified
     * value.
     *
     * @param value
     *            the value whose presence in the map is to be tested.
     * @return {@code true} if the map maps one or more keys to the specified
     *         value.
     */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if the map contains no mapping for the key.
     *
     * @param key
     *            the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null}
     *         if no mapping.
     */
    @Override
    public V get(Object key) {
        return map.get(key);
    }

    /**
     * Associates the specified value with the specified key in the map.
     *
     * @param key
     *            key with which the specified value is to be associated.
     * @param value
     *            value to be associated with the specified key.
     * @return the previous value associated with key, or {@code null} if no
     *         mapping.
     */
    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    /**
     * Removes the mapping for a key from the map if it is present.
     *
     * @param key
     *            key whose mapping is to be removed from the map.
     * @return the previous value associated with key, or {@code null} if no
     *         mapping.
     */
    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    /**
     * Copies all of the mappings from the specified map to the map.
     *
     * @param m
     *            mappings to be stored in the map.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    /**
     * Removes all of the mappings from the map.
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in the map.
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in the map.
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in the map.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns the hash code value for the map.
     */
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Compares the specified object with the map for equality.
     *
     * @param o
     *            object to be compared for equality with the map.
     * @return {@code true} if the specified object is equal to the map.
     */
    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }
}
