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
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Exposes a range of integer values as a set. Avoids the allocation of storage for all
 * values. Order of elements is guaranteed to be in the order from the specified start and
 * stop values.
 * <p>
 * If combination of start/stop/step values are not mathematically possible to represent
 * as a set of values, it is represented by this implementation as an empty set.
 */
public class RangeSet extends AbstractSet<Integer> implements Cloneable, Serializable {

    /** Establishes serialized object compatibility. */
    private static final long serialVersionUID = 1L;

    /** The start of the range, inclusive. */
    private final int start;

    /** The end of the range, inclusive. */
    private final int stop;

    /** TODO: Description. */
    private final int step;

    /**
     * Constructs a range set for a sequence of numbers, starting at {@code 0} with
     * the value to stop.  Equivalent to constructing the range set with:
     * {@code RangeSet(0, stop, 1)}.
     * @param stop the point at which to stop the range (exclusive).
     */
    public RangeSet(int stop) {
        this(0, stop, 1);
    }

    /**
     * Constructs a range set for the specified range of integers with a step of {@code 1}.
     * Equivalent to constructing the range set with: {@code RangeSet(start, stop, 1)}.
     *
     * @param start the start of the range (inclusive).
     * @param stop the point at which to stop the range (exclusive).
     */
    public RangeSet(int start, int stop) {
        this(start, stop, 1);
    }

    /**
     * Constructs a range set for the specified range of integers and increment.
     *
     * @param start the start of the range, inclusive.
     * @param stop the point at which to stop the range (exclusive).
     * @param step the step to increment for each value in the range.
     * @throws IllegalArgumentException if {@code step} is {@code 0}.
     */
    public RangeSet(int start, int stop, int step) {
        if (step == 0) {
            throw new IllegalArgumentException();
        }
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    /**
     * Returns the number of elements in this set.
     */
    @Override
    public int size() {
        int difference = (stop - start); // show all work
        int count = (difference / step);
        int remainder = Math.abs(difference % step);
        return (count > 0 ? count + remainder : 0);
    }

    /**
     * Returns {@code true} if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return (size() == 0);
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param o element whose presence in this set is to be tested.
     * @return {@code true} if this set contains the specified element.
     */
    @Override
    public boolean contains(Object o) {
        boolean result = false;
        if (o != null && o instanceof Integer && size() != 0) {
            int contains = ((Number) o).intValue();
            if ((step > 0 && contains >= start && contains < stop)
                    || (step < 0 && contains >= start && contains > stop)) {
                result = ((contains - start) % step == 0);
            }
        }
        return result;
    }

    /**
     * Returns an iterator over the elements in this set. The elements are returned in
     * the order they are specified from start to stop.
     */
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int cursor = start;
            @Override public boolean hasNext() {
                boolean result;
                if (step > 0) {
                    result = (cursor < stop);
                } else {
                    result = (cursor > stop);
                }
                return result;
            }
            @Override public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                int result = cursor;
                cursor += step;
                return result;
            }
            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
