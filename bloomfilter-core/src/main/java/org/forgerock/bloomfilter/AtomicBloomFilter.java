/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

// Portions Copyrighted 2015 ForgeRock AS.

package org.forgerock.bloomfilter;

import org.forgerock.guava.common.hash.Funnel;
import org.forgerock.guava.common.hash.Hashing;
import org.forgerock.guava.common.primitives.Longs;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * A BloomFilter that stores elements in an {@link AtomicLongArray} and uses atomic compare-and-swap operations to
 * ensure thread safety.
 */
@ThreadSafe
final class AtomicBloomFilter<T> implements BloomFilter<T> {
    private static final int BITS_PER_WORD = 64;

    private final long capacity;
    private final double falsePositiveProbability;
    private final AtomicLongArray bits;
    private final AtomicLong bitCount = new AtomicLong(0);
    private final Funnel<? super T> funnel;
    private final int numHashFunctions;
    private final long bitSize;

    /**
     * Constructs an atomic bloom filter with the given parameters.
     *
     * @param funnel the funnel to use for serialising objects for hashing.
     * @param capacity the desired capacity of the bloom filter.
     * @param falsePositiveProbability the overall false positive probability to maintain.
     */
    AtomicBloomFilter(final Funnel<? super T> funnel,
                      final long capacity,
                      final double falsePositiveProbability) {
        this.funnel = funnel;
        this.capacity = capacity;
        this.falsePositiveProbability = falsePositiveProbability;

        this.bitSize = BloomFilterStatistics.optimumBitSize(capacity, falsePositiveProbability);
        int arraySize = (int) Math.ceil((double) bitSize / (double) BITS_PER_WORD);
        this.bits = new AtomicLongArray(arraySize);
        this.numHashFunctions = BloomFilterStatistics.optimalNumberOfHashFunctions(bitSize, capacity);
    }

    @Override
    public void add(final T element) {
        // Implementation derived from Guava MURMUR128_MITZ_64 in BloomFilterStrategies
        byte[] hashBytes = Hashing.murmur3_128().hashObject(element, funnel).asBytes();
        long hash1 = lowerEight(hashBytes);
        long hash2 = upperEight(hashBytes);

        long combinedHash = hash1;

        for (int i = 1; i <= numHashFunctions; ++i) {
            set((combinedHash & Long.MAX_VALUE) % bitSize);
            combinedHash += hash2;
        }
    }


    @Override
    public void addAll(final Collection<? extends T> elements) {
        for (T element : elements) {
            add(element);
        }
    }

    @Override
    public boolean mightContain(final T element) {
        // Implementation derived from Guava MURMUR128_MITZ_64 in BloomFilterStrategies
        byte[] hashBytes = Hashing.murmur3_128().hashObject(element, funnel).asBytes();
        long hash1 = lowerEight(hashBytes);
        long hash2 = upperEight(hashBytes);

        long combinedHash = hash1;

        for (int i = 1; i <= numHashFunctions; ++i) {
            if (!get((combinedHash & Long.MAX_VALUE) % bitSize)) {
                return false;
            }
            combinedHash += hash2;
        }

        return true;
    }

    double expectedFpp() {
        return Math.pow((double)bitCount.get() / (double)bitSize, (double)this.numHashFunctions);

    }

    @Override
    public BloomFilterStatistics getStatistics() {
        double expectedFpp = expectedFpp();
        // Estimate the current cardinality of the bloom filter
        long cardinality = (long) -((bitSize * Math.log(1.0d - bitCount.get()/(double)bitSize)) / numHashFunctions);

        return new BloomFilterStatistics(falsePositiveProbability, expectedFpp, capacity, bitSize, Long.MAX_VALUE,
                capacity - cardinality);
    }

    /**
     * Atomically sets the bit at the given index.
     *
     * @param index the index of the bit to set.
     * @return true if the bits changed as a result of setting this index.
     */
    boolean set(long index) {
        // Based on Guava BloomFilterStrategies.BitArray, but adapted to AtomicLongArray.
        boolean changed;
        long prev, next;
        int bucket = (int) (index >>> 6);
        do {
            prev = this.bits.get(bucket);
            next = prev | 1L << (int) index;
            changed = (prev != next);
        } while (changed && !bits.compareAndSet(bucket, prev, next));

        if (changed) {
            bitCount.incrementAndGet();
        }

        return changed;
    }

    /**
     * Atomically tests the bit at the given index.
     * @param index the index of the bit to test.
     * @return whether the bit at the index is set or not.
     */
    boolean get(long index) {
        // Based on Guava BloomFilterStrategies.BitArray, but adapted to AtomicLongArray.
        return (bits.get((int) (index >>> 6)) & 1L << (int) index) != 0L;
    }

    private long lowerEight(byte[] bytes) {
        return Longs.fromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
    }

    private long upperEight(byte[] bytes) {
        return Longs.fromBytes(bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
    }
}
