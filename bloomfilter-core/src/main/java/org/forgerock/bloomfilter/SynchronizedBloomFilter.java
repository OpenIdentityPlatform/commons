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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * A fully synchronized bloom filter implementation.
 */
@ThreadSafe
final class SynchronizedBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizedBloomFilter.class);
    private final long capacity;
    private final double falsePositiveProbability;
    private final org.forgerock.guava.common.hash.BloomFilter<T> bloomFilter;

    SynchronizedBloomFilter(final Funnel<? super T> funnel,
                                   final long capacity,
                                   final double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
        this.capacity = capacity;

        this.bloomFilter = org.forgerock.guava.common.hash.BloomFilter.create(funnel, (int) capacity, falsePositiveProbability);
    }

    @Override
    public synchronized void add(final T element) {
        LOGGER.debug("Adding element: {}", element);
        bloomFilter.put(element);
    }

    @Override
    public synchronized void addAll(final Collection<? extends T> elements) {
        LOGGER.debug("Adding elements: {}", elements);
        for (T element : elements) {
            bloomFilter.put(element);
        }
    }

    @Override
    public synchronized boolean mightContain(final T element) {
        return bloomFilter.mightContain(element);
    }

    @Override
    public synchronized BloomFilterStatistics getStatistics() {
        final double expectedFpp = bloomFilter.expectedFpp();
        final long bitSize = BloomFilterStatistics.optimumBitSize(capacity, falsePositiveProbability);
        return new BloomFilterStatistics(falsePositiveProbability, expectedFpp, capacity, bitSize, Long.MAX_VALUE,
                BloomFilterStatistics.optimumRemainingCapacity(bitSize, expectedFpp, capacity));
    }

    @Override
    public String toString() {
        return "SynchronizedBloomFilter{" +
                "capacity=" + capacity +
                ", falsePositiveProbability=" + falsePositiveProbability +
                '}';
    }
}
