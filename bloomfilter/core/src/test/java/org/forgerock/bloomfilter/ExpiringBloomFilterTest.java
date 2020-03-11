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
import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class ExpiringBloomFilterTest {

    @Mock
    private BloomFilter<Integer> mockDelegate;

    @Mock
    private ExpiryStrategy<Integer> mockExpiryStrategy;

    private ExpiringBloomFilter<Integer> bloomFilter;

    @BeforeMethod
    public void createBloomFilter() {
        MockitoAnnotations.initMocks(this);
        bloomFilter = new ExpiringBloomFilter<Integer>(mockDelegate, mockExpiryStrategy);
    }

    @Test
    public void shouldDelegateAddCalls() {
        bloomFilter.add(42);
        verify(mockDelegate).add(42);
    }

    @Test
    public void shouldDelegateAddAllCalls() {
        List<Integer> values = Arrays.asList(1, 2, 3, 4);
        bloomFilter.addAll(values);
        verify(mockDelegate).addAll(values);
    }

    @Test
    public void shouldDelegateMightContainCalls() {
        given(mockExpiryStrategy.expiryTime(42)).willReturn(3l);
        bloomFilter.add(42);
        bloomFilter.mightContain(42);
        verify(mockDelegate).mightContain(42);
    }

    @Test
    public void shouldUpdateLatestExpiryTimeWhenNotSet() {
        given(mockDelegate.getStatistics()).willReturn(new BloomFilterStatistics(0.01, 0.01, 100, 100, 0, 0));
        given(mockExpiryStrategy.expiryTime(42)).willReturn(3l);
        bloomFilter.add(42);
        assertThat(bloomFilter.getStatistics().getExpiryTime()).isEqualTo(3l);
    }

    @Test
    public void shouldUpdateLatestExpiryTime() {
        given(mockDelegate.getStatistics()).willReturn(new BloomFilterStatistics(0.01, 0.01, 100, 100, 0, 0));
        given(mockExpiryStrategy.expiryTime(42)).willReturn(3l);
        given(mockExpiryStrategy.expiryTime(43)).willReturn(4l);
        bloomFilter.add(43);
        bloomFilter.add(42);
        assertThat(bloomFilter.getStatistics().getExpiryTime()).isEqualTo(4l);
    }

    @Test
    public void shouldUpdateLatestExpiryTimeAddAll() {
        given(mockDelegate.getStatistics()).willReturn(new BloomFilterStatistics(0.01, 0.01, 100, 100, 0, 0));
        given(mockExpiryStrategy.expiryTime(42)).willReturn(3l);
        given(mockExpiryStrategy.expiryTime(43)).willReturn(4l);
        bloomFilter.addAll(Arrays.asList(43, 42));
        assertThat(bloomFilter.getStatistics().getExpiryTime()).isEqualTo(4l);
    }
}