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

import java.util.Collection;
import java.util.Collections;
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
 *
 * @param <T> the type of elements stored in this bloom filter.
 */
final class BatchingBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchingBloomFilter.class);
    private final BloomFilter<T> delegate;
    private final int batchSize;
    private final Collection<T> buffer;

    /**
     * Constructs the batching decorator with the given delegate and batch size.
     *
     * @param delegate the real bloom filter to delegate calls to.
     * @param batchSize the number of writes to batch before sending to the delegate.
     */
    BatchingBloomFilter(final BloomFilter<T> delegate, final int batchSize) {
        Reject.ifNull(delegate);
        Reject.ifFalse(batchSize > 0, "Batch size must be > 0");
        this.delegate = delegate;
        this.batchSize = batchSize;
        this.buffer = Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>(batchSize));
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
        // Synchronize on the buffer to ensure mutual exclusion from other writes when draining the buffer. We don't
        // care about concurrent reads while this is occurring because the ConcurrentHashMap will ensure they see
        // some consistent state of affairs. I haven't discovered any clever lock-free algorithm that can ensure both
        // (i) no lost updates, and (ii) that #mightContains never returns false negatives during a buffer flush. In
        // practice, I would expect this lock to be uncontended in typical usage.
        synchronized (buffer) {
            boolean changed = buffer.add(element);
            int size = buffer.size();
            if (size >= batchSize) {
                LOGGER.debug("Flushing buffer: size={}", size);
                changed = delegate.addAll(buffer);
                buffer.clear();
            }
            return changed;
        }
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
        // Always check the buffer first to ensure no false negatives during a buffer flush
        return buffer.contains(element) || delegate.mightContain(element);
    }

    @Override
    public BloomFilterStatistics getStatistics() {
        // Adjust estimated remaining capacity to take into account current buffer size
        final BloomFilterStatistics stats = delegate.getStatistics();
        return new BloomFilterStatistics(stats.getConfiguredFalsePositiveProbability(),
                stats.getExpectedFalsePositiveProbability(), stats.getCapacity(), stats.getBitSize(),
                stats.getExpiryTime(), stats.getEstimatedRemainingCapacity() - buffer.size());
    }

    @Override
    public String toString() {
        return "BatchingBloomFilter{delegate=" + delegate + ", batchSize=" + batchSize + '}';
    }
}
