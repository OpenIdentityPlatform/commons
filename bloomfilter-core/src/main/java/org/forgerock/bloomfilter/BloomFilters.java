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
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.time.TimeService;

/**
 * Methods for creating bloom filters of various kinds.
 */
public final class BloomFilters {
    private BloomFilters() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> BloomFilterBuilder<T> create(final Funnel<? super T> funnel) {
        return new BloomFilterBuilder<T>(funnel);
    }

    /**
     * Expiry strategy for objects that never expire. With this strategy a rolling Bloom Filter becomes a normal scalable
     * Bloom Filter.
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
     * Builder pattern for constructing and configuring Bloom Filter implementations.
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

        BloomFilterBuilder(final BloomFilterBuilder<T> toCopy) {
            this(toCopy.funnel);
            this.initialCapacity = toCopy.initialCapacity;
            this.falsePositiveProbability = toCopy.falsePositiveProbability;
            this.concurrencyStrategy = toCopy.concurrencyStrategy;
            this.writeBatchSize = toCopy.writeBatchSize;
        }

        public BloomFilterBuilder<T> withInitialCapacity(final int capacity) {
            Reject.ifFalse(capacity > 0, "Initial capacity must be > 0");
            this.initialCapacity = capacity;
            return this;
        }

        public BloomFilterBuilder<T> withFalsePositiveProbability(final double fpp) {
            Reject.ifFalse(fpp > 0.0d, "False positive probability must be > 0");
            Reject.ifFalse(fpp < 1.0d, "False positive probability must be < 1");
            this.falsePositiveProbability = fpp;
            return this;
        }

        public ScalableBloomFilterBuilder<T> withCapacityGrowthFactor(final double growthFactor) {
            return new ScalableBloomFilterBuilder<T>(this).withCapacityGrowthFactor(growthFactor);
        }

        public ScalableBloomFilterBuilder<T> withFalsePositiveProbabilityScaleFactor(final double scaleFactor) {
            return new ScalableBloomFilterBuilder<T>(this).withFalsePositiveProbabilityScaleFactor(scaleFactor);
        }

        public ScalableBloomFilterBuilder<T> withMaximumNumberOfBuckets(final int maximumNumberOfBuckets) {
            return new ScalableBloomFilterBuilder<T>(this).withMaximumNumberOfBuckets(maximumNumberOfBuckets);
        }

        public RollingBloomFilterBuilder<T> withExpiryStrategy(final ExpiryStrategy<T> expiryStrategy) {
            return new RollingBloomFilterBuilder<T>(this).withExpiryStrategy(expiryStrategy);
        }

        @VisibleForTesting
        RollingBloomFilterBuilder<T> withClock(final TimeService clock) {
            return new RollingBloomFilterBuilder<T>(this).withClock(clock);
        }

        public BloomFilterBuilder<T> withConcurrencyStrategy(
                final ConcurrencyStrategy strategy) {
            Reject.ifNull(strategy);
            this.concurrencyStrategy = strategy;
            return this;
        }

        public BloomFilterBuilder<T> withWriteBatchSize(final int batchSize) {
            Reject.ifFalse(batchSize >= 0, "Write batch size must be >= 0");
            this.writeBatchSize = batchSize;
            return this;
        }

        public ScalableBloomFilterBuilder<T> scalable() {
            return new ScalableBloomFilterBuilder<T>(this);
        }

        public RollingBloomFilterBuilder<T> rolling() {
            return new RollingBloomFilterBuilder<T>(this);
        }

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

    public static class ScalableBloomFilterBuilder<T> extends BloomFilterBuilder<T> {
        double capacityGrowthFactor = 2.0d;
        double falsePositiveProbabilityScaleFactor = 0.8d;
        int maxNumberOfBuckets = 64;

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
            Reject.ifNull(expiryStrategy);
            return new ConcurrentRollingBloomFilter<T>(this);
        }
    }
}
