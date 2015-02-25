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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bloom filter decorator that batches up writes in an internal buffer and applies them once the buffer is full via
 * the {@link BloomFilter#addAll(Collection)} method. This can be useful to mitigate the slow write performance (and
 * memory usage) of the {@link CopyOnWriteBloomFilter} by amortizing the cost of the array copy over a large number
 * of modifications. The implementation ensures that the buffer is also considered during any read requests,
 * eliminating the possibility of false negatives. The downside of this implementation, other than increased memory
 * usage and a small additional overhead for reads, is that the result of the {@link #add(Object)} method is no
 * longer accurate for any write that does not trigger a buffer flush, and only returns an aggregate result for those
 * that do.
 */
final class BatchingBloomFilter<T> implements BloomFilter<T> {
    private final BloomFilter<T> delegate;
    private final int batchSize;
    private final Set<T> buffer;

    /**
     * Constructs the batching decorator with the given delegate and batch size.
     *
     * @param delegate the real bloom filter to delegate calls to.
     * @param batchSize the number of writes to batch before sending to the delegate.
     */
    BatchingBloomFilter(final BloomFilter<T> delegate, final int batchSize) {
        this.delegate = delegate;
        this.batchSize = batchSize;
        this.buffer = newBuffer(batchSize);
    }

    /**
     * Adds the given element to the internal buffer. If the buffer has exceeded the batch size then the contents of
     * the buffer is flushed to the underlying bloom filter.
     *
     * @param element the element to add to this set.
     * @return {@code false} if the addition causes the buffer to be flushed to the underlying bloom filter, but no
     * changes were made as a result, otherwise {@code true}. As with all BloomFilters, a {@code true} result should
     * be treated as "maybe".
     */
    @Override
    public boolean add(final T element) {
        boolean changed = buffer.add(element);
        if (buffer.size() >= batchSize) {
            final List<T> batch = new ArrayList<T>(buffer);
            if (!batch.isEmpty()) {
                changed |= delegate.addAll(batch);
                // Only remove elements that we have actually processed, to avoid lost updates
                buffer.removeAll(batch);
            }
        }
        return changed;
    }

    /**
     * Adds the entire collection of elements directly to the underlying Bloom Filter. No buffering is done in this
     * case.
     *
     * @param elements the elements to add to the set.
     * @return {@code true} if changes were made to the underlying Bloom Filter as a result of adding any of the
     * elements
     */
    @Override
    public boolean addAll(final Collection<? extends T> elements) {
        // Pass through directly
        return delegate.addAll(elements);
    }

    @Override
    public boolean mightContain(final T element) {
        return buffer.contains(element) || delegate.mightContain(element);
    }

    @Override
    public BloomFilterStatistics statistics() {
        // Adjust estimated remaining capacity to take into account current buffer size
        final BloomFilterStatistics stats = delegate.statistics();
        return new BloomFilterStatistics(stats.getConfiguredFalsePositiveProbability(),
                stats.getExpectedFalsePositiveProbability(), stats.getCapacity(), stats.getBitSize(),
                stats.getExpiryTime(), stats.getEstimatedRemainingCapacity() - buffer.size());
    }

    @Override
    public String toString() {
        return "BatchingBloomFilter{delegate=" + delegate + ", batchSize=" + batchSize + '}';
    }

    private Set<T> newBuffer(int batchSize) {
        return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>(batchSize));
    }
}
