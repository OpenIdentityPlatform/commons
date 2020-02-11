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
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A bloom filter decorator that batches up writes in an internal buffer and applies them once the buffer is full via
 * the {@link BloomFilter#addAll(Collection)} method. This can be useful to mitigate the slow write performance (and
 * memory usage) of the {@link CopyOnWriteBloomFilter} by amortizing the cost of the array copy over a large number
 * of modifications. The implementation ensures that the buffer is also considered during any read requests,
 * eliminating the possibility of false negatives.
 *
 * @param <T> the type of elements stored in this bloom filter.
 */
@ThreadSafe
final class BatchingBloomFilter<T> implements BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchingBloomFilter.class);
    private final BloomFilter<T> delegate;
    @VisibleForTesting
    final int batchSize;
    private final Collection<T> buffer;

    /**
     * Indicates that a buffer flush is in progress. Ensures mutual exclusion of other writer threads.
     * This performs significantly better than a lock in most realistic scenarios.
     */
    private final AtomicBoolean bufferFlushInProgress = new AtomicBoolean(false);

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
     */
    @Override
    public void add(final T element) {
        boolean changed = buffer.add(element);
        if (changed) {
            // Determine if the buffer needs to be flushed to the underlying bloom filter
            if (buffer.size() >= batchSize && bufferFlushInProgress.compareAndSet(false, true)) {
                try {
                    // Copy the buffer into a temporary buffer so that we do not lose updates during the flush.
                    // We use a Set here to speed up the buffer.removeAll method below, which is a hot-spot.
                    final Set<T> tmp = new HashSet<T>(buffer);
                    int size = tmp.size();
                    if (size >= batchSize) {
                        LOGGER.debug("Flushing buffer: size={}", size);
                        delegate.addAll(tmp);
                        buffer.removeAll(tmp);
                    }
                } finally {
                    bufferFlushInProgress.set(false);
                }
            }
        }
    }

    /**
     * Adds the entire collection of elements directly to the underlying Bloom Filter. No buffering is done in this
     * case.
     *
     * @param elements the elements to add to the set.
     */
    @Override
    public void addAll(final Collection<? extends T> elements) {
        // Pass through directly
        delegate.addAll(elements);
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
