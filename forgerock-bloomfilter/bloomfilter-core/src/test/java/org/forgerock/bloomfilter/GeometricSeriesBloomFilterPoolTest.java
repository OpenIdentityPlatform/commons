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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GeometricSeriesBloomFilterPoolTest {
    private static final int MAX_BUCKETS = 4;
    private static final long INITIAL_CAPACITY = 100l;
    private static final double CAPACITY_GROWTH_FACTOR = 2.0d;
    private static final double OVERALL_FPP = 0.01d;
    private static final double FPP_SCALE_FACTOR = 0.6d;

    private static final double EXPECTED_INITIAL_FPP = OVERALL_FPP * (1.0d - FPP_SCALE_FACTOR);

    @Mock
    private BloomFilterFactory<Integer> mockFactory;

    @Mock
    private BloomFilter<Integer> mockBloomFilter;

    private GeometricSeriesBloomFilterPool<Integer> pool;

    @BeforeMethod
    public void createPool() {
        MockitoAnnotations.initMocks(this);
        pool = new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY,
                CAPACITY_GROWTH_FACTOR, OVERALL_FPP, FPP_SCALE_FACTOR);

        given(mockFactory.create(anyLong(), anyDouble())).willReturn(mockBloomFilter);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullFactory() {
        new GeometricSeriesBloomFilterPool<Integer>(null, MAX_BUCKETS, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroMaxBuckets() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, 0, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeMaxBuckets() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, -42, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroInitialCapacity() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, 0, CAPACITY_GROWTH_FACTOR,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeInitialCapacity() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, -42, CAPACITY_GROWTH_FACTOR,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroCapacityGrowthFactor() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, 0.0,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeCapacityGrowthFactor() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, -42.0,
                OVERALL_FPP, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroOverallFalsePositiveProbability() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                0.0, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeOverallFalsePositiveProbability() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                -42.0, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectCertainOverallFalsePositiveProbability() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                1.0, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectGreaterThanCertainOverallFalsePositiveProbability() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY, CAPACITY_GROWTH_FACTOR,
                42.0, FPP_SCALE_FACTOR);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroFalsePositiveProbabilityScaleFactor() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY,
                CAPACITY_GROWTH_FACTOR, OVERALL_FPP, 0.0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeFalsePositiveProbabilityScaleFactor() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY,
                CAPACITY_GROWTH_FACTOR, OVERALL_FPP, -42.0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectGreaterThanOneFalsePositiveProbabilityScaleFactor() {
        new GeometricSeriesBloomFilterPool<Integer>(mockFactory, MAX_BUCKETS, INITIAL_CAPACITY,
                CAPACITY_GROWTH_FACTOR, OVERALL_FPP, 42.0);
    }

    @Test
    public void shouldReportCorrectOverallFalsePositiveProbability() {
        assertThat(pool.getOverallFalsePositiveProbability()).isEqualTo(OVERALL_FPP);
    }

    @Test
    public void shouldCreateFirstBucketCorrectly() {
        pool.nextAvailable();
        verify(mockFactory).create(INITIAL_CAPACITY, EXPECTED_INITIAL_FPP);
    }

    @Test
    public void shouldScaleSubsequentBucketsAccordingToFactors() {
        pool.nextAvailable();
        verify(mockFactory).create(INITIAL_CAPACITY, EXPECTED_INITIAL_FPP);
        pool.nextAvailable();
        verify(mockFactory).create((long)(INITIAL_CAPACITY * CAPACITY_GROWTH_FACTOR),
                EXPECTED_INITIAL_FPP * FPP_SCALE_FACTOR);
        pool.nextAvailable();
        verify(mockFactory).create((long)(INITIAL_CAPACITY * CAPACITY_GROWTH_FACTOR * CAPACITY_GROWTH_FACTOR),
                EXPECTED_INITIAL_FPP * FPP_SCALE_FACTOR * FPP_SCALE_FACTOR);
    }

    @Test
    public void shouldReuseReleasedBuckets() {
        BloomFilter<Integer> bf = pool.nextAvailable();
        pool.release(bf);
        pool.nextAvailable();
        // Should re-use the initial bucket stats
        verify(mockFactory, times(2)).create(INITIAL_CAPACITY, EXPECTED_INITIAL_FPP);
    }
}