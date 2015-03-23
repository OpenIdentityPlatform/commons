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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ConcurrencyStrategyTest {

    @Test
    public void shouldUseCorrectImplementations() {
        // Given
        Funnel<Integer> funnel = Funnels.integerFunnel();
        long capacity = 100;
        double fpp = 0.01d;

        // When
        BloomFilter<Integer> cowBf = ConcurrencyStrategy.COPY_ON_WRITE.<Integer>getFactory(funnel)
                .create(capacity, fpp);
        BloomFilter<Integer> syncBf = ConcurrencyStrategy.SYNCHRONIZED.<Integer>getFactory(funnel)
                .create(capacity, fpp);
        BloomFilter<Integer> atomicBf = ConcurrencyStrategy.ATOMIC.<Integer>getFactory(funnel)
                .create(capacity, fpp);

        // Then
        assertThat(cowBf).isInstanceOf(CopyOnWriteBloomFilter.class);
        assertThat(syncBf).isInstanceOf(SynchronizedBloomFilter.class);
        assertThat(atomicBf).isInstanceOf(AtomicBloomFilter.class);
    }

    @Test(dataProvider = "strategies")
    public void shouldUseSpecifiedCapacityAndFalsePositiveProbability(ConcurrencyStrategy strategy) {
        // Given
        Funnel<Integer> funnel = Funnels.integerFunnel();
        long capacity = 100;
        double fpp = 0.01d;


        // When
        BloomFilter<Integer> result = strategy.<Integer>getFactory(funnel).create(capacity, fpp);

        // Then
        assertThat(result.getStatistics().getCapacity()).as("capacity").isEqualTo(capacity);
        assertThat(result.getStatistics().getConfiguredFalsePositiveProbability()).as("falsePositiveProbability")
                .isEqualTo(fpp);
    }

    @DataProvider
    public Object[][] strategies() {
        return new Object[][] {
                { ConcurrencyStrategy.COPY_ON_WRITE },
                { ConcurrencyStrategy.SYNCHRONIZED },
                { ConcurrencyStrategy.ATOMIC }
        };
    }
}