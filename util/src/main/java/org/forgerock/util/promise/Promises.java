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
 * Copyright 2014 ForgeRock Inc.
 */
package org.forgerock.util.promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;

/**
 * Utility methods for creating and composing {@link Promise}s.
 */
public final class Promises {
    // TODO: n-of, etc.

    private static abstract class CompletedPromise<V, E extends Exception> implements Promise<V, E> {
        @Override
        public final boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public final V get() throws ExecutionException {
            if (isSuccess()) {
                return getValue();
            } else {
                throw new ExecutionException(getError());
            }
        }

        @Override
        public final V get(final long timeout, final TimeUnit unit) throws ExecutionException {
            return get();
        }

        @Override
        public final V getOrThrow() throws E {
            if (isSuccess()) {
                return getValue();
            } else {
                throw getError();
            }
        }

        @Override
        public final V getOrThrow(final long timeout, final TimeUnit unit) throws E {
            return getOrThrow();
        }

        @Override
        public final V getOrThrowUninterruptibly() throws E {
            return getOrThrow();
        }

        @Override
        public final V getOrThrowUninterruptibly(final long timeout, final TimeUnit unit) throws E {
            return getOrThrow();
        }

        @Override
        public final boolean isCancelled() {
            return false;
        }

        @Override
        public final boolean isDone() {
            return true;
        }

        @Override
        public final Promise<V, E> thenOnFailure(final FailureHandler<? super E> onFail) {
            if (!isSuccess()) {
                onFail.handleError(getError());
            }
            return this;
        }

        @Override
        public final Promise<V, E> thenOnSuccess(final SuccessHandler<? super V> onSuccess) {
            if (isSuccess()) {
                onSuccess.handleResult(getValue());
            }
            return this;
        }

        @Override
        public final Promise<V, E> thenOnSuccessOrFailure(final SuccessHandler<? super V> onSuccess,
            final FailureHandler<? super E> onFail) {
            return thenOnSuccess(onSuccess).thenOnFailure(onFail);
        }

        @Override
        public final Promise<V, E> thenOnSuccessOrFailure(final Runnable onSuccessOrFail) {
            onSuccessOrFail.run();
            return this;
        }

        @Override
        public final <VOUT> Promise<VOUT, E> then(final Function<? super V, VOUT, E> onSuccess) {
            return then(onSuccess, Promises.<VOUT, E> failIdempotentFunction());
        }

        @Override
        public <EOUT extends Exception> Promise<V, EOUT> thenCatch(Function<? super E, V, EOUT> onFailure) {
            return then(Promises.<V, EOUT> successIdempotentFunction(), onFailure);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
                final Function<? super V, VOUT, EOUT> onSuccess,
                final Function<? super E, VOUT, EOUT> onFail) {
            try {
                if (isSuccess()) {
                    return newSuccessfulPromise(onSuccess.apply(getValue()));
                } else {
                    return newSuccessfulPromise(onFail.apply(getError()));
                }
            } catch (final Exception e) {
                return newFailedPromise((EOUT) e);
            }
        }


        @Override
        public final Promise<V, E> thenAlways(final Runnable onSuccessOrFail) {
            return thenOnSuccessOrFailure(onSuccessOrFail);
        }

        @Override
        public Promise<V, E> thenFinally(Runnable onSuccessOrFailure) {
            return thenOnSuccessOrFailure(onSuccessOrFailure);
        }

        @Override
        public final <VOUT> Promise<VOUT, E> thenAsync(
                final AsyncFunction<? super V, VOUT, E> onSuccess) {
            return thenAsync(onSuccess, Promises.<VOUT, E> failIdempotentAsyncFunction());
        }

        @Override
        public final <EOUT extends Exception> Promise<V, EOUT> thenCatchAsync(
                final AsyncFunction<? super E, V, EOUT> onFailure) {
            return thenAsync(Promises.<V, EOUT> successIdempotentAsyncFunction(), onFailure);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
                final AsyncFunction<? super V, VOUT, EOUT> onSuccess,
                final AsyncFunction<? super E, VOUT, EOUT> onFail) {
            try {
                if (isSuccess()) {
                    return onSuccess.apply(getValue());
                } else {
                    return onFail.apply(getError());
                }
            } catch (final Exception e) {
                return newFailedPromise((EOUT) e);
            }
        }

        abstract E getError();

        abstract V getValue();

        abstract boolean isSuccess();
    }

    private static final class FailedPromise<V, E extends Exception> extends CompletedPromise<V, E> {
        private final E error;

