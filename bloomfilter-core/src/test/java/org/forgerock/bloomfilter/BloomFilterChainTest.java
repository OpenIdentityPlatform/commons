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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.util.time.TimeService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BloomFilterChainTest {
    private static final BloomFilterStatistics SATURATED = new BloomFilterStatistics(0.01d, 0.02d, 1, 1, 1, 0);

    @Mock
    private BloomFilterPool<Integer> mockPool;

    @Mock
    private TimeService mockClock;

    @Mock
    private BloomFilter<Integer> mockBloomFilter;

    private BloomFilterChain<Integer> testChain;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testChain = new BloomFilterChain<Integer>(mockPool, mockClock);
    }

    @Test
    public void shouldAcquireBucketFromPoolIfEmpty() {
        // Given
        int value = 42;
        given(mockPool.nextAvailable()).willReturn(mockBloomFilter);

        // When
        testChain.add(value);

        // Then
        verify(mockPool).nextAvailable();
        verify(mockBloomFilter).add(value);
    }

    @Test
    public void shouldAcquireBucketIfLastBucketIsSaturated() {
        // Given
        @SuppressWarnings("unchecked")
        BloomFilter<Integer> newBucket = mock(BloomFilter.class);
        int value = 42;
        given(mockPool.nextAvailable()).willReturn(mockBloomFilter);
        given(mockBloomFilter.getStatistics()).willReturn(SATURATED);
        given(mockPool.nextAvailable()).willReturn(newBucket); // 2nd call

        // When
        testChain.add(value);

        // Then
        verify(newBucket).add(value);
    }

    @Test
    public void shouldReleaseExpiredAndSaturatedBuckets() {
        // Given
        int value = 42;
        given(mockPool.nextAvailable()).willReturn(mockBloomFilter);
        testChain.add(value);
        given(mockBloomFilter.getStatistics()).willReturn(SATURATED);
        given(mockClock.now()).willReturn(Long.MAX_VALUE);

        // When
        testChain.add(value+1);

        // Then
        verify(mockPool).release(mockBloomFilter);
    }
}