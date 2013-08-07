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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A sub-implementation of the Java HashMap which does not allow duplicate entries to be added.
 * <p>
 * This HashMap sub-implementation behaves the same as a HashMap except for the fact that any entry the is added into
 * the map and the map already has an entry with the same given key, a DuplicateMapEntryException will be thrown.
 *
 * @param <K> {@inheritDoc}
 * @param <V> {@inheritDoc}
 *
 * @author Phill Cunnington
 * @since 2.0.0
 * @see Map
 * @see HashMap
 */
public class NoDuplicatesMap<K, V> extends HashMap<K, V> {

    /** Serializable class version number. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an empty <tt>NoDuplicatesMap</tt> with the default initial capacity (16) and the default
     * load factor (0.75).
     * @see java.util.HashMap#HashMap()
     */
    public NoDuplicatesMap() {
        super();
    }

    /**
     * Constructs an empty <tt>NoDuplicatesMap</tt> with the specified initial capacity and the default
     * load factor (0.75).
     *
     * @param initialCapacity The initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     * @see java.util.HashMap#HashMap(int)
     */
    public NoDuplicatesMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs an empty <tt>NoDuplicatesMap</tt> with the specified initial capacity and load factor.
     *
     * @param initialCapacity The initial capacity.
     * @param loadFactor The load factor.
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor is non-positive.
     * @see java.util.HashMap#HashMap(int, float)
     */
    public NoDuplicatesMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new <tt>NoDuplicatesMap</tt> with the same mappings as the specified <tt>Map</tt>.
     * The <tt>NoDuplicatesMap</tt> is created with default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param map The map whose mappings are to be placed in this map.
     * @throws NullPointerException if the specified map is null.
     * @see java.util.HashMap#HashMap(java.util.Map)
     */
    public NoDuplicatesMap(Map<? extends K, ? extends V> map) {
        super(map);
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * the key, then a DuplicateMapEntryException is thrown.
     *
     * @param key {@inheritDoc}
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     * @throws DuplicateMapEntryException if a
     * @see java.util.HashMap#put(Object, Object)
     */
    @Override
    public V put(K key, V value) {
        if (containsKey(key)) {
            throw new DuplicateMapEntryException("Attempting to add entry: " + key + "=" + value
                    + ", when entry already exists: " + key + "=" + get(key));
        }
        return super.put(key, value);
    }

    /**
     * Copies all of the mappings from the specified map to this map. If this map previously contained a mapping for
     * the key, then a DuplicateMapEntryException is thrown.
     *
     * @param m {@inheritDoc}
     * @throws NullPointerException if the specified map is null
     * @see java.util.HashMap#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
