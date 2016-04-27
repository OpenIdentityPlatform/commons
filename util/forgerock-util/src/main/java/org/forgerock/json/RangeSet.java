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
 * Copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.json;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Exposes a range of integer values as a set. Used to expose a set of values without
 * requiring the allocation of storage for all values.
 */
class RangeSet extends AbstractSet<String> implements Set<String>, Cloneable, Serializable {

    /** Establishes serialized object compatibility. */
    static final long serialVersionUID = 1L;

    /** The start of the range, inclusive. */
    private int start;

    /** The end of the range, inclusive. */
    private int end;

    /**
     * Constructs a range set for the specified range.
     *
     * @param start the start of the range, inclusive.
     * @param end the end of the range, inclusive.
     */
    public RangeSet(int start, int end) {
        this.start = start;
        this.end = end;
        if (start > end) {
            throw new IllegalArgumentException("start must be <= end");
        }
    }

    /**
     * Returns an iterator over the elements in this set.
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            int cursor = start;
            @Override
            public boolean hasNext() {
                return cursor <= end;
            }
            @Override
            public String next() {
                if (cursor > end) {
                    throw new NoSuchElementException();
                }
                return Integer.toString(cursor++);
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the number of elements in this set.
     */
    @Override
    public int size() {
        return end - start + 1;
    }

    /**
     * Returns {@code false} unconditionally. Range sets always have at least one element.
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     */
    @Override
    public boolean contains(Object o) {
        boolean result = false;
        if (o != null && o instanceof String) {
            try {
                int n = Integer.parseInt((String) o);
                result = (n >= start && n <= end);
            } catch (NumberFormatException nfe) {
                // result remains false
            }
        }
        return result;
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException}, as range sets are
     * immutable.
     */
    @Override
    public boolean add(String e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException}, as range sets are
     * immutable.
     */
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unconditionally throws {@link UnsupportedOperationException}, as range sets are
     * immutable.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
