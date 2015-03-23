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

import junit.framework.Assert;
import org.forgerock.guava.common.hash.Funnels;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentRollingBloomFilterTest {

    private static final int NUM_THREADS = 16;

    private volatile ExecutorService executorService;

    @BeforeMethod
    public void createThreadPool() {
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @AfterMethod
    public void destroyThreadPool() {
        executorService.shutdownNow();
    }

    @DataProvider
    public static Object[][] implementations() {
        return new Object[][] {
                {ConcurrencyStrategy.COPY_ON_WRITE},
                {ConcurrencyStrategy.SYNCHRONIZED},
                {ConcurrencyStrategy.ATOMIC}
        };
    }

    @Test(dataProvider = "implementations", invocationCount = 3, invocationTimeOut = 60000)
    public void testConcurrentWritePerformance(final ConcurrencyStrategy strategy) throws
            Exception {
        final BloomFilter<String> impl = BloomFilters.<String>create(Funnels
                .unencodedCharsFunnel())
                .withConcurrencyStrategy(strategy)
                .withWriteBatchSize(500)
                .withInitialCapacity(10000)
                .withCapacityGrowthFactor(2.0d)
                .withFalsePositiveProbabilityScaleFactor(0.5d)
                .scalable()
                .build();

        final CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS + 1);
        for (int i = 0; i < NUM_THREADS; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Warmup
                        barrier.await();
                        for (int i = 0; i < 50000; ++i) {
                            impl.add("Test" + i);
                        }

                        barrier.await();
                        for (int i = 0; i < 50000; ++i) {
                            impl.add("Test" + i);
                        }
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                        barrier.reset();
                    }
                }
            });
        }

        // Warmup
        barrier.await();
        // Start the test
        barrier.await();
        final long start = System.currentTimeMillis();
        barrier.await();
        final long end = System.currentTimeMillis();

        Thread.sleep(50);

        System.out.printf("Write Time: %s -> %dms%n", impl, (end - start));

        for (int i = 0; i < 50000; ++i) {
            Assert.assertTrue(impl.mightContain("Test" + i));
        }
    }

    @Test(dataProvider = "implementations", invocationCount = 3, invocationTimeOut = 60000)
    public void testConcurrentReadPerformance(final ConcurrencyStrategy strategy) throws
            Exception {

        final BloomFilter<String> impl = BloomFilters.<String>create(Funnels
                .unencodedCharsFunnel())
                .withConcurrencyStrategy(strategy)
                .withWriteBatchSize(500)
                .withInitialCapacity(10000)
                .withCapacityGrowthFactor(2.0d)
                .withFalsePositiveProbabilityScaleFactor(0.5d)
                .scalable()
                .build();

        // Given
        final Random random = new Random();
        for (int i = 0; i < 50000; ++i) {
            impl.add("Test" + random.nextInt(100000));
        }

        final CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS + 1);
        for (int i = 0; i < NUM_THREADS; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Warmup
                        barrier.await();
                        for (int i = 0; i < 50000; ++i) {
                            impl.mightContain("Test" + i);
                        }

                        barrier.await();
                        for (int i = 0; i < 50000; ++i) {
                            impl.mightContain("Test" + i);
                        }
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Warmup
        barrier.await();
        // Start the test
        barrier.await();
        final long start = System.currentTimeMillis();
        barrier.await();
        final long end = System.currentTimeMillis();

        System.out.printf("Read Time: %s -> %dms%n", impl, (end - start));
    }


}