/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the License.
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
package org.forgerock.util.promise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class aims to be like a TCK for every implementations of Promise, that'll ensure the Promise's contract is
 * honored.
 */
public class PromiseContractTest {

    private static final String PROMISE_RESULT = "result";
    private static final Exception PROMISE_EXCEPTION = new Exception();
    private static final RuntimeException PROMISE_RUNTIME_EXCEPTION = new RuntimeException("Boom");

    @Test(dataProvider = "completedPromisesWithResult")
    public void completedPromiseWithResultWithListenerThrowingRuntimeException(String label, 
            Promise<String, Exception> promise) throws Exception {
        promise.thenOnResult(onResultThrowingRuntimeException(label));

        assertThat(promise.get()).as(label).isEqualTo("completed");
    }

    @DataProvider
    private Object[][] completedPromisesWithResult() {
        final String result = "completed";
        // Async promise, completed immediately
        PromiseImpl<String, Exception> promise = PromiseImpl.create();
        promise.handleResult(result);

        return new Object[][] {
            { "PromiseImpl", promise },
            { "CompletedPromise", newResultPromise(result) }
        };
    }

    private static ResultHandler<String> onResultThrowingRuntimeException(final String label) {
        return new ResultHandler<String>() {
            @Override
            public void handleResult(String result) {
                throw new RuntimeException("Boom " + label);
            }
        };
    }

    @Test(dataProvider = "completedPromisesWithException")
    public void completedPromiseWithExceptionWithListenerThrowingRuntimeException(Promise<String, Exception> promise,
            Exception exception) throws Exception {

        promise.thenOnException(onExceptionThrowingRuntimeException(promise.getClass().getName()));

        try {
            promise.getOrThrow();
            failBecauseExceptionWasNotThrown(exception.getClass());
        } catch (Exception e) {
            assertThat(e).isSameAs(exception);
        }
    }

    @DataProvider
    private Object[][] completedPromisesWithException() {
        Object[][] result = new Object[2][];

        final Exception exception = new Exception("completed");
        // Async promise, completed immediately
        PromiseImpl<String, Exception> promise = PromiseImpl.create();
        promise.handleException(exception);

        return new Object[][] {
            { promise, exception },
            { newExceptionPromise(exception), exception }
        };
    }

    private static ExceptionHandler<Exception> onExceptionThrowingRuntimeException(final String label) {
        return new ExceptionHandler<Exception>() {
            @Override
            public void handleException(Exception exception) {
                throw new RuntimeException("Boom " + label);
            }
        };
    }

    @Test(dataProvider = "completedPromisesWithResult")
    public void completedPromiseWithResultWithAlwaysListenerThrowingRuntimeException(String label,
            Promise<String, Exception> promise) {
        promise.thenAlways(runnableThrowingRuntimeException(promise.getClass().getName()));
    }

