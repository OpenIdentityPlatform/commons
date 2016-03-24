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

import java.util.Comparator;

/**
 * Ordered pair of arbitrary objects. Use of Pair is strictly restricted to internal (non-public) APIs:<br>
 * - Pair cannot be used in public APIs
 * - Consider using dedicated classes over Pair. Dedicated classes are more readable while Pair is meaningless.<br>
 * - Pair should only be used to return two values from a method, just like a tuple. If you need more than two
 * values, create a dedicated class.
 *
 * @param <F>
 *            type of the first pair element
 * @param <S>
 *            type of the second pair element
 */
public final class Pair<F, S> {

    private static final class ComparablePairComparator
            <F extends Comparable<F>, S extends Comparable<S>>
            implements Comparator<Pair<F, S>> {
        /** {@inheritDoc} */
        @Override
        public int compare(Pair<F, S> o1, Pair<F, S> o2) {
            final int compareResult = o1.getFirst().compareTo(o2.getFirst());
            if (compareResult == 0) {
                return o1.getSecond().compareTo(o2.getSecond());
            }
            return compareResult;
        }
    }

    /** An empty Pair. */
    public static final Pair<?, ?> EMPTY = Pair.of(null, null);

    /**
     * {@link Comparator} for {@link Pair}s made of {@link Comparable} elements.
     */
    @SuppressWarnings("rawtypes")
    public static final Comparator COMPARATOR = new ComparablePairComparator();

    /** The first pair element. */
    private final F first;

    /** The second pair element. */
    private final S second;

    /**
     * Creates a pair.
     *
     * @param first
     *            the first element of the constructed pair
     * @param second
     *            the second element of the constructed pair
     */
    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a new {@code Pair}.
     *
     * @param first
     *            the first element of the constructed pair
     * @param second
     *            the second element of the constructed pair
     * @param <F>
     *            type of the first pair element
     * @param <S>
     *            type of the second pair element
     * @return A new Pair built with the provided elements
     */
    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    /**
     * Returns an empty Pair matching the required types.
     *
     * @param <F>
     *            type of the first pair element
     * @param <S>
     *            type of the second pair element
     * @return An empty Pair matching the required types
     */
    @SuppressWarnings("unchecked")
    public static <F, S> Pair<F, S> empty() {
        return (Pair<F, S>) EMPTY;
    }

    /**
     * Returns a comparator for Pairs of comparable objects.
     *
     * @param <F>
     *            type of the first pair element
     * @param <S>
     *            type of the second pair element
     * @return a comparator for Pairs of comparable objects.
     */
    @SuppressWarnings("unchecked")
    public static <F extends Comparable<F>, S extends Comparable<S>> Comparator<Pair<F, S>> getPairComparator() {
        return COMPARATOR;
    }

    /**
     * Returns the first element of this pair.
     *
     * @return the first element of this pair
     */
    public F getFirst() {
        return first;
    }

    /**
     * Returns the second element of this pair.
     *
     * @return the second element of this pair
     */
    public S getSecond() {
        return second;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }

        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (first == null) {
            if (other.first != null) {
                return false;
            }
        } else if (!first.equals(other.first)) {
            return false;
        }

        if (second == null) {
            if (other.second != null) {
                return false;
            }
        } else {
            return second.equals(other.second);
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Pair [" + first + ", " + second + "]";
    }
}
