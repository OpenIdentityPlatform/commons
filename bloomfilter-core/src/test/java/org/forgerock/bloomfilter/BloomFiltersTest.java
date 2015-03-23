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
import static org.forgerock.guava.common.hash.Funnels.integerFunnel;
import static org.mockito.Mockito.mock;

import org.forgerock.guava.common.hash.Funnel;
import org.forgerock.util.time.TimeService;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BloomFiltersTest {

    @Test
    public void shouldHaveSensibleDefaults() {
        // Given
        Funnel<Integer> funnel = integerFunnel();

        // When
        final BloomFilters.BloomFilterBuilder<Integer> builder = BloomFilters.create(funnel);

        // Then
        checkCommonDefaults(builder, funnel);
    }

    @Test
    public void shouldHaveSensibleScalingDefaults() {
        // Given
        Funnel<Integer> funnel = integerFunnel();

        // When
        final BloomFilters.ScalableBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(funnel)
                .scalable();

        // Then
        checkCommonDefaults(builder, funnel);
        assertThat(builder.capacityGrowthFactor).as("capacityGrowthFactor").isGreaterThan(1.0d)
                .isLessThanOrEqualTo(3.0d);
        assertThat(builder.falsePositiveProbabilityScaleFactor).as("falsePositiveProbabilityScaleFactor")
                .isGreaterThan(0.0d).isLessThanOrEqualTo(1.0d);
        assertThat(builder.maxNumberOfBuckets).as("maxNumberOfBuckets").isGreaterThan(1);
    }

    @Test
    public void shouldHaveSensibleRollingDefaults() {
        // Given
        Funnel<Integer> funnel = integerFunnel();

        // When
        final BloomFilters.RollingBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(funnel).rolling();

        // Then
        checkCommonDefaults(builder, funnel);
        assertThat(builder.capacityGrowthFactor).as("capacityGrowthFactor").isGreaterThan(1.0d)
                .isLessThanOrEqualTo(3.0d);
        assertThat(builder.falsePositiveProbabilityScaleFactor).as("falsePositiveProbabilityScaleFactor")
                .isGreaterThan(0.0d).isLessThanOrEqualTo(1.0d);
        assertThat(builder.maxNumberOfBuckets).as("maxNumberOfBuckets").isGreaterThan(1);
        assertThat(builder.clock).as("clock").isSameAs(TimeService.SYSTEM);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "No expiry strategy specified")
    public void shouldRequireExplicitExpiryStrategyForRollingBloomFilters() {
        BloomFilters.create(integerFunnel()).rolling().build();
    }

    @Test
    public void shouldUseSpecifiedInitialCapacity() {
        // Given
        int initialCapacity = 42;

        // When
        BloomFilter<Integer> bf = BloomFilters.<Integer>create(integerFunnel()).withInitialCapacity(initialCapacity)
                .build();

        // Then
        assertThat(bf.getStatistics().getCapacity()).isEqualTo(initialCapacity);
    }

    @Test
    public void shouldUseSpecifiedFalsePositiveProbability() {
        // Given
        double falsePositiveProbability = 0.42d;

        // When
        BloomFilter<Integer> bf = BloomFilters.<Integer>create(integerFunnel())
                .withFalsePositiveProbability(falsePositiveProbability).build();

        // Then
        assertThat(bf.getStatistics().getConfiguredFalsePositiveProbability()).isEqualTo(falsePositiveProbability);
    }

    @Test
    public void shouldUseSpecifiedCapacityGrowthFactor() {
        // Given
        double growthFactor = 4.2d;

        // When
        BloomFilters.ScalableBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(integerFunnel())
                .withCapacityGrowthFactor(growthFactor);

        // Then
        assertThat(builder.capacityGrowthFactor).isEqualTo(growthFactor);
    }

    @Test
    public void shouldUseSpecifiedFalsePositiveProbabilityScaleFactor() {
        // Given
        double fppScaleFactor = 0.3d;

        // When
        BloomFilters.ScalableBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(integerFunnel())
                .withFalsePositiveProbabilityScaleFactor(fppScaleFactor);

        // Then
        assertThat(builder.falsePositiveProbabilityScaleFactor).isEqualTo(fppScaleFactor);
    }

    @Test
    public void shouldUseSpecifiedWriteBatchSize() {
        // Given
        int batchSize = 42;

        // When
        BloomFilter<Integer> bf = BloomFilters.<Integer>create(integerFunnel())
                .withWriteBatchSize(batchSize).build();

        // Then
        assertThat(bf).isInstanceOf(BatchingBloomFilter.class);
        assertThat(((BatchingBloomFilter)bf).batchSize).isEqualTo(batchSize);
    }

    @Test
    public void shouldUseSpecifiedMaxNumberOfBuckets() {
        // Given
        int max = 31;

        // When
        BloomFilters.ScalableBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(integerFunnel())
                .withMaximumNumberOfBuckets(max);

        // Then
        assertThat(builder.maxNumberOfBuckets).isEqualTo(max);
    }

    @Test
    public void shouldUseSpecifiedExpiryStrategy() {
        // Given
        @SuppressWarnings("unchecked")
        ExpiryStrategy<Integer> strategy = mock(ExpiryStrategy.class);

        // When
        BloomFilters.RollingBloomFilterBuilder<Integer> builder = BloomFilters.<Integer>create(integerFunnel())
                .withExpiryStrategy(strategy);

        // Then
        assertThat(builder.expiryStrategy).isSameAs(strategy);
    }

    @Test(dataProvider = "concurrencyStrategies")
    public void shouldUseSpecifiedConcurrencyStrategy(ConcurrencyStrategy strategy) {
        BloomFilter<Integer> bf = BloomFilters.<Integer>create(integerFunnel())
                .withConcurrencyStrategy(strategy).build();

        // Is there a better way of testing this?
        switch (strategy) {
            case SYNCHRONIZED:
                assertThat(bf).isInstanceOf(SynchronizedBloomFilter.class);
                break;
            case COPY_ON_WRITE:
                assertThat(bf).isInstanceOf(CopyOnWriteBloomFilter.class);
                break;
            case ATOMIC:
                assertThat(bf).isInstanceOf(AtomicBloomFilter.class);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullFunnel() {
        BloomFilters.create(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroCapacity() {
        BloomFilters.create(integerFunnel()).withInitialCapacity(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeCapacity() {
        BloomFilters.create(integerFunnel()).withInitialCapacity(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroFalsePositiveProbability() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbability(0.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeFalsePositiveProbability() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbability(-1.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectFalsePositiveCertainty() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbability(1.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectFalsePositiveProbabilitiesGreaterThan1() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbability(1.1d);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullConcurrencyStrategy() {
        BloomFilters.create(integerFunnel()).withConcurrencyStrategy(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeWriteBatchSize() {
        BloomFilters.create(integerFunnel()).withWriteBatchSize(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectCapacityGrowthFactorLessThan1() {
        BloomFilters.create(integerFunnel()).withCapacityGrowthFactor(0.9d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroFalsePositiveProbabilityScaleFactor() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbabilityScaleFactor(0.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeFalsePositiveProbabilityScaleFactor() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbabilityScaleFactor(-1.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectFalsePositiveProbabilityScaleFactorOf1() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbabilityScaleFactor(1.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectFalsePositiveProbabilityScaleFactorsGreaterThan1() {
        BloomFilters.create(integerFunnel()).withFalsePositiveProbabilityScaleFactor(2.0d);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectZeroMaxBuckets() {
        BloomFilters.create(integerFunnel()).withMaximumNumberOfBuckets(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNegativeMaxBuckets() {
        BloomFilters.create(integerFunnel()).withMaximumNumberOfBuckets(-1);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullExpiryStrategy() {
        BloomFilters.create(integerFunnel()).withExpiryStrategy(null);
    }

    @DataProvider
    public Object[][] concurrencyStrategies() {
        return new Object[][] {
                { ConcurrencyStrategy.COPY_ON_WRITE },
                { ConcurrencyStrategy.SYNCHRONIZED },
                { ConcurrencyStrategy.ATOMIC }
        };
    }

    private void checkCommonDefaults(BloomFilters.BloomFilterBuilder<Integer> builder, Funnel<Integer> funnel) {
        assertThat(builder.falsePositiveProbability).as("falsePositiveProbability").isBetween(0.001d, 0.1d);
        assertThat(builder.initialCapacity).as("initialCapacity").isBetween(100, 100000);
        assertThat(builder.funnel).as("funnel").isSameAs(funnel);
        assertThat(builder.concurrencyStrategy).as("concurrencyStrategy").isNotNull();
        assertThat(builder.writeBatchSize).as("writeBatchSize").isBetween(0, 10000);
    }

}