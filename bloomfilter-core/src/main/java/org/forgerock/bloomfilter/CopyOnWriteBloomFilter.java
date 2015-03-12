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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe implementation of a Bloom Filter that is optimised for read performance with only rarely expected
 * additions to the set.
 */
@ThreadSafe
final class CopyOnWriteBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyOnWriteBloomFilter.class);
    private final long capacity;
    private final double falsePositiveProbability;

    private final AtomicReference<org.forgerock.guava.common.hash.BloomFilter<T>> bloomFilterAtomicReference;

    CopyOnWriteBloomFilter(final Funnel<? super T> funnel,
                           final long capacity,
                           final double falsePositiveProbability) {
        this.capacity = capacity;
        this.falsePositiveProbability = falsePositiveProbability;

        final org.forgerock.guava.common.hash.BloomFilter<T> bf =
                org.forgerock.guava.common.hash.BloomFilter.create(funnel, (int) capacity, falsePositiveProbability);
        this.bloomFilterAtomicReference = new AtomicReference<org.forgerock.guava.common.hash.BloomFilter<T>>(bf);
    }

    @Override
    public void add(final T element) {
        addAll(Collections.singleton(element));
    }

    @Override
    public void addAll(final Collection<? extends T> elements) {
        LOGGER.debug("Adding elements: {}", elements);
        boolean changed;
        org.forgerock.guava.common.hash.BloomFilter<T> prev;
        org.forgerock.guava.common.hash.BloomFilter<T> next;

        int attempts = 0;

        do {
            attempts++;
            changed = false;
            prev = bloomFilterAtomicReference.get();
            next = prev.copy();

            for (T element : elements) {
                changed |= next.put(element);
            }

        } while (changed && !bloomFilterAtomicReference.compareAndSet(prev, next));

        LOGGER.debug("Updated BloomFilter after {} attempts", attempts);
    }

    @Override
    public boolean mightContain(final T element) {
        return bloomFilterAtomicReference.get().mightContain(element);
    }

    @Override
    public BloomFilterStatistics getStatistics() {
        final double expectedFpp = bloomFilterAtomicReference.get().expectedFpp();
        final long bitSize = BloomFilterStatistics.optimumBitSize(capacity, falsePositiveProbability);
        return new BloomFilterStatistics(falsePositiveProbability, expectedFpp, capacity, bitSize, Long.MAX_VALUE,
                BloomFilterStatistics.optimumRemainingCapacity(bitSize, expectedFpp, capacity));
    }

    @Override
    public String toString() {
        return "CopyOnWriteBloomFilter" + getStatistics();
    }
}
