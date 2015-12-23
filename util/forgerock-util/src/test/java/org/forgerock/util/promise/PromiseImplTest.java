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
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.mockito.InOrder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PromiseImplTest {

    @SuppressWarnings("unchecked")
    @Test
    public void completingPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThen() {

        //Given
        Function<Void, Void, NeverThrowsException> promiseFunction = mock(Function.class);
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> rootPromise = new PromiseImpl<>();
        rootPromise
                .then(promiseFunction)
                .thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleRuntimeException(runtimeException);

        //Then
        verify(promiseFunction, never()).apply(any(Void.class));
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
    public void completingPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThenAsync() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> rootPromise = new PromiseImpl<>();
        final PromiseImpl<Void, NeverThrowsException> badPromise = new PromiseImpl<>();
        rootPromise
                .thenAsync(new AsyncFunction<Void, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(Void value) {
                        return badPromise;
                    }
                })
                .thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleResult(null);
        badPromise.handleRuntimeException(runtimeException);

        //Then
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
    public void promiseThrowingRuntimeExceptionShouldPropagateThroughAChainedThen() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> rootPromise = new PromiseImpl<>();
        rootPromise
                .then(new Function<Void, Void, NeverThrowsException>() {
                    @Override
                    public Void apply(Void value) {
                        throw runtimeException;
                    }
                })
                .thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleResult(null);

        //Then
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
    public void promiseThrowingRuntimeExceptionShouldPropagateThroughAChainedThenAsync() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> rootPromise = new PromiseImpl<>();
        rootPromise
                .thenAsync(new AsyncFunction<Void, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(Void value) {
                        throw runtimeException;
                    }
                })
                .thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleResult(null);

        //Then
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
    public void promiseBrokenWithRuntimeExceptionShouldThrowOnGet() {

        //Given
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> promise = new PromiseImpl<>();
        promise.handleRuntimeException(runtimeException);

        //When
        try {
            promise.get();
            fail();
        } catch (Exception e) {
            //Then
            assertThat(e).isInstanceOf(ExecutionException.class);
            assertThat(e.getCause()).isSameAs(runtimeException);
        }
    }

    @Test
    public void promiseBrokenWithRuntimeExceptionShouldThrowOnGetOrThrow() throws Exception {

        //Given
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> promise = new PromiseImpl<>();
        promise.handleRuntimeException(runtimeException);

        //When
        try {
            promise.getOrThrow();
            fail();
        } catch (RuntimeException e) {
            //Then
            assertThat(e).isSameAs(runtimeException);
        }
    }

    @Test
    public void promiseSupportsCovariantReturnType() {
        assertThat(supplyNumber(false).getOrThrowUninterruptibly()).isEqualTo(123L);
        assertThat(supplyNumber(true).getOrThrowUninterruptibly()).isEqualTo(1.23);
    }

    private Promise<? extends Number, NeverThrowsException> supplyNumber(boolean wantDouble) {
        if (wantDouble) {
            return supplyDouble();
        } else {
            return supplyLong();
        }
    }

    private Promise<Double, NeverThrowsException> supplyDouble() {
        return newResultPromise(1.23);
    }

    private Promise<Long, NeverThrowsException> supplyLong() {
        return newResultPromise(123L);
    }

    @Test
    public void promiseSupportsCovariantExceptionType() {
        try {
            throwException(false).getOrThrowUninterruptibly();
            fail();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IOException.class);
        }

        try {
            throwException(true).getOrThrowUninterruptibly();
            fail();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    /**
     * This test aims to verify that we have the same behavior if the promise is completed before or after having
     * registered the listeners.
     * We want all the appropriate listeners to be executed even if one throws a RuntimeException. The
     * RuntimeExceptionHandler must *not* be triggered because of a listener threw a RuntimeException
     *
     * @param completeBefore complete the promise before or after registering the listeners
     */
    @Test(dataProvider = "completeBeforeAfter")
    @SuppressWarnings("unchecked")
    public void completeResultPromiseBeforeRegisteringListener(boolean completeBefore) throws Exception {
        final String result = "completed";
        // Setup mocks behavior
        ResultHandler<String> resultHandler1 = mock(ResultHandler.class);
        doThrow(new RuntimeException("ResultHandler #1"))
                .when(resultHandler1).handleResult(result);

        ResultHandler<String> resultHandler2 = mock(ResultHandler.class);
        doThrow(new RuntimeException("ResultHandler #2"))
                .when(resultHandler2).handleResult(result);

        Runnable alwaysListener = mock(Runnable.class);
        doThrow(new RuntimeException("Always listener"))
                .when(alwaysListener).run();

        Runnable onResultOrException = mock(Runnable.class);
        doThrow(new RuntimeException("On result or exception"))
                .when(onResultOrException).run();

        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);

        // Given
        PromiseImpl<String, Exception> promise = PromiseImpl.create();

        if (completeBefore) {
            // When
            promise.handleResult(result);
        }

        // Given again...
        promise
                .thenOnResult(resultHandler1)
                .thenAlways(alwaysListener)
                .thenOnResult(resultHandler2)
                .thenOnRuntimeException(runtimeExceptionHandler)
                .thenOnResultOrException(onResultOrException);

        if (!completeBefore) {
            // When
            promise.handleResult(result);
        }

        // Then
        assertThat(promise.get()).isSameAs(result);
        InOrder inOrder = inOrder(resultHandler1, resultHandler2, alwaysListener, onResultOrException);
        inOrder.verify(resultHandler1).handleResult(result);
        inOrder.verify(alwaysListener).run();
        inOrder.verify(resultHandler2).handleResult(result);
        inOrder.verify(onResultOrException).run();
        verifyZeroInteractions(runtimeExceptionHandler);
    }

    /**
     * This test aims to verify that we have the same behavior if the promise is completed before or after having
     * registered the listeners.
     * We want all the appropriate listeners to be executed even if one throws a RuntimeException. The
     * RuntimeExceptionHandler must *not* be triggered because of a listener threw a RuntimeException
     *
     * @param completeBefore complete the promise before or after registering the listeners
     */
    @Test(dataProvider = "completeBeforeAfter")
    @SuppressWarnings("unchecked")
    public void completeExceptionPromiseBeforeRegisteringListener(boolean completeBefore) throws Exception {
        final Exception result = new Exception("completed");
        // Setup mocks behavior
        ExceptionHandler<Exception> exceptionHandler1 = mock(ExceptionHandler.class);
        doThrow(new RuntimeException("ExceptionHandler #1"))
                .when(exceptionHandler1).handleException(result);

        ExceptionHandler<Exception> exceptionHandler2 = mock(ExceptionHandler.class);
        doThrow(new RuntimeException("ResultHandler #2"))
                .when(exceptionHandler2).handleException(result);

        Runnable alwaysListener = mock(Runnable.class);
        doThrow(new RuntimeException("Always listener"))
                .when(alwaysListener).run();

        Runnable onResultOrException = mock(Runnable.class);
        doThrow(new RuntimeException("On result or exception"))
                .when(onResultOrException).run();

        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);

        // Given
        PromiseImpl<String, Exception> promise = PromiseImpl.create();

        if (completeBefore) {
            // When
            promise.handleException(result);
        }

        // Given again...
        promise
                .thenOnException(exceptionHandler1)
                .thenAlways(alwaysListener)
                .thenOnException(exceptionHandler2)
                .thenOnRuntimeException(runtimeExceptionHandler)
                .thenOnResultOrException(onResultOrException);

        if (!completeBefore) {
            // When
            promise.handleException(result);
        }

        // Then
        try {
            promise.getOrThrow();
            fail("The promise should have thrown the completed Exception.");
        } catch (Exception e) {
            assertThat(e).isSameAs(e);
        }
        InOrder inOrder = inOrder(exceptionHandler1, exceptionHandler2, alwaysListener, onResultOrException);
        inOrder.verify(exceptionHandler1).handleException(result);
        inOrder.verify(alwaysListener).run();
        inOrder.verify(exceptionHandler2).handleException(result);
        inOrder.verify(onResultOrException).run();
        verifyZeroInteractions(runtimeExceptionHandler);
    }

    /**
     * This test aims to verify that we have the same behavior if the promise is completed before or after having
     * registered the listeners.
     * We want all the appropriate listeners to be executed even if one throws a RuntimeException. The
     * RuntimeExceptionHandler must *not* be triggered because of a listener threw a RuntimeException
     *
     * @param completeBefore complete the promise before or after registering the listeners
     */
    @Test(dataProvider = "completeBeforeAfter")
    @SuppressWarnings("unchecked")
    public void completeRuntimeExceptionPromiseBeforeRegisteringListener(boolean completeBefore) throws Exception {
        final RuntimeException result = new RuntimeException("completed");
        // Setup mocks behavior
        RuntimeExceptionHandler runtimeExceptionHandler1 = mock(RuntimeExceptionHandler.class);
        doThrow(new RuntimeException("RuntimeExceptionHandler #1"))
                .when(runtimeExceptionHandler1).handleRuntimeException(result);

        RuntimeExceptionHandler runtimeExceptionHandler2 = mock(RuntimeExceptionHandler.class);
        doThrow(new RuntimeException("RuntimeExceptionHandler #2"))
                .when(runtimeExceptionHandler2).handleRuntimeException(result);

        Runnable alwaysListener = mock(Runnable.class);
        doThrow(new RuntimeException("Always listener"))
                .when(alwaysListener).run();

        Runnable onResultOrException = mock(Runnable.class);

        // Given
        PromiseImpl<String, Exception> promise = PromiseImpl.create();

        if (completeBefore) {
            // When
            promise.handleRuntimeException(result);
        }

        // Given again...
        promise
                .thenOnRuntimeException(runtimeExceptionHandler1)
                .thenOnRuntimeException(runtimeExceptionHandler2)
                .thenAlways(alwaysListener)
                .thenOnResultOrException(onResultOrException);

        if (!completeBefore) {
            // When
            promise.handleRuntimeException(result);
        }

        // Then
        try {
            promise.getOrThrow();
            fail("The promise should have thrown the completed RuntimeException.");
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(e);
        }
        InOrder inOrder = inOrder(runtimeExceptionHandler1, runtimeExceptionHandler2, alwaysListener);
        inOrder.verify(runtimeExceptionHandler1).handleRuntimeException(result);
        inOrder.verify(runtimeExceptionHandler2).handleRuntimeException(result);
        inOrder.verify(alwaysListener).run();
        verifyZeroInteractions(onResultOrException);
    }

    @DataProvider
    private Object[][] completeBeforeAfter() {
        return new Object[][] { { Boolean.TRUE }, { Boolean.FALSE } };
    }

    private Promise<Void, ? extends Exception> throwException(boolean wantRuntimeException) {
        if (wantRuntimeException) {
            return throwRuntimeException();
        } else {
            return throwIOException();
        }
    }

    private Promise<Void, RuntimeException> throwRuntimeException() {
        return newExceptionPromise(new RuntimeException());
    }

    private Promise<Void, IOException> throwIOException() {
        return newExceptionPromise(new IOException());
    }

}
