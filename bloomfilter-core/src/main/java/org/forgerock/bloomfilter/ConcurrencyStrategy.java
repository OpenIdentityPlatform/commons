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

import org.forgerock.guava.common.hash.Funnel;

/**
 * Strategy that determines how thread-safety of bloom filters should be managed. Different strategies have different
 * trade-offs in terms of memory usage and read or write performance.
 */
public enum ConcurrencyStrategy {
    /**
     * Treats Bloom Filters as effectively immutable objects and creates a copy when applying updates before
     * atomically swapping with the original. This strategy optimises read (mightContain) performance at the cost of
     * significantly more expensive write (add/addAll) times and increased garbage collection pressure due to the
     * temporary copies that are created. This is the recommended strategy if read performance is critical. Writing
     * batching can be used to mitigate the poor write performance (even exceeding the synchronized strategy) and
     * somewhat mitigate the additional garbage creation.
     */
    COPY_ON_WRITE {
        @Override
        <T> BloomFilterFactory<T> getFactory(final Funnel<? super T> funnel) {
            return new BloomFilterFactory<T>() {
                @Override
                public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
                    return new CopyOnWriteBloomFilter<T>(funnel, expectedInsertions, falsePositiveProbability);
                }
            };
        }
    },
    /**
     * Fully synchronizes all accesses to the bloom filter. For scalable and rolling bloom filters, each bucket in
     * the chain is synchronized individually, allowing a finer level of locking. This strategy provides a reasonable
     * compromise between read and write performance when read performance is not critical. Additionally, this
     * strategy creates significantly less garbage collection pressure than the copy-on-write strategy as it modifies
     * bloom filters in-place.
     */
    SYNCHRONIZED {
        @Override
        <T> BloomFilterFactory<T> getFactory(final Funnel<? super T> funnel) {
            return new BloomFilterFactory<T>() {
                @Override
                public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
                    return new SynchronizedBloomFilter<T>(funnel, expectedInsertions, falsePositiveProbability);
                }
            };
        }
    },

    /**
     * Uses atomic compare-and-set (CAS) instructions to implement BloomFilter operations over AtomicLongArrays. This
     * strategy offers excellent performance for reads and writes with less garbage collection pressure than the Copy
     * on Write strategy and comparable performance to that strategy even with write batching.
     */
    ATOMIC {
        @Override
        <T> BloomFilterFactory<T> getFactory(final Funnel<? super T> funnel) {
            return new BloomFilterFactory<T>() {
                @Override
                public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
                    return new AtomicBloomFilter<T>(funnel, expectedInsertions, falsePositiveProbability);
                }
            };
        }
    }
    ;

    /**
     * Returns a factory object for creating fixed-capacity bloom filters using the given concurrency strategy.
     *
     * @param funnel the funnel to use for hashing elements.
     * @param <T> the type of elements to contain.
     * @return an appropriate factory object for this concurrency strategy.
     */
    abstract <T> BloomFilterFactory<T> getFactory(Funnel<? super T> funnel);

}
