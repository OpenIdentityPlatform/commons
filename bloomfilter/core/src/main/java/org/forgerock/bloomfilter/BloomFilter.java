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

package org.forgerock.bloomfilter;

import java.util.Collection;

/**
 * General interface contract for implementations of <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom
 * Filters</a>. A Bloom Filter is a Set-like data structure that uses only a few bits of memory per element stored.
 * This allows very large numbers (billions) of elements to be stored in-memory, with the trade-off being that set
 * containment can now return false positives. That is, the Bloom Filter can say for certain if a given element is
 * definitely <em>not</em> in the set, but it cannot say for certain whether it is. Bloom Filters are therefore
 * appropriate as a quick initial check to reduce load on a more expensive but more accurate storage mechanism (e.g.,
 * a database) or where some level of false positives is tolerable. The expected usage pattern for the former case
 * would be something like the following psuedo-code:
 * <pre>
 *     BloomFilter&lt;T&gt filter = ...;
 *     Set&lt;T&gt; expensiveSet = ...; // e.g. database, web-service
 *     if (filter.mightContain(element)) {
 *         // Perform the more expensive check to be sure
 *         return expensive.contains(element);
 *     }
 *     return false;
 * </pre>
 * Note: this assumes that the Bloom Filter is kept in-sync with the definitive set! How this is accomplished is
 * outside of the scope of this package.
 * <p/>
 * All Bloom Filter implementations in this package allow the probability of false positives (FPP) to be specified,
 * and the implementation will adjust the amount of memory used to ensure the given level of accuracy for the
 * expected number of elements.
 *
 * @param <E> the type of elements contained in the bloom filter.
 * @see <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom Filter Wikipedia entry</a>.
 */
public interface BloomFilter<E> {

    /**
     * Adds the specified element to this set if it is not already possibly present. After a call to this method,
     * subsequent calls to {@link #mightContain(Object)} will return {@code true} for the same object.
     *
     * @param element the element to add to this set.
     */
    void add(E element);

    /**
     * Adds all of the specified elements to this set if they are not possibly already present.
     *
     * @param elements the elements to add to the set.
     */
    void addAll(Collection<? extends E> elements);

    /**
     * Checks if the given element <em>might</em> be a member of this set. If this method returns {@code false}, then
     * the given object is definitely not a member of the set. If the result is {@code true} then the object may or
     * may not be a member of this set, with a certain probability of false positives.
     *
     * @param element the element to check for membership in this set.
     * @return {@code false} if the element is definitely not in the set, or {@code true} if it might be.
     */
    boolean mightContain(E element);

    /**
     * Gets a snapshot of the current statistics of the set.
     */
    BloomFilterStatistics getStatistics();
}
