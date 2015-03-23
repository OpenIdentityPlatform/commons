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
import org.forgerock.guava.common.hash.Funnels;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.time.TimeService;

/**
 * Factory methods for creating bloom filters with various requirements.
 */
public final class BloomFilters {
    private BloomFilters() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a {@link BloomFilterBuilder} to begin configuration of a particular bloom filter.
     *
     * @param funnel the funnel to use to hash elements of the bloom filter.
     * @param <T> the type of elements to be contained in the bloom filter.
     * @return a builder object to continue configuration of the bloom filter.
     * @see Funnels
     */
    public static <T> BloomFilterBuilder<T> create(final Funnel<? super T> funnel) {
        return new BloomFilterBuilder<T>(funnel);
    }

    /**
     * Expiry strategy for objects that never expire. With this strategy a rolling Bloom Filter becomes a normal
     * scalable Bloom Filter.
     */
    static enum NeverExpires implements ExpiryStrategy<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public static <T> ExpiryStrategy<T> strategy() {
            return (ExpiryStrategy<T>) INSTANCE;
        }

        @Override
        public long expiryTime(final Object it) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Builder for constructing and configuring Bloom Filter implementations.
     */
    public static class BloomFilterBuilder<T> {
        int initialCapacity = 1000;
        double falsePositiveProbability = 0.01d;
        ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.COPY_ON_WRITE;
        int writeBatchSize = 0;

        final Funnel<? super T> funnel;

        BloomFilterBuilder(final Funnel<? super T> funnel) {
            Reject.ifNull(funnel);
            this.funnel = funnel;
        }

        /**
         * Copy constructor.
         */
        BloomFilterBuilder(final BloomFilterBuilder<T> toCopy) {
            this(toCopy.funnel);
            this.initialCapacity = toCopy.initialCapacity;
            this.falsePositiveProbability = toCopy.falsePositiveProbability;
            this.concurrencyStrategy = toCopy.concurrencyStrategy;
            this.writeBatchSize = toCopy.writeBatchSize;
        }

        /**
         * Specifies the initial capacity of the bloom filter. This is the expected number of elements that can be
         * inserted into the bloom filter before it becomes saturated (exceeds the configured false positive
         * probability).
         *
         * @param capacity the initial capacity of the bloom filter.
         */
        public BloomFilterBuilder<T> withInitialCapacity(final int capacity) {
            Reject.ifFalse(capacity > 0, "Initial capacity must be > 0");
            this.initialCapacity = capacity;
            return this;
        }

        /**
         * Specifies the overall probability of false positives that this bloom filter should achieve. The bloom
         * filter will be sized to achieve this probability for the specified capacity.
         *
         * @param fpp the probability of false positives to achieve.
         */
        public BloomFilterBuilder<T> withFalsePositiveProbability(final double fpp) {
            Reject.ifFalse(fpp > 0.0d, "False positive probability must be > 0");
            Reject.ifFalse(fpp < 1.0d, "False positive probability must be < 1");
            this.falsePositiveProbability = fpp;
            return this;
        }

        /**
         * The factor by which to increase the capacity of the bloom filter when it exceeds the initial capacity. A
         * factor of 1 will increase the capacity linearly, which may result in an explosion in the number of buckets
         * if the expected capacity of the filter has been underestimated. The default factor is 2, which ensures
         * that the bloom filter quickly grows to be "large enough" without creating a large number of buckets (i.e.,
         * the number of buckets created will be logarithmic in the total number of insertions).
         *
         * @param growthFactor the factor by which to increase bloom filter capacity when expanding.
         */
        public ScalableBloomFilterBuilder<T> withCapacityGrowthFactor(final double growthFactor) {
            return new ScalableBloomFilterBuilder<T>(this).withCapacityGrowthFactor(growthFactor);
        }

