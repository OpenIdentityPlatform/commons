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

import static java.lang.Math.min;

import org.forgerock.util.Reject;
import org.forgerock.util.time.TimeService;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A chain of bloom filters that together acts as a bloom filter. The overall false positive probability of the chain
 * is the sum of the false positive probabilities of the elements in the chain. Subsequent filters ("buckets") in the
 * chain are acquired from a {@link BloomFilterPool} according to a geometric series, ensuring that the overall false
 * positive probability is maintained, while also accommodating massive underestimation of required capacity.
 *
 * @see BloomFilterPool
 */
@ThreadSafe
final class BloomFilterChain<T> implements BloomFilter<T> {
    private static final double FILL_FACTOR = 0.9d;
    private static final int MAX_ADD_SIZE = 1000;
    private final List<BloomFilter<T>> chain = new CopyOnWriteArrayList<BloomFilter<T>>();
    private final BloomFilterPool<T> pool;
    private final TimeService clock;

    /**
     * Creates an initially empty filter chain.
     *
     * @param pool the pool from which to get new Bloom Filters for subsequent elements in the chain.
     * @param clock the clock to use for determining if a bloom filter has expired.
     */
    BloomFilterChain(final BloomFilterPool<T> pool, final TimeService clock) {
        Reject.ifNull(pool, clock);
        this.pool = pool;
        this.clock = clock;
    }

    /**
     * Adds the given element to the last bucket in the filter chain, acquiring a new bucket from the pool if all
     * existing buckets are saturated.
     *
     * @param element the element to add to this set.
     * @return {@code true} if any filter in the chain changed as a result of adding this element.
     */
    @Override
    public boolean add(final T element) {
        return lastBucket().add(element);
    }

    /**
     * Adds all of the given elements to the last bucket in the chain, spilling over to new buckets as required to
     * maintain the correct false positive probability.
     *
     * @param elements the elements to add to the set.
     * @return {@code true} if any element in the collection caused the filter chain to change.
     */
    @Override
    public boolean addAll(final Collection<? extends T> elements) {
        boolean changed = false;
        final List<T> queue = new ArrayList<T>(elements);
        int i = 0, size = queue.size();
        while (i < size) {
            final BloomFilter<T> bucket = lastBucket();
            final long remainingCapacity =
                    min((long) (bucket.statistics().getEstimatedRemainingCapacity() * FILL_FACTOR), MAX_ADD_SIZE);
            final int batchSize = min(size - i, (int) remainingCapacity);
            final List<T> batch = (i == 0 && batchSize == size) ? queue : queue.subList(i, i + batchSize);
            changed |= bucket.addAll(batch);
            i += batchSize;
        }
        return changed;
    }

    @Override
    public boolean mightContain(final T element) {
        for (BloomFilter<T> bucket : chain) {
            if (bucket.mightContain(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BloomFilterStatistics statistics() {
        double configuredFpp = 0.0d;
        double expectedFpp = 0.0d;
        long capacity = 0L;
        long bitSize = 0L;
        long lastExpiryTime = Long.MIN_VALUE;
        long remainingCapacity = 0L;
        for (BloomFilter<T> bucket : chain) {
            final BloomFilterStatistics bucketStats = bucket.statistics();
            configuredFpp += bucketStats.getConfiguredFalsePositiveProbability();
            expectedFpp += bucketStats.getExpectedFalsePositiveProbability();
            capacity += bucketStats.getCapacity();
            bitSize += bucketStats.getBitSize();
            lastExpiryTime = Math.max(lastExpiryTime, bucketStats.getExpiryTime());
            // Only the last bucket is relevant to remaining capacity as we never insert into previous ones
            remainingCapacity = bucketStats.getEstimatedRemainingCapacity();
        }

        return new BloomFilterStatistics(configuredFpp, expectedFpp, capacity, bitSize, lastExpiryTime, remainingCapacity);
    }

    private BloomFilter<T> lastBucket() {
        BloomFilter<T> lastBucket = null;
        ListIterator<BloomFilter<T>> it = chain.listIterator(chain.size());
        if (it.hasPrevious()) {
            lastBucket = it.previous();
        }
        if (lastBucket == null || lastBucket.statistics().isSaturated()) {
            // Synchronize to ensure atomicity (double-checked locking). Chain.listIterator().previous() is volatile
            // read.
            synchronized (chain) {
                lastBucket = null;
                it = chain.listIterator(chain.size());
                while (it.hasPrevious()) {
                    lastBucket = it.previous();

                    final BloomFilterStatistics statistics = lastBucket.statistics();
                    if (statistics.isSaturated() && statistics.getExpiryTime() < clock.now()) {
                        // it.remove() is unsupported operation for CoWAL:
                        if (chain.remove(lastBucket)) {
                            pool.release(lastBucket);
                        }
                        lastBucket = null;
                    } else {
                        break;
                    }
                }
                if (lastBucket == null || lastBucket.statistics().isSaturated()) {
                    lastBucket = pool.nextAvailable();
                    chain.add(lastBucket);
                }
            }
        }

        return lastBucket;
    }

    @Override
    public String toString() {
        return "BloomFilterChain{size=" + chain.size() + '}';
    }
}
