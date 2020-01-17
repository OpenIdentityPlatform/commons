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

import static java.lang.Math.max;
import static java.lang.Math.min;

import org.forgerock.util.Reject;
import org.forgerock.util.time.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A chain of bloom filters that together acts as a single bloom filter. The overall false positive probability of the
 * chain is the sum of the false positive probabilities of the elements in the chain. Subsequent filters ("buckets")
 * in the chain are acquired from a {@link BloomFilterPool} according to a geometric series, ensuring that the overall
 * false positive probability is maintained, while also accommodating massive underestimation of required capacity.
 * <p/>
 * The chain supports removal of bloom filters from the chain if all elements contained within that Bloom Filter have
 * expired. This forms the basis of <em>Rolling Bloom Filters</em>, which provide a time-limited view of some set.
 * Use-cases include blacklisting user security tokens that will naturally expire after a certain interval anyway and so
 * only need to be blacklisted until that expiry time.
 *
 * @param <T> the type of elements stored in the bloom filter.
 * @see GeometricSeriesBloomFilterPool
 */
@ThreadSafe
final class BloomFilterChain<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterChain.class);

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
     */
    @Override
    public void add(final T element) {
        lastBucket().add(element);
    }

    /**
     * Adds all of the given elements to the last bucket in the chain, spilling over to new buckets as required to
     * maintain the correct false positive probability.
     *
     * @param elements the elements to add to the set.
     */
    @Override
    public void addAll(final Collection<? extends T> elements) {
        @SuppressWarnings("unchecked")
        final List<T> queue = (elements instanceof List) ? (List<T>) elements : new ArrayList<T>(elements);
        final int size = queue.size();
        int i = 0;

        // We cannot simply call lastBucket().addAll(...) because this might over-saturate that bucket. Instead we
        // estimate how many more elements can be inserted into that bucket before it becomes saturated and only
        // insert (some percentage of) that number at a time, creating a new bucket if it does actually overflow.
        while (i < size) {
            final BloomFilter<T> bucket = lastBucket();
            final long remainingCapacity = bucket.getStatistics().getEstimatedRemainingCapacity();
            final int batchSize = min(size - i, min(max((int)(remainingCapacity * FILL_FACTOR), 1), MAX_ADD_SIZE));

            LOGGER.debug("Adding batch: remainingCapacity={}, batchSize={}", remainingCapacity, batchSize);

            final List<T> batch = (i == 0 && batchSize == size) ? queue : queue.subList(i, i + batchSize);
            bucket.addAll(batch);
            i += batchSize;
        }
    }

    /**
     * Checks each bloom filter in the chain to see if any of them might contain the given element.
     *
     * @param element the element to check for membership in this set.
     * @return {@code true} if any of the filters in the chain might contain the given element.
     */
    @Override
    public boolean mightContain(final T element) {
        for (BloomFilter<T> bucket : chain) {
            if (bucket.mightContain(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the aggregate statistics for all buckets in the chain as it currently stands. Note that this will
     * underestimate the remaining capacity, as it does not take into account the capacity that is still available in
     * the pool.
     */
    @Override
    public BloomFilterStatistics getStatistics() {
        final double configuredFpp = pool.getOverallFalsePositiveProbability();
        double expectedFpp = 0.0d;
        long capacity = 0L;
        long bitSize = 0L;
        long lastExpiryTime = Long.MIN_VALUE;
        long remainingCapacity = 0L;
        for (BloomFilter<T> bucket : chain) {
            final BloomFilterStatistics bucketStats = bucket.getStatistics();
            expectedFpp += bucketStats.getExpectedFalsePositiveProbability();
            capacity += bucketStats.getCapacity();
            bitSize += bucketStats.getBitSize();
            lastExpiryTime = Math.max(lastExpiryTime, bucketStats.getExpiryTime());
            // Only the last bucket is relevant to remaining capacity as we never insert into previous ones
            remainingCapacity = bucketStats.getEstimatedRemainingCapacity();
        }

        return new BloomFilterStatistics(configuredFpp, expectedFpp, capacity, bitSize, lastExpiryTime,
                remainingCapacity);
    }

    /**
     * Returns a reference to the last bucket in the chain, creating a new bucket if the chain is empty or if the
     * last bucket is saturated. Additionally, this method will release any buckets that have expired.
     */
    private BloomFilter<T> lastBucket() {
        BloomFilter<T> lastBucket = null;
        ListIterator<BloomFilter<T>> it = chain.listIterator(chain.size());
        if (it.hasPrevious()) {
            lastBucket = it.previous();
        }
        if (lastBucket == null || lastBucket.getStatistics().isSaturated()) {
            // Synchronize to ensure atomicity (double-checked locking). Chain.listIterator().previous() is volatile
            // read.
            synchronized (chain) {
                // Perform some initial cleanup to remove any expired buckets
                Set<BloomFilter<T>> toRemove = new HashSet<BloomFilter<T>>();
                for (BloomFilter<T> bucket : chain) {
                    final long now = clock.now();
                    final BloomFilterStatistics stats = bucket.getStatistics();
                    if (stats.isSaturated() && stats.getExpiryTime() < now) {
                        toRemove.add(bucket);
                        pool.release(bucket);
                    }
                }
                // CoWAL performance is better if we remove all at once.
                if (!toRemove.isEmpty()) {
                    LOGGER.debug("Removing expired buckets: {}", toRemove);
                    chain.removeAll(toRemove);
                }

                lastBucket = null;
                it = chain.listIterator(chain.size());
                if (it.hasPrevious()) {
                    lastBucket = it.previous();
                }
                if (lastBucket == null || lastBucket.getStatistics().isSaturated()) {
                    LOGGER.debug("Adding new bucket: {}", lastBucket);
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
