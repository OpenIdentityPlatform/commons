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
 * A map with lazy initialization. The factory is called to initialize the map
 * on the first call to one of this object's methods.
 *
 * @param <K>
 *            The type of key.
 * @param <V>
 *            The type of value.
 */
public class LazyMap<K, V> implements Map<K, V> {

    /** The map that this lazy map exposes, once initialized. */
    private Map<K, V> map;

    /** Factory to create the instance of the map to expose. */
    protected Factory<Map<K, V>> factory;

    /**
     * Constructs a new lazy map. Allows factory to be set in subclass
     * constructor.
     */
    protected LazyMap() {
    }

    /**
     * Constructs a new lazy map.
     *
     * @param factory
     *            factory to create the map instance to expose.
     */
    public LazyMap(Factory<Map<K, V>> factory) {
        this.factory = factory;
    }

    /**
     * Performs lazy initialization of the map if not already performed, and
     * returns the initialized map.
     */
    private Map<K, V> lazy() {
        if (map == null) {
            synchronized (this) {
                if (map == null) {
                    map = factory.newInstance();
                }
            }
        }
        return map;
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return lazy().size();
    }

    /**
     * Returns {@code true} if the map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        return lazy().isEmpty();
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
        return lazy().containsKey(key);
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
        return lazy().containsValue(value);
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
        return lazy().get(key);
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
        return lazy().put(key, value);
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
        return lazy().remove(key);
    }

    /**
     * Copies all of the mappings from the specified map to the map.
     *
     * @param m
     *            mappings to be stored in the map.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        lazy().putAll(m);
    }

    /**
     * Removes all of the mappings from the map.
     */
    @Override
    public void clear() {
        lazy().clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in the map.
     */
    @Override
    public Set<K> keySet() {
        return lazy().keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in the map.
     */
    @Override
    public Collection<V> values() {
        return lazy().values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in the map.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return lazy().entrySet();
    }

    /**
     * Returns the hash code value for the map.
     */
    @Override
    public int hashCode() {
        return lazy().hashCode();
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
        return lazy().equals(o);
    }
}
