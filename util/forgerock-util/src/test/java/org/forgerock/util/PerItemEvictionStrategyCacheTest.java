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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.newRuntimeExceptionPromise;
import static org.forgerock.util.time.Duration.UNLIMITED;
import static org.forgerock.util.time.Duration.duration;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.forgerock.util.promise.Promise;
import org.forgerock.util.time.Duration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class PerItemEvictionStrategyCacheTest {

    private static final Duration DEFAULT_CACHE_TIMEOUT = duration("30 seconds");

    private PerItemEvictionStrategyCache<Integer, Integer> cache;

    @Mock
    private ScheduledExecutorService executorService;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        cache = new PerItemEvictionStrategyCache<>(executorService, DEFAULT_CACHE_TIMEOUT);
    }

    @Test
    public void shouldMonitorCacheContent() throws Exception {
        assertThat(cache.isEmpty()).isTrue();
        assertThat(cache.size()).isEqualTo(0);

        cache.getValue(42, callable());

        assertThat(cache.isEmpty()).isFalse();
        assertThat(cache.size()).isEqualTo(1);

        cache.clear();

        assertThat(cache.isEmpty()).isTrue();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    public void shouldNotComputeMoreThanOnce() throws Exception {
        final Callable<Integer> callable = spy(callable());

        final int numberOfThreads = 2;
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    latch.countDown();
                    try {
                        latch.await();
                        cache.getValue(42, callable);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        verify(callable).call();
    }

    @Test
    public void shouldRegisterAnExpirationCallbackWithAppropriateDuration() throws Exception {
        cache.getValue(42, callable());

        verify(executorService).schedule(anyRunnable(), eq(DEFAULT_CACHE_TIMEOUT.getValue()),
                eq(DEFAULT_CACHE_TIMEOUT.getUnit()));
    }

    @Test
    public void shouldOverrideDefaultTimeout() throws Exception {
        final Duration lowerDuration = duration("10 seconds");
        cache.getValue(42, callable(), expire(lowerDuration));

        verify(executorService).schedule(anyRunnable(), eq(lowerDuration.getValue()),
                eq(lowerDuration.getUnit()));
    }

    @Test
    public void shouldNotCacheTheValueWhenTimeoutIsZero() throws Exception {
        cache.getValue(42, callable(), expire(Duration.ZERO));

        assertThat(cache.size()).isEqualTo(0);
    }

    @DataProvider
    private static Object[][] timeoutFunctionsNotCacheable() {
        // @formatter:off
        return new Object[][]{
            {
                new AsyncFunction<Integer, Duration, Exception>() {
                    @Override
                    public Promise<Duration, Exception> apply(Integer value) throws Exception {
                        throw new Exception("Boom");
                    }
                }
            },
            {
                new AsyncFunction<Integer, Duration, Exception>() {
                    @Override
                    public Promise<Duration, Exception> apply(Integer value) throws Exception {
                        return newRuntimeExceptionPromise(new RuntimeException("Boom"));
                    }
                }
            },
            {
                new AsyncFunction<Integer, Duration, Exception>() {
                    @Override
                    public Promise<Duration, Exception> apply(Integer value) throws Exception {
                        return newExceptionPromise(new Exception("Boom"));
                    }
                }
            }
        };
        // @formatter:on
    }

    @Test(dataProvider = "timeoutFunctionsNotCacheable")
    public void shouldNotCacheWithTheseTimeoutFunctions(AsyncFunction<Integer, Duration, Exception> timeoutFunction)
            throws Exception {
        Callable<Integer> callable = spy(callable());

        // First call : the timeout function fails with an RuntimeException : the value should not have been cached
        cache.getValue(42, callable, timeoutFunction);

        // Second call with the same Callable
        cache.getValue(42, callable);

        // since the value was not cached previously, then
        verify(callable, times(2)).call();
    }

    @Test
    public void shouldNotScheduleExpirationWhenTimeoutIsUnlimited() throws Exception {
        cache.getValue(42, callable(), expire(UNLIMITED));
        verifyZeroInteractions(executorService);
    }

    @DataProvider
    private Object[][] durations() {
        return new Object[][]{
                {UNLIMITED},
                {duration(3, TimeUnit.MINUTES)},
                {duration(1, TimeUnit.DAYS)},
        };
    }

    @Test(dataProvider = "durations")
    public void shouldNotCacheMoreThanTheMaxTimeout(final Duration timeout) throws Exception {
        cache.setMaxTimeout(duration(3, TimeUnit.MINUTES));
        cache.getValue(42, callable(), expire(timeout));

        verify(executorService).schedule(anyRunnable(), eq(3L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void shouldCacheLessThanTheMaxTimeout() throws Exception {
        cache.setMaxTimeout(duration(3, TimeUnit.MINUTES));
        cache.getValue(42, callable(), expire(duration(42, TimeUnit.SECONDS)));

        verify(executorService).schedule(anyRunnable(), eq(42L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldCancelTheExpirationTaskWhenClearingTheCache() throws Exception {
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(executorService).schedule(anyRunnable(), eq(DEFAULT_CACHE_TIMEOUT.getValue()),
                eq(DEFAULT_CACHE_TIMEOUT.getUnit()));

        // Given
        cache.getValue(42, callable());

        // When
        cache.clear();

        // Then
        verify(future).cancel(anyBoolean());
    }

    @Test
    public void shouldCancelTheExpirationTaskWhenEvictingAnEntry() throws Exception {
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(executorService).schedule(anyRunnable(), eq(DEFAULT_CACHE_TIMEOUT.getValue()),
                eq(DEFAULT_CACHE_TIMEOUT.getUnit()));

        // Given
        final int key = 42;
        cache.getValue(key, callable());

        // When
        cache.evict(key);

        // Then
        verify(future).cancel(anyBoolean());
    }

    private static Runnable anyRunnable() {
        return any(Runnable.class);
    }

    private Callable<Integer> callable() {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 404;
            }
        };
    }

    private AsyncFunction<Integer, Duration, Exception> expire(final Duration duration) {
        return new AsyncFunction<Integer, Duration, Exception>() {

            @Override
            public Promise<Duration, Exception> apply(Integer ignore) throws Exception {
                return newResultPromise(duration);
            }
        };
    }
}