        private FailedPromise(final E error) {
            this.error = error;
        }

        @Override
        E getError() {
            return error;
        }

        @Override
        V getValue() {
            throw new IllegalStateException();
        }

        @Override
        boolean isSuccess() {
            return false;
        }
    }

    private static final class SuccessfulPromise<V, E extends Exception> extends
            CompletedPromise<V, E> {
        private final V value;

        private SuccessfulPromise(final V value) {
            this.value = value;
        }

        @Override
        E getError() {
            throw new IllegalStateException();
        }

        @Override
        V getValue() {
            return value;
        }

        @Override
        boolean isSuccess() {
            return true;
        }
    }

    private static final AsyncFunction<Exception, Object, Exception> FAIL_IDEM_ASYNC_FUNC =
            new AsyncFunction<Exception, Object, Exception>() {
                @Override
                public Promise<Object, Exception> apply(final Exception error) throws Exception {
                    throw error;
                }
            };

    private static final Function<Exception, Object, Exception> FAIL_IDEM_FUNC =
            new Function<Exception, Object, Exception>() {
                @Override
                public Object apply(final Exception error) throws Exception {
                    throw error;
                }
            };

    private static final AsyncFunction<Exception, Object, Exception> SUCCESS_IDEM_ASYNC_FUNC =
            new AsyncFunction<Exception, Object, Exception>() {
                @Override
                public Promise<Object, Exception> apply(final Exception error) throws Exception {
                    throw error;
                }
            };

    private static final Function<Object, Object, Exception> SUCCESS_IDEM_FUNC =
            new Function<Object, Object, Exception>() {
                @Override
                public Object apply(final Object value) throws Exception {
                    return value;
                }
            };

    /**
     * Returns a {@link Promise} representing an asynchronous task which has
     * already failed with the provided error. Attempts to get the result will
     * immediately fail, and any listeners registered against the returned
     * promise will be immediately invoked in the same thread as the caller.
     *
     * @param <V>
     *            The type of the task's result, or {@link Void} if the task
     *            does not return anything (i.e. it only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the task if it fails, or
     *            {@link NeverThrowsException} if the task cannot fail.
     * @param error
     *            The exception indicating why the asynchronous task has failed.
     * @return A {@link Promise} representing an asynchronous task which has
     *         already failed with the provided error.
     */
    public static final <V, E extends Exception> Promise<V, E> newFailedPromise(final E error) {
        return new FailedPromise<V, E>(error);
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
    public static final <V, E extends Exception> Promise<V, E> newSuccessfulPromise(final V result) {
        return new SuccessfulPromise<V, E>(result);
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
    public static final <V, E extends Exception> Promise<List<V>, E> when(
            final List<Promise<V, E>> promises) {
        final int size = promises.size();
        final AtomicInteger remaining = new AtomicInteger(size);
        final List<V> results = new ArrayList<V>(size);
        final PromiseImpl<List<V>, E> composite = PromiseImpl.create();
        for (final Promise<V, E> promise : promises) {
            promise.thenOnSuccess(new SuccessHandler<V>() {
                @Override
                public void handleResult(final V value) {
                    synchronized (results) {
                        results.add(value);
                    }
                    if (remaining.decrementAndGet() == 0) {
                        composite.handleResult(results);
                    }
                }
            }).thenOnFailure(new FailureHandler<E>() {
                @Override
                public void handleError(final E error) {
                    composite.handleError(error);
                }
            });
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
     *         fails.
     */
    public static final <V, E extends Exception> Promise<List<V>, E> when(
            final Promise<V, E>... promises) {
        return when(Arrays.asList(promises));
    }

    @SuppressWarnings("unchecked")
    static <VOUT, E extends Exception> AsyncFunction<E, VOUT, E> failIdempotentAsyncFunction() {
        return (AsyncFunction<E, VOUT, E>) FAIL_IDEM_ASYNC_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <VOUT, E extends Exception> Function<E, VOUT, E> failIdempotentFunction() {
        return (Function<E, VOUT, E>) FAIL_IDEM_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <V, E extends Exception> AsyncFunction<V, V, E> successIdempotentAsyncFunction() {
        return (AsyncFunction<V, V, E>) SUCCESS_IDEM_ASYNC_FUNC;
    }

    @SuppressWarnings("unchecked")
    static <V, E extends Exception> Function<V, V, E> successIdempotentFunction() {
        return (Function<V, V, E>) SUCCESS_IDEM_FUNC;
    }

    private Promises() {
        // Prevent instantiation.
    }
}
