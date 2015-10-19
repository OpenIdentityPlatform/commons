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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.util.concurrent.ExecutionException;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
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
        Promise<Void, NeverThrowsException> leafPromise = rootPromise.then(promiseFunction);

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

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
        Promise<Void, NeverThrowsException> leafPromise = rootPromise
                .thenAsync(new AsyncFunction<Void, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(Void value) {
                        return badPromise;
                    }
                });

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

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
        Promise<Void, NeverThrowsException> leafPromise = rootPromise
                .then(new Function<Void, Void, NeverThrowsException>() {
                    @Override
                    public Void apply(Void value) {
                        throw runtimeException;
                    }
                });

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

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
        Promise<Void, NeverThrowsException> leafPromise = rootPromise
                .thenAsync(new AsyncFunction<Void, Void, NeverThrowsException>() {
                    @Override
                    public Promise<Void, NeverThrowsException> apply(Void value) {
                        throw runtimeException;
                    }
                });

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleResult(null);

        //Then
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
     public void promiseThrowingRuntimeExceptionShouldPropagateThroughAChainedThenOnResult() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> rootPromise = new PromiseImpl<>();
        Promise<Void, NeverThrowsException> leafPromise = rootPromise
                .thenOnResult(new ResultHandler<Void>() {
                    @Override
                    public void handleResult(Void result) {
                        throw runtimeException;
                    }
                });

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleResult(null);

        //Then
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @Test
    public void promiseThrowingRuntimeExceptionShouldPropagateThroughAChainedThenOnException() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, Exception> rootPromise = new PromiseImpl<>();
        Promise<Void, Exception> leafPromise = rootPromise
                .thenOnException(new ExceptionHandler<Exception>() {
                    @Override
                    public void handleException(Exception exception) {
                        throw runtimeException;
                    }
                });

        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //When
        rootPromise.handleException(new Exception());

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
    public void promiseBrokenWithRuntimeExceptionShouldThrowOnGetOrThrow() {

        //Given
        final RuntimeException runtimeException = new RuntimeException();

        PromiseImpl<Void, NeverThrowsException> promise = new PromiseImpl<>();
        promise.handleRuntimeException(runtimeException);

        //When
        try {
            promise.getOrThrow();
            fail();
        } catch (Exception e) {
            //Then
            assertThat(e).isSameAs(runtimeException);
        }
    }
}