        /**
         * The factor by which to reduce the probability of false positives when expanding a scalable/rolling bloom
         * filter. As the overall false positive probability (FPP) is the sum of the probabilities of each component
         * bloom filter in the chain, the FPP of each subsequent bucket is reduced by this factor so that the sum never
         * exceeds the overall FPP. A factor close to 1 will reduce the FPP slowly, limiting the additional memory
         * used on each expansion, but with the trade-off of using more memory initially. A lower number will use
         * less memory initially but then decrease the FPP more rapidly, resulting in increased memory usage. The
         * default setting (0.8) is tuned for scalable bloom filters that grow unbounded over time. It is recommended
         * to reduce the factor in rolling bloom filters where the chain is expected not to grow beyond a few buckets
         * in size.
         *
         * @param scaleFactor the factor by which to decrease the false positive probability when expanding.
         */
        public ScalableBloomFilterBuilder<T> withFalsePositiveProbabilityScaleFactor(final double scaleFactor) {
            return new ScalableBloomFilterBuilder<T>(this).withFalsePositiveProbabilityScaleFactor(scaleFactor);
        }

        /**
         * Sets the maximum number of buckets to create before throwing an exception indicating the maximum capacity
         * has been reached. Defaults to unlimited.
         *
         * @param maximumNumberOfBuckets the maximum number of buckets to create.
         */
        public ScalableBloomFilterBuilder<T> withMaximumNumberOfBuckets(final int maximumNumberOfBuckets) {
            return new ScalableBloomFilterBuilder<T>(this).withMaximumNumberOfBuckets(maximumNumberOfBuckets);
        }

        /**
         * Strategy for determining when elements of a rolling bloom filter have expired. Used to determine when all
         * elements in a bucket have expired and so can be released.
         *
         * @param expiryStrategy strategy for determining element expiry time.
         */
        public RollingBloomFilterBuilder<T> withExpiryStrategy(final ExpiryStrategy<T> expiryStrategy) {
            return new RollingBloomFilterBuilder<T>(this).withExpiryStrategy(expiryStrategy);
        }

        @VisibleForTesting
        RollingBloomFilterBuilder<T> withClock(final TimeService clock) {
            return new RollingBloomFilterBuilder<T>(this).withClock(clock);
        }

        /**
         * Sets the strategy to use for managing thread-safety in the bloom filter.
         *
         * @param strategy the concurrency strategy to use.
         */
        public BloomFilterBuilder<T> withConcurrencyStrategy(
                final ConcurrencyStrategy strategy) {
            Reject.ifNull(strategy);
            this.concurrencyStrategy = strategy;
            return this;
        }

        /**
         * Enables batching of writes to the bloom filter. This can significantly increase the performance of
         * write-heavy workloads at the expense of using additional memory and reducing the accuracy of the result of
         * the add/addAll methods. It is recommended to enable this setting when using the
         * {@link ConcurrencyStrategy#COPY_ON_WRITE} strategy.
         *
         * @param batchSize the number of elements to buffer before writing to the bloom filter.
         */
        public BloomFilterBuilder<T> withWriteBatchSize(final int batchSize) {
            Reject.ifFalse(batchSize >= 0, "Write batch size must be >= 0");
            this.writeBatchSize = batchSize;
            return this;
        }

        /**
         * Forces the bloom filter to be a Scalable Bloom Filter which can expand to arbitrary capacity.
         *
         * @see <a href="http://www.sciencedirect.com/science/article/pii/S0020019006003127">Scalable Bloom Filters</a>
         * by Almeida et al., <em>Information Processing Letters</em>, 101(6), p.255&ndash;261, 2007.
         */
        public ScalableBloomFilterBuilder<T> scalable() {
            return new ScalableBloomFilterBuilder<T>(this);
        }

        /**
         * Forces the bloom filter to be a Rolling Bloom Filter, which is a variant on Scalable Bloom Filters where
         * elements in the bloom filter can expire allowing space to be reclaimed.
         */
        public RollingBloomFilterBuilder<T> rolling() {
            return new RollingBloomFilterBuilder<T>(this);
        }

        /**
         * Builds the bloom filter with the configured options.
         *
         * @return a new bloom filter configured appropriately.
         */
        public BloomFilter<T> build() {
            BloomFilter<T> bf = buildBloomFilter();
            if (writeBatchSize > 0) {
                bf = new BatchingBloomFilter<T>(bf, writeBatchSize);
            }
            return bf;
        }

