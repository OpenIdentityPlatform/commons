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

import static java.lang.Math.pow;
import static java.util.Collections.synchronizedMap;

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * Maintains a pool of available bloom filters whose capacities and false positive probabilities are adjusted
 * according to some geometric series. This ensures that a {@link BloomFilterChain} built from elements from this
 * pool will never exceed the desired overall false positive probability, while also ensuring that the capacity of
 * each subsequent bloom filter is increased to avoid creating long chains in cases where the required capacity has
 * been underestimated by orders of magnitude.
 * <p/>
 * A pool of entries is maintained rather than a simple counter to allow for rolling bloom filter chains, in which
 * older filters in the chain may expire. By re-using the element in the series at that point, we avoid creating
 * excessively large filters unnecessarily.
 * <p/>
 * As an implementation detail, the current implementation only pools the natural numbers representing elements in
 * the geometric series. The Bloom Filters themselves are created on-demand, with parameters according to the following
 * formulae. To create the <em>i</em>th element in the series we use:
 * <pre>
 *     capacity(i) = initialCapacity * capacityGrowthFactor^i
 *     fpp(i) = initialFalsePositiveProbability * fppScaleFactor^i
 * </pre>
 * Where the initialFalsePositiveProbability is given by <code>overallFalsePositiveProbability *
 * (1-fppScaleFactor)</code>
 * <p/>
 * The strategy used here is described in detail in the paper <a
 * href="http://www.sciencedirect.com/science/article/pii/S0020019006003127">Scalable Bloom Filters</a> by Almeida et
 * al., <em>Information Processing Letters</em>, 101(6), p.255&ndash;261, 2007. We add the ability to remove buckets
 * from the chain and later reuse them, resulting in what we call <em>Rolling Bloom Filters</em>.
 */
@ThreadSafe
final class GeometricSeriesBloomFilterPool<T> implements BloomFilterPool<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeometricSeriesBloomFilterPool.class);
    private final BloomFilterFactory<T> factory;
    private final BitSet bucketNumbers;
    private final int maxBuckets;

    private final long initialCapacity;
    private final double capacityGrowthFactor;
    private final double initialFalsePositiveProbability;
    private final double falsePositiveProbabilityScaleFactor;

    private final Map<BloomFilter<T>, Integer> takenBucketNumbers;

    /**
     * Constructs the pool with the given parameters.
     *
     * @param factory the factory to use to create new buckets.
     * @param maxBuckets the maximum number of buckets in the chain.
     * @param initialCapacity the initial capacity of the first bucket in the chain.
     * @param capacityGrowthFactor the factor by which to increase the capacity of subsequent buckets in the chain.
     * @param overallFalsePositiveProbability the overall false positive probability to maintain for the entire chain.
     * @param falsePositiveProbabilityScaleFactor the factor by which to scale false positive probability on each
     *                                            subsequent bucket in the chain (must be &gt;0 and &lt;1).
     */
    GeometricSeriesBloomFilterPool(final BloomFilterFactory<T> factory, final int maxBuckets,
                                   final long initialCapacity, final double capacityGrowthFactor,
                                   final double overallFalsePositiveProbability,
                                   final double falsePositiveProbabilityScaleFactor) {
        Reject.ifFalse(maxBuckets > 0, "Max # buckets must be > 0");
        Reject.ifNull(factory);
        Reject.ifFalse(initialCapacity > 0L, "Capacity must be > 0");
        Reject.ifFalse(capacityGrowthFactor > 0.0d, "Capacity growth factor must be > 0");
        Reject.ifFalse(overallFalsePositiveProbability > 0.0d && overallFalsePositiveProbability < 1.0d,
                "Overall false positive probability must be > 0 and < 1");
        Reject.ifFalse(falsePositiveProbabilityScaleFactor > 0.0d && falsePositiveProbabilityScaleFactor < 1.0d,
                "False positive probability scale factor must be > 0 and < 1");

        final int initialNumberOfBuckets = Math.min(maxBuckets, 256); // Limit size initially


        this.factory = factory;
        this.maxBuckets = maxBuckets;
        this.bucketNumbers = new BitSet(initialNumberOfBuckets);

        this.initialCapacity = initialCapacity;
        this.capacityGrowthFactor = capacityGrowthFactor;
        this.initialFalsePositiveProbability =
                overallFalsePositiveProbability * (1.0d - falsePositiveProbabilityScaleFactor);
        this.falsePositiveProbabilityScaleFactor = falsePositiveProbabilityScaleFactor;
        this.takenBucketNumbers = synchronizedMap(new IdentityHashMap<BloomFilter<T>, Integer>(initialNumberOfBuckets));

        LOGGER.debug("Constructed GeometricSeriesBloomFilterPool: {}", this);
    }

    /**
     * Returns the next available BloomFilter from the pool. Subsequent calls will yield progressively larger and
     * more accurate bloom filters according to the geometric series defined by the pool parameters, until bloom
     * filters are released. Once filters have been released back to the pool, then the pool will reuse the capacity
     * and false positive probability of the released pool in preference to any higher values.
     *
     * @return the next available bloom filter according to the geometric series.
     * @throws NoSuchElementException if the maximum number of buckets has been exceeded.
     */
    @Override
    public BloomFilter<T> nextAvailable() {
        final int bucketNumber;
        synchronized (bucketNumbers) {
            bucketNumber = bucketNumbers.nextClearBit(0);
            if (bucketNumber >= maxBuckets) {
                throw new NoSuchElementException("Maximum number of buckets exceeded: " + maxBuckets);
            }
            bucketNumbers.set(bucketNumber);
        }

        final long capacity = (long) (initialCapacity * pow(capacityGrowthFactor, bucketNumber));
        final double fpp =
                initialFalsePositiveProbability * pow(falsePositiveProbabilityScaleFactor, bucketNumber);

        LOGGER.debug("Creating BloomFilter number {} with capacity={}, fpp={}", bucketNumber, capacity, fpp);

        final BloomFilter<T> bucket = factory.create(capacity, fpp);
        takenBucketNumbers.put(bucket, bucketNumber);

        return bucket;
    }

    /**
     * Releases a Bloom Filter back into the pool.
     *
     * @param released the bloom filter to release back to the pool.
     */
    @Override
    public void release(BloomFilter<T> released) {
        final int bucketNumber = takenBucketNumbers.get(released);
        LOGGER.debug("Releasing bucket number {}", bucketNumber);
        synchronized (bucketNumbers) {
            bucketNumbers.clear(bucketNumber);
        }
    }

    @Override
    public double getOverallFalsePositiveProbability() {
        return initialFalsePositiveProbability / (1.0d - falsePositiveProbabilityScaleFactor);
    }

    @Override
    public String toString() {
        return "BloomFilterPool{" +
                "maxBuckets=" + maxBuckets +
                ", initialCapacity=" + initialCapacity +
                ", capacityGrowthFactor=" + capacityGrowthFactor +
                ", initialFalsePositiveProbability=" + initialFalsePositiveProbability +
                ", falsePositiveProbabilityScaleFactor=" + falsePositiveProbabilityScaleFactor +
                ", takenBucketNumbers=" + new TreeSet<Integer>(takenBucketNumbers.values()) +
                '}';
    }
}
