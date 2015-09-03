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

package org.forgerock.util.promise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.testng.annotations.Test;

public class PromisesTest {

    @Test
    public void promiseCreatedByWhenShouldCompleteWithEmptyPromiseList() throws Exception {

        //Given
        final List<Promise<Void, NeverThrowsException>> promises =
                new ArrayList<>();
        final AtomicBoolean complete = new AtomicBoolean(false);

        //When
        Promises.when(promises)
                .thenAlways(new Runnable() {
                    @Override
                    public void run() {
                        complete.set(true);
                    }
                });

        //Then
        assertThat(complete.get()).describedAs("Promises.when did not complete").isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chainedCompletedResultPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThen() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();
        Promise<Void, Exception> completedPromise = newResultPromise(null);

        Function<Void, Void, Exception> resultHandler = mock(Function.class);
        Function<Exception, Void, Exception> exceptionHandler = mock(Function.class);

        Promise<Void, Exception> leafPromise = completedPromise
                .then(new Function<Void, Void, Exception>() {
                    @Override
                    public Void apply(Void value) {
                        throw runtimeException;
                    }
                }).then(resultHandler, exceptionHandler);

        //When
        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //Then
        verifyZeroInteractions(resultHandler, exceptionHandler);
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chainedCompletedExceptionPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThen() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();
        Promise<Void, Exception> completedPromise = newExceptionPromise(null);

        Function<Void, Void, Exception> resultHandler = mock(Function.class);
        Function<Exception, Void, Exception> exceptionHandler = mock(Function.class);

        Promise<Void, Exception> leafPromise = completedPromise
                .then(null, new Function<Exception, Void, Exception>() {
                    @Override
                    public Void apply(Exception exception) {
                        throw runtimeException;
                    }
                }).then(resultHandler, exceptionHandler);

        //When
        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //Then
        verifyZeroInteractions(resultHandler, exceptionHandler);
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chainedCompletedResultPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThenAsync() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();
        Promise<Void, Exception> completedPromise = newResultPromise(null);

        AsyncFunction<Void, Void, Exception> resultHandler = mock(AsyncFunction.class);
        AsyncFunction<Exception, Void, Exception> exceptionHandler = mock(AsyncFunction.class);

        Promise<Void, Exception> leafPromise = completedPromise
                .thenAsync(new AsyncFunction<Void, Void, Exception>() {
                    @Override
                    public Promise<Void, Exception> apply(Void value) {
                        throw runtimeException;
                    }
                }).thenAsync(resultHandler, exceptionHandler);

        //When
        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //Then
        verifyZeroInteractions(resultHandler, exceptionHandler);
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chainedCompletedExceptionPromiseWithRuntimeExceptionShouldPropagateThroughAChainedThenAsync() {

        //Given
        RuntimeExceptionHandler runtimeExceptionHandler = mock(RuntimeExceptionHandler.class);
        final RuntimeException runtimeException = new RuntimeException();
        Promise<Void, Exception> completedPromise = newExceptionPromise(null);

        AsyncFunction<Void, Void, Exception> resultHandler = mock(AsyncFunction.class);
        AsyncFunction<Exception, Void, Exception> exceptionHandler = mock(AsyncFunction.class);

        Promise<Void, Exception> leafPromise = completedPromise
                .thenAsync(null, new AsyncFunction<Exception, Void, Exception>() {
                    @Override
                    public Promise<Void, Exception> apply(Exception exception) {
                        throw runtimeException;
                    }
                }).thenAsync(resultHandler, exceptionHandler);

        //When
        leafPromise.thenOnRuntimeException(runtimeExceptionHandler);

        //Then
        verifyZeroInteractions(resultHandler, exceptionHandler);
        verify(runtimeExceptionHandler).handleRuntimeException(runtimeException);
    }
}
