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
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Bloom Filter decorator that can expire when the youngest element within it expires.
 */
@ThreadSafe
final class ExpiringBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiringBloomFilter.class);
    private final BloomFilter<T> delegate;
    private final ExpiryStrategy<T> expiryStrategy;

    private final AtomicLong latestExpiryTime = new AtomicLong(Long.MIN_VALUE);

    ExpiringBloomFilter(final BloomFilter<T> delegate,
                        final ExpiryStrategy<T> expiryStrategy) {
        Reject.ifNull(delegate, expiryStrategy);
        this.delegate = delegate;
        this.expiryStrategy = expiryStrategy;
    }

    @Override
    public void add(final T element) {
        updateExpiryTime(expiryStrategy.expiryTime(element));
        delegate.add(element);
    }

    @Override
    public void addAll(final Collection<? extends T> elements) {
        updateExpiryTime(maxExpiryTime(elements));
        delegate.addAll(elements);
    }

    @Override
    public boolean mightContain(final T element) {
        return expiryStrategy.expiryTime(element) <= latestExpiryTime.get() && delegate.mightContain(element);
    }

    @Override
    public BloomFilterStatistics getStatistics() {
        final BloomFilterStatistics stats = delegate.getStatistics();
        return new BloomFilterStatistics(stats.getConfiguredFalsePositiveProbability(),
                stats.getExpectedFalsePositiveProbability(),
                stats.getCapacity(),
                stats.getBitSize(),
                latestExpiryTime.get(),
                stats.getEstimatedRemainingCapacity());
    }

    /**
     * Atomic update of the latest expiry time.
     * @param newExpiryTime the candidate new latest expiry time.
     */
    private void updateExpiryTime(final long newExpiryTime) {
        int attempts = 0;
        boolean changed;
        long oldExpiryTime;
        do {
            attempts++;
            oldExpiryTime = latestExpiryTime.get();
            changed = newExpiryTime > oldExpiryTime;
        } while (changed && !latestExpiryTime.compareAndSet(oldExpiryTime, newExpiryTime));

        LOGGER.debug("Updated expiry timestamp after {} attempts: new={}, old={}, changed?={}", attempts, newExpiryTime,
                oldExpiryTime, changed);
    }

    private long maxExpiryTime(final Collection<? extends T> elements) {
        long max = Long.MIN_VALUE;
        for (T element : elements) {
            max = Math.max(expiryStrategy.expiryTime(element), max);
        }
        return max;
    }
}