        BloomFilter<T> buildBloomFilter() {
            return concurrencyStrategy.<T>getFactory(funnel).create(initialCapacity, falsePositiveProbability);
        }

    }

    /**
     * Builder pattern for Scalable Bloom Filters.
     *
     * @param <T> the type of elements contained in this bloom filter.
     * @see <a href="http://www.sciencedirect.com/science/article/pii/S0020019006003127">Scalable Bloom Filters</a>
     * by Almeida et al., <em>Information Processing Letters</em>, 101(6), p.255&ndash;261, 2007.
     */
    public static class ScalableBloomFilterBuilder<T> extends BloomFilterBuilder<T> {
        double capacityGrowthFactor = 2.0d;
        double falsePositiveProbabilityScaleFactor = 0.8d;
        int maxNumberOfBuckets = Integer.MAX_VALUE;

        ScalableBloomFilterBuilder(final BloomFilterBuilder<T> toCopy) {
            super(toCopy);
            if (toCopy instanceof ScalableBloomFilterBuilder) {
                ScalableBloomFilterBuilder<T> sbf = (ScalableBloomFilterBuilder<T>) toCopy;
                this.capacityGrowthFactor = sbf.capacityGrowthFactor;
                this.falsePositiveProbabilityScaleFactor = sbf.falsePositiveProbabilityScaleFactor;
                this.maxNumberOfBuckets = sbf.maxNumberOfBuckets;
            }
        }

        @Override
        public ScalableBloomFilterBuilder<T> withCapacityGrowthFactor(final double growthFactor) {
            Reject.ifFalse(growthFactor >= 1.0d, "Capacity growth factor must be >= 1");
            this.capacityGrowthFactor = growthFactor;
            return this;
        }

        @Override
        public ScalableBloomFilterBuilder<T> withFalsePositiveProbabilityScaleFactor(final double scaleFactor) {
            Reject.ifFalse(scaleFactor > 0.0d, "False positive probability scale factor must be > 0");
            Reject.ifFalse(scaleFactor < 1.0d, "False positive probability scale factor must be < 1");
            this.falsePositiveProbabilityScaleFactor = scaleFactor;
            return this;
        }

        @Override
        public ScalableBloomFilterBuilder<T> withMaximumNumberOfBuckets(final int maximumNumberOfBuckets) {
            Reject.ifFalse(maximumNumberOfBuckets > 0, "Must be at least one bucket");
            this.maxNumberOfBuckets = maximumNumberOfBuckets;
            return this;
        }

        @Override
        BloomFilter<T> buildBloomFilter() {
            return new RollingBloomFilterBuilder<T>(this).withExpiryStrategy(NeverExpires.<T>strategy())
                    .buildBloomFilter();
        }
    }

    /**
     * Builder pattern for Rolling Bloom Filters, which are Scalable Bloom Filters whose elements can expire allowing
     * space to be reclaimed over time.
     *
     * @param <T> the type of elements to contain in the bloom filter.
     */
    public static final class RollingBloomFilterBuilder<T> extends ScalableBloomFilterBuilder<T> {
        TimeService clock = TimeService.SYSTEM;
        ExpiryStrategy<T> expiryStrategy = null;

        public RollingBloomFilterBuilder(final BloomFilterBuilder<T> toCopy) {
            super(toCopy);
        }

        @VisibleForTesting
        RollingBloomFilterBuilder<T> withClock(final TimeService clock) {
            Reject.ifNull(clock);
            this.clock = clock;
            return this;
        }

        @Override
        public RollingBloomFilterBuilder<T> withExpiryStrategy(final ExpiryStrategy<T> expiryStrategy) {
            Reject.ifNull(expiryStrategy);
            this.expiryStrategy = expiryStrategy;
            return this;
        }

        @Override
        BloomFilter<T> buildBloomFilter() {
            Reject.ifNull(expiryStrategy, "No expiry strategy specified");
            return new ConcurrentRollingBloomFilter<T>(this);
        }
    }
}
