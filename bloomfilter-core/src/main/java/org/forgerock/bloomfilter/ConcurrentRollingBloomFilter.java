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

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * A thread-safe implementation of a Bloom Filter that can expand over time to accommodate arbitrary numbers of
 * elements, while also allowing old elements to be deleted after they have expired. Rolling bloom filters are useful
 * for maintaining on-going blacklists of short-lived elements.
 * <p/>
 * This implementation is optimised for concurrent read performance. Concurrent writes performance is likely to be
 * considerably slower than reads, so this implementation is only suitable for situations where read performance is
 * critical and writes are relatively rare. Write performance may be improved by batching writes via the
 * {@link #addAll(Collection)} method, or by using some external synchronisation mechanism to perform pre-emptive
 * locking (at the cost of reducing read performance).
 */
@ThreadSafe
public final class ConcurrentRollingBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentRollingBloomFilter.class);

    private final BloomFilterChain<T> bucketChain;
    private final BloomFilterPool<T> bucketPool;
    private final ConcurrencyStrategy concurrencyStrategy;

    ConcurrentRollingBloomFilter(final BloomFilters.RollingBloomFilterBuilder<T> builder) {
        this.concurrencyStrategy = builder.concurrencyStrategy;

        BloomFilterFactory<T> factory = concurrencyStrategy.<T>getFactory(builder.funnel);
        if (builder.expiryStrategy != BloomFilters.NeverExpires.strategy()) {
            factory = new ExpiringBloomFilterFactory<T>(factory, builder.expiryStrategy);
        }
        this.bucketPool = new GeometricSeriesBloomFilterPool<T>(factory, builder.maxNumberOfBuckets,
                builder.initialCapacity, builder.capacityGrowthFactor,
                builder.falsePositiveProbability, builder.falsePositiveProbabilityScaleFactor);
        this.bucketChain = new BloomFilterChain<T>(bucketPool, builder.clock);
    }

    private static final class ExpiringBloomFilterFactory<T> implements BloomFilterFactory<T> {
        private final BloomFilterFactory<T> factory;
        private final ExpiryStrategy<T> expiryStrategy;

        ExpiringBloomFilterFactory(final BloomFilterFactory<T> factory,
                                   final ExpiryStrategy<T> strategy) {
            this.factory = factory;
            this.expiryStrategy = strategy;
        }

        @Override
        public BloomFilter<T> create(final long expectedInsertions, final double falsePositiveProbability) {
            return new ExpiringBloomFilter<T>(factory.create(expectedInsertions, falsePositiveProbability),
                    expiryStrategy);
        }
    }

    @Override
    public void add(final T element) {
        LOGGER.debug("Adding object {}", element);
        Reject.ifNull(element);
        bucketChain.add(element);
    }

    @Override
    public void addAll(final Collection<? extends T> elements) {
        Reject.ifNull(elements);
        LOGGER.debug("Adding collection: {}", elements);
        bucketChain.addAll(elements);
    }

    @Override
    public boolean mightContain(final T element) {
        return bucketChain.mightContain(element);
    }

    @Override
    public BloomFilterStatistics getStatistics() {
        return bucketChain.getStatistics();
    }


    @Override
    public String toString() {
        return "ConcurrentRollingBloomFilter{" +
                "concurrencyStrategy=" + concurrencyStrategy +
                ", bucketChain=" + bucketChain +
                ", bucketPool=" + bucketPool +
                '}';
    }
}
