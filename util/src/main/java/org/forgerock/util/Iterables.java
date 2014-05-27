/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.util;

import org.forgerock.util.promise.Function;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Iterable utilties.
 */
public class Iterables {
    /**
     * An {@link Iterable} that will filter the provided {@link Iterable}
     * using a {@link Predicate}.
     */
    private static class FilteredIterable<T> implements Iterable<T> {

        /** the Iterable to filter */
        private final Iterable<T> iterable;

        /** the Predicate to determine inclusion or exclusion */
        private final Predicate<T> predicate;

        /**
         * Construct the FilteredIterable.
         *
         * @param iterable the Iterable to filter
         * @param predicate the Predicate to determine inclusion
         */
        private FilteredIterable(Iterable<T> iterable, Predicate<T> predicate) {
            this.iterable = iterable;
            this.predicate = predicate;
        }

        /**
         * Return an {@link Iterator} that filters objects according to the Predicate.
         *
         * @return the filtered iterator.
         */
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                final Iterator<T> oIterator = iterable.iterator();
                T nextElement = null;

                @Override
                public boolean hasNext() {
                    if (nextElement != null) {
                        // fail-fast for next() or repeated hasNext() calls
                        return true;
                    }

                    while (oIterator.hasNext()) {
                        nextElement = oIterator.next();
                        if (predicate.apply(nextElement)) {
                            return true;
                        }
                        nextElement = null;
                    }
                    return false;
                }

                @Override
                public T next() {
                    if (!hasNext()) { // ensure hasNext() has "advanced" the next "filtered-in" element
                        throw new NoSuchElementException();
                    }

                    // return next element and reset for next iteration
                    T retValue = nextElement;
                    nextElement = null;
                    return retValue;
                }

                @Override
                public void remove() {
                    oIterator.remove();
                }
            };
        }
    }

    /**
     * An {@link Iterable} that will apply a {@link Function} on the elements of
     * the provided {@link Iterable}.
     */
    private static class MappedIterable<T, R, E extends RuntimeException> implements Iterable<R> {

        /** the Iterable to map */
        private final Iterable<T> iterable;

        /** the function to map across the elements */
        private final Function<T, R, E> mapper;

        /**
         * Construct the MappedIterable.
         *
         * @param iterable the Iterable to map
         * @param mapper the mapper function to apply
         */
        private MappedIterable(Iterable<T> iterable, Function<T, R, E> mapper) {
            this.iterable = iterable;
            this.mapper = mapper;
        }

        /**
         * Return an {@link Iterator} that maps objects according to the mapper function.
         *
         * @return the mapped iterator.
         */
        @Override
        public Iterator<R> iterator() {
            return new Iterator<R>() {
                final Iterator<T> oIterator = iterable.iterator();

                @Override
                public boolean hasNext() {
                    return oIterator.hasNext();
                }

                @Override
                public R next() {
                    return mapper.apply(oIterator.next());
                }

                @Override
                public void remove() {
                    oIterator.remove();
                }
            };
        }
    }

    private Iterables() {
        // prevent construction
    }

    /**
     * Filter an {@link Iterable} according to a {@link Predicate}.  The {@link Predicate}
     * determines which elements are <em>filtered <strong>in</strong></em>.
     * <pre>
     * {@code
     *     List<String> fruit = new ArrayList<String>();
     *     fruit.add("apple");
     *     fruit.add("pineapple");
     *     fruit.add("banana");
     *     fruit.add("orange");
     *     fruit.add("pear");
     *     return filter(fruit,
     *              new Predicate<String>() {
     *                  public boolean apply(String fruit) {
     *                      return fruit.startsWith("p");
     *                  }
     *              });
     * }
     * </pre>
     * would return
     * <pre>
     *     [ "pineapple, "pear" ]
     * </pre>
     *
     * @param <T> the element type
     * @param iterable the {@linkplain Iterable} to filter
     * @param predicate the {@linkplan Predicate} used to include elements in the filtered result
     * @return the filtered {@linkplain Iterable}
     */
    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<T> predicate) {
        return new FilteredIterable<T>(iterable, predicate);
    }

    /**
     * Create an {@link Iterable} according to a source {@link Iterable} and a mapper {@link Function}.
     * The {@link Function} transforms the source elements in the source Iterable.
     * <pre>
     * {@code
     *     List<String> fruit = new ArrayList<String>();
     *     fruit.add("apple");
     *     fruit.add("pineapple");
     *     fruit.add("banana");
     *     fruit.add("orange");
     *     fruit.add("pear");
     *     return map(fruit,
     *              new Function<String, Integer, NeverThrowsException>() {
     *                  public Integer apply(String fruit) {
     *                      return fruit.length();
     *                  }
     *              });
     * }
     * </pre>
     * would return an iterable whose elements are
     * <pre>
     *     [ 5, 9, 6, 6, 4 ]
     * </pre>
     *
     * @param <T> the source element type
     * @param <R> the returned iterable element type
     * @param <E> a RuntimeException thrown by the map function
     * @param iterable the source {@linkplain Iterable} to map
     * @param mapper the {@linkplan Function} used to map elements from source type to return type
     * @return the mapped {@linkplain Iterable}
     */
    public static <T, R, E extends RuntimeException> Iterable<R> map(Iterable<T> iterable, Function<T, R, E> mapper) {
        return new MappedIterable(iterable, mapper);
    }

}
