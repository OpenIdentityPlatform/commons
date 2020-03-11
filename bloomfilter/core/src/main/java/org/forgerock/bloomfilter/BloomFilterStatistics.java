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

import javax.annotation.concurrent.Immutable;
import java.util.Locale;

/**
 * Provides a snapshot of the current statistics and configuration of a Bloom Filter implementation.
 */
@Immutable
public final class BloomFilterStatistics {
    private final double configuredFalsePositiveProbability;
    private final double expectedFalsePositiveProbability;
    private final long capacity;
    private final long bitSize;
    private final long expiryTime;
    private final long estimatedRemainingCapacity;

    /**
     * Constructs a statistics object with the given parameters.
     *
     * @param configuredFalsePositiveProbability the false positive probability that was configured for this set.
     * @param expectedFalsePositiveProbability the expected false positive probability given the current state of the
     *                                         set.
     * @param capacity the expected total number of insertions into the set before it becomes saturated
     *                 (exceeds the configured false positive probability).
     * @param bitSize the total memory size of the set in bits.
     */
    public BloomFilterStatistics(final double configuredFalsePositiveProbability,
                                 final double expectedFalsePositiveProbability,
                                 final long capacity,
                                 final long bitSize,
                                 final long expiryTime,
                                 final long estimatedRemainingCapacity) {
        this.configuredFalsePositiveProbability = configuredFalsePositiveProbability;
        this.expectedFalsePositiveProbability = expectedFalsePositiveProbability;
        this.capacity = capacity;
        this.bitSize = bitSize;
        this.expiryTime = expiryTime;
        this.estimatedRemainingCapacity = estimatedRemainingCapacity;
    }

    /**
     * The configured probability of false positives.
     *
     * @return the false positive probability (FPP) between 0 and 1.
     */
    public double getConfiguredFalsePositiveProbability() {
        return configuredFalsePositiveProbability;
    }

    /**
     * The expected probability of false positives given the current state of the set.
     *
     * @return the expected false positive probability (FPP) between 0 and 1.
     */
    public double getExpectedFalsePositiveProbability() {
        return expectedFalsePositiveProbability;
    }

    /**
     * The capacity of the set. This is the expected total number of elements that can be inserted into the
     * set before it exceeds the configured false positive probability.
     *
     * @return the expected capacity of the set.
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * The total memory size of the set, in bits. This size is a guide to the memory used for storing the actual
     * elements of the set, and may not reflect fixed-cost overheads such as the size of object headers,
     * configuration options, etc.
     *
     * @return the total memory size of the set in bits.
     */
    public long getBitSize() {
        return bitSize;
    }

    /**
     * Calculates the optimum size of a bloom filter (in bits) to achieve the given false positive probability and
     * expected number of insertions (capacity).
     *
     * @param capacity the expected number of insertions into the bloom filter.
     * @param falsePositiveProbability the desired probability of false positives.
     * @return the optimum number of bits to use for the bloom filter.
     */
    public static long optimumBitSize(final long capacity, final double falsePositiveProbability) {
        return (long)((double)(-capacity) * Math.log(falsePositiveProbability) / (Math.log(2.0D) * Math.log(2.0D)));
    }

    /**
     * Provides an estimate of the remaining capacity in this bloom filter before it would become saturated.
     */
    public long getEstimatedRemainingCapacity() {
        return estimatedRemainingCapacity;
    }


    /**
     * Estimates the remaining capacity in an optimum Bloom Filter. This is
     * assuming that the Bloom Filter is close to optimal in space efficiency for the configured false positive
     * probability.
     *
     * @param bitSize the size of the Bloom Filter bit-vector in bits.
     * @param expectedFalsePositiveProbability the expected current false positive probability of the bloom filter.
     * @param capacity the overall expected capacity of the bloom filter.
     * @return an estimate of the number of elements that could be inserted before the bloom filter becomes saturated.
     */
    public static long optimumRemainingCapacity(final long bitSize, final double expectedFalsePositiveProbability,
                                                final long capacity) {

        // Calculate the number of hash functions used by this bloom filter (assuming optimal).
        int h = optimalNumberOfHashFunctions(bitSize, capacity);

        // Reverse engineer the number of bits set to 1 in the underlying bit-vector:
        double oneBits = Math.pow(expectedFalsePositiveProbability, 1.0d/(double)h) * bitSize;

        // Estimate the cardinality (number of elements) of the set represented by this bloom filter. See
        // http://en.wikipedia.org/wiki/Bloom_filter#Approximating_the_number_of_items_in_a_Bloom_filter
        long cardinalityEstimate = (long) -((bitSize * Math.log(1.0d - oneBits/(double)bitSize)) / h);

        // Estimated remaining capacity is then just the overall capacity minus the current cardinality
        return capacity - cardinalityEstimate;
    }

    /**
     * Calculates the optimal number of hash functions to use to represent a given number of elements in a given
     * sized bit-vector.
     *
     * @param bitSize the size of the bit-vector.
     * @param capacity the total number of elements to represent.
     * @return the optimum number of hash functions to use per element.
     */
    static int optimalNumberOfHashFunctions(long bitSize, long capacity) {
        return Math.max(1, (int) Math.round((double)bitSize / (double) capacity * Math.log(2.0d)));
    }


    /**
     * The time in milliseconds since the UTC epoch until the last element contained in this bloom filter expires.
     * This statistic is only relevant to expiring or rolling bloom filter implementations.
     *
     * @return the latest expiry time in milliseconds from the UTC epoch.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Determines whether the bloom filter expected false positive probability has exceeded the configured false
     * positive probability. This happens roughly when half of the bits in the underlying bit vector have been set to 1.
     * No more elements should be inserted into the Bloom Filter beyond this point to avoid exceeding the configured
     * probability of false positives.
     *
     * @return {@code true} if the expected false positive probability is greater than the configured probability.
     */
    public boolean isSaturated() {
        return expectedFalsePositiveProbability >= configuredFalsePositiveProbability;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BloomFilterStatistics that = (BloomFilterStatistics) o;

        return bitSize == that.bitSize && capacity == that.capacity
                && expiryTime == that.expiryTime && estimatedRemainingCapacity == that.estimatedRemainingCapacity
                && Double.compare(that.configuredFalsePositiveProbability, configuredFalsePositiveProbability) == 0
                && Double.compare(that.expectedFalsePositiveProbability, expectedFalsePositiveProbability) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(configuredFalsePositiveProbability);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(expectedFalsePositiveProbability);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (bitSize ^ (bitSize >>> 32));
        result = 31 * result + (int) (expiryTime ^ (expiryTime >>> 32));
        result = 31 * result + (int) (estimatedRemainingCapacity ^ (estimatedRemainingCapacity >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "{ \"configuredFalsePositiveProbability\": %.6f" +
                ", \"expectedFalsePositiveProbability\": %.6f" +
                ", \"capacity\": %d" +
                ", \"estimatedRemainingCapacity\": %d" +
                ", \"bitSize\": %d" +
                ", \"expiryTime\": \"%tFT%<tT.%<tLZ\" }",
                configuredFalsePositiveProbability, expectedFalsePositiveProbability, capacity,
                estimatedRemainingCapacity, bitSize, expiryTime);
    }
}
