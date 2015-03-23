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

import org.forgerock.guava.common.hash.Funnel;
import org.forgerock.guava.common.hash.Funnels;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;

public class AtomicBloomFilterTest {
    private static final Funnel<Integer> FUNNEL = Funnels.integerFunnel();
    private static final long CAPACITY = 100;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.02d;

    private AtomicBloomFilter<Integer> bloomFilter;

    @BeforeClass
    public void createBloomFilter() {
        bloomFilter = new AtomicBloomFilter<Integer>(FUNNEL, CAPACITY, FALSE_POSITIVE_PROBABILITY);
    }

    @Test
    public void shouldUseSpecifiedFalsePositiveProbability() {
        assertThat(bloomFilter.getStatistics().getConfiguredFalsePositiveProbability())
                .isEqualTo(FALSE_POSITIVE_PROBABILITY);
    }

    @Test
    public void shouldUseSpecifiedCapacity() {
        assertThat(bloomFilter.getStatistics().getCapacity()).isEqualTo(CAPACITY);
    }

    @Test(dataProvider = "randomInts", invocationCount = 16, threadPoolSize = 16)
    public void shouldNotLoseUpdates(int value) {
        bloomFilter.add(value);
        assertThat(bloomFilter.mightContain(value)).isTrue();
    }

    @DataProvider
    public Object[][] randomInts() {
        final Random random = new Random();
        final Object[][] results = new Object[100][1];
        for (int i = 0; i < results.length; ++i) {
            results[i][0] = random.nextInt();
        }
        return results;
    }
}