    private static Runnable runnableThrowingRuntimeException(final String label) {
        return new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("Boom " + label);
            }
        };
    }

    // ----------------------------------------------------------------
    // Ensure that promise are returning themselves or a new instance depending on the called then*** method
    // ----------------------------------------------------------------


    @Test(dataProvider = "completePromises")
    @SuppressWarnings("unchecked")
    public void shouldReturnNewPromiseUsingThenWithFunction(Promise<?, Exception> rootPromise) throws Exception {
        Promise<?, Exception> leafPromise;

        leafPromise = rootPromise.then(mock(Function.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);

        leafPromise = rootPromise.then(mock(Function.class), mock(Function.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);

        leafPromise = rootPromise.thenAsync(mock(AsyncFunction.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);

        leafPromise = rootPromise.thenAsync(mock(AsyncFunction.class), mock(AsyncFunction.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);

        leafPromise = rootPromise.thenCatch(mock(Function.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);

        leafPromise = rootPromise.thenCatchAsync(mock(AsyncFunction.class));
        assertThat(leafPromise).isNotSameAs(rootPromise);
    }

    @Test(dataProvider = "completePromises")
    @SuppressWarnings("unchecked")
    public void shouldReturnSamePromiseUsingThenWithListener(Promise<?, Exception> rootPromise) throws Exception {
        Promise<?, Exception> leafPromise;

        leafPromise = rootPromise.thenOnRuntimeException(mock(RuntimeExceptionHandler.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenOnResult(mock(ResultHandler.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenOnException(mock(ExceptionHandler.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenOnResultOrException(mock(ResultHandler.class), mock(ExceptionHandler.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenOnResultOrException(mock(Runnable.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenAlways(mock(Runnable.class));
        assertThat(leafPromise).isSameAs(rootPromise);

        leafPromise = rootPromise.thenFinally(mock(Runnable.class));
        assertThat(leafPromise).isSameAs(rootPromise);
    }

    @Test(dataProvider = "resultPromises")
    @SuppressWarnings("unchecked")
    public void promiseReturningValueShouldIgnoreAChainedThenCatchAsync(Promise<String, Exception> rootPromise)
            throws Exception {
        //Given
        AsyncFunction catchException = mock(AsyncFunction.class);

        //When
        Promise resultPromise = rootPromise.thenCatchAsync(catchException);

        //Then
        verifyZeroInteractions(catchException);
        assertThat(resultPromise.getOrThrowUninterruptibly()).isEqualTo(PROMISE_RESULT);
    }

    @Test(dataProvider = "exceptionPromises")
    @SuppressWarnings("unchecked")
    public void promiseReturningExceptionShouldHitAChainedThenCatchAsync(Promise<String, Exception> rootPromise)
            throws Exception {
        //Given
        Exception e2 = new Exception();
        AsyncFunction catchException = mock(AsyncFunction.class);
        when(catchException.apply(any())).thenThrow(e2);

        // When
        Promise resultPromise = rootPromise.thenCatchAsync(catchException);

        // Then
        try {
            resultPromise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(e2.getClass());
        } catch (Exception e) {
            assertThat(e).isSameAs(e2);
            verify(catchException).apply(PROMISE_EXCEPTION);
        }
    }

    @Test(dataProvider = "runtimeExceptionPromises")
    @SuppressWarnings("unchecked")
    public void promiseThrowingRuntimeExceptionShouldIgnoreAChainedThenCatchAsync
            (Promise<String, Exception> rootPromise) throws Exception {
        //Given
        AsyncFunction catchException = mock(AsyncFunction.class);

        //When
        Promise resultPromise = rootPromise.thenCatchAsync(catchException);

        //Then
        verifyZeroInteractions(catchException);
        try {
            resultPromise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(PROMISE_RUNTIME_EXCEPTION.getClass());
        } catch (Exception e) {
            assertThat(e).isSameAs(PROMISE_RUNTIME_EXCEPTION);
        }
    }

    @Test(dataProvider = "exceptionPromises")
    @SuppressWarnings("unchecked")
    public void promiseReturningExceptionShouldIgnoreAChainedThenAsync(Promise<String, Exception> rootPromise)
            throws Exception {
        //Given
        AsyncFunction ignoreException = mock(AsyncFunction.class);

        // When
        Promise resultPromise = rootPromise.thenAsync(ignoreException);

        // Then
        verifyZeroInteractions(ignoreException);
        try {
            resultPromise.getOrThrowUninterruptibly();
            failBecauseExceptionWasNotThrown(PROMISE_EXCEPTION.getClass());
        } catch (Exception e) {
            assertThat(e).isSameAs(PROMISE_EXCEPTION);
        }
    }

    @DataProvider
    private Iterator<Object[]> completePromises() {
        List<Object[]> promises = new ArrayList<>();
        Collections.addAll(promises, resultPromises());
        Collections.addAll(promises, exceptionPromises());
        Collections.addAll(promises, runtimeExceptionPromises());
        return promises.iterator();
    }

    @DataProvider
    private Object[][] resultPromises() {
        PromiseImpl<String, Exception> promiseImplResult = PromiseImpl.create();
        promiseImplResult.handleResult(PROMISE_RESULT);

        return new Object[][] {
            { Promises.newResultPromise(PROMISE_RESULT) },
            { promiseImplResult }
        };
    }

    @DataProvider
    private Object[][] exceptionPromises() {
        PromiseImpl<String, Exception> promiseImplException = PromiseImpl.create();
        promiseImplException.handleException(PROMISE_EXCEPTION);

        return new Object[][] {
            { Promises.<String, Exception>newExceptionPromise(PROMISE_EXCEPTION) },
            { promiseImplException }
        };
    }

    @DataProvider
    private Object[][] runtimeExceptionPromises() {
        PromiseImpl<String, Exception> promiseImplRuntimeException = PromiseImpl.create();
        promiseImplRuntimeException.handleRuntimeException(PROMISE_RUNTIME_EXCEPTION);

        return new Object[][] {
            { createCompletedRuntimePromise() },
            { promiseImplRuntimeException }
        };
    }

    private Promise<String, Exception> createCompletedRuntimePromise() {
        // Cannot create a RuntimeExceptionPromise in another way
        return Promises.newResultPromise(PROMISE_RESULT)
                .then(new Function<String, String, Exception>() {
            @Override
            public String apply(String value) throws Exception {
                throw PROMISE_RUNTIME_EXCEPTION;
            }
        });
    }
}
