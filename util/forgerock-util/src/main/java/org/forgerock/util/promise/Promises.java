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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;

/**
 * Utility methods for creating and composing {@link Promise}s.
 */
public final class Promises {
    // TODO: n-of, etc.

    private static final AsyncFunction<Exception, Object, Exception> EXCEPTION_IDEM_ASYNC_FUNC =
            new AsyncFunction<Exception, Object, Exception>() {
                @Override
                public Promise<Object, Exception> apply(final Exception exception) throws Exception {
                    throw exception;
                }
            };

    private static final Function<Exception, Object, Exception> EXCEPTION_IDEM_FUNC =
            new Function<Exception, Object, Exception>() {
                @Override
                public Object apply(final Exception exception) throws Exception {
                    throw exception;
                }
            };

    private static final AsyncFunction<Exception, Object, Exception> RESULT_IDEM_ASYNC_FUNC =
            new AsyncFunction<Exception, Object, Exception>() {
                @Override
                public Promise<Object, Exception> apply(final Exception exception) throws Exception {
                    throw exception;
                }
            };

    private static final Function<Object, Object, Exception> RESULT_IDEM_FUNC =
            new Function<Object, Object, Exception>() {
                @Override
                public Object apply(final Object value) throws Exception {
                    return value;
                }
            };

    /**
     * Returns a {@link Promise} representing an asynchronous task which has
     * already failed with the provided exception. Attempts to get the result will
     * immediately fail, and any listeners registered against the returned
     * promise will be immediately invoked in the same thread as the caller.
     *
     * @param <V>
     *            The type of the task's result, or {@link Void} if the task
     *            does not return anything (i.e. it only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the task if it fails, or
     *            {@link NeverThrowsException} if the task cannot fail.
     * @param exception
     *            The exception indicating why the asynchronous task has failed.
     * @return A {@link Promise} representing an asynchronous task which has
     *         already failed with the provided exception.
     */
    public static <V, E extends Exception> Promise<V, E> newExceptionPromise(final E exception) {
        PromiseImpl<V, E> promise = new PromiseImpl<>();
        promise.handleException(exception);
        return promise;
    }

    /**
     * Returns a {@link Promise} representing an asynchronous task which has
     * already succeeded with the provided result. Attempts to get the result
     * will immediately return the result, and any listeners registered against
     * the returned promise will be immediately invoked in the same thread as
     * the caller.
     *
     * @param <V>
     *            The type of the task's result, or {@link Void} if the task
     *            does not return anything (i.e. it only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the task if it fails, or
     *            {@link NeverThrowsException} if the task cannot fail.
     * @param result
     *            The result of the asynchronous task.
     * @return A {@link Promise} representing an asynchronous task which has
     *         already succeeded with the provided result.
     */
    public static <V, E extends Exception> Promise<V, E> newResultPromise(final V result) {
        PromiseImpl<V, E> promise = new PromiseImpl<>();
        promise.handleResult(result);
        return promise;
    }

    /**
     * Returns a {@link Promise} which will be completed once all of the
     * provided promises have succeeded, or as soon as one of them fails.
     *
     * @param <V>
     *            The type of the tasks' result, or {@link Void} if the tasks do
     *            not return anything (i.e. they only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the tasks if they fail, or
     *            {@link NeverThrowsException} if the tasks cannot fail.
     * @param promises
     *            The list of tasks to be combined.
     * @return A {@link Promise} which will be completed once all of the
     *         provided promises have succeeded, or as soon as one of them
     *         fails.
     */
    public static <V, E extends Exception> Promise<List<V>, E> when(
            final List<Promise<V, E>> promises) {
        final int size = promises.size();
        final AtomicInteger remaining = new AtomicInteger(size);
        final List<V> results = new ArrayList<>(size);
        final PromiseImpl<List<V>, E> composite = PromiseImpl.create();
        for (final Promise<V, E> promise : promises) {
            promise.thenOnResult(new ResultHandler<V>() {
                @Override
                public void handleResult(final V value) {
                    synchronized (results) {
                        results.add(value);
                    }
                    if (remaining.decrementAndGet() == 0) {
                        composite.handleResult(results);
                    }
                }
            }).thenOnException(new ExceptionHandler<E>() {
                @Override
                public void handleException(final E exception) {
                    composite.handleException(exception);
                }
            });
        }
        if (promises.isEmpty()) {
            composite.handleResult(results);
        }
        return composite;
    }

    /**
     * Returns a {@link Promise} which will be completed once all of the
     * provided promises have succeeded, or as soon as one of them fails.
     *
     * @param <V>
     *            The type of the tasks' result, or {@link Void} if the tasks do
     *            not return anything (i.e. they only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the tasks if they fail, or
     *            {@link NeverThrowsException} if the tasks cannot fail.
     * @param promises
     *            The list of tasks to be combined.
     * @return A {@link Promise} which will be completed once all of the
     *         provided promises have succeeded, or as soon as one of them
     *         has thrown an exception.
     */
    @SafeVarargs
    public static <V, E extends Exception> Promise<List<V>, E> when(final Promise<V, E>... promises) {
        return when(Arrays.asList(promises));
    }

    @SuppressWarnings("unchecked")
    static <VOUT, E extends Exception> AsyncFunction<E, VOUT, E> exceptionIdempotentAsyncFunction() {
        return (AsyncFunction<E, VOUT, E>) EXCEPTION_IDEM_ASYNC_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <VOUT, E extends Exception> Function<E, VOUT, E> exceptionIdempotentFunction() {
        return (Function<E, VOUT, E>) EXCEPTION_IDEM_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <V, E extends Exception> AsyncFunction<V, V, E> resultIdempotentAsyncFunction() {
        return (AsyncFunction<V, V, E>) RESULT_IDEM_ASYNC_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <V, E extends Exception> Function<V, V, E> resultIdempotentFunction() {
        return (Function<V, V, E>) RESULT_IDEM_FUNC;
    }

    private Promises() {
        // Prevent instantiation.
    }
}
