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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;

/**
 * An implementation of {@link Promise} which can be used as is, or as the basis
 * for more complex asynchronous behavior. A {@code PromiseImpl} must be
 * completed by invoking one of:
 * <ul>
 * <li>{@link #handleResult} - marks the promise as having succeeded with the
 * provide result
 * <li>{@link #handleError} - marks the promise as having failed with the
 * provide exception
 * <li>{@link #cancel} - requests cancellation of the asynchronous task
 * represented by the promise. Cancellation is only supported if the
 * {@link #tryCancel(boolean)} is overridden and returns an exception.
 * </ul>
 *
 * @param <V>
 *            The type of the task's result, or {@link Void} if the task does
 *            not return anything (i.e. it only has side-effects).
 * @param <E>
 *            The type of the exception thrown by the task if it fails, or
 *            {@link NeverThrowsException} if the task cannot fail.
 * @see Promise
 * @see Promises
 */
public class PromiseImpl<V, E extends Exception> implements Promise<V, E>, SuccessHandler<V>,
        FailureHandler<E> {
    // TODO: Is using monitor based sync better than AQS?

    private static interface StateListener<V, E extends Exception> {
        void handleStateChange(int newState, V result, E error);
    }

    /**
     * State value indicating that this promise has not completed.
     */
    private static final int PENDING = 0;

    /**
     * State value indicating that this promise has completed successfully
     * (result set).
     */
    private static final int SUCCEEDED = 1;

    /**
     * State value indicating that this promise has failed (error set).
     */
    private static final int FAILED = 2;

    /**
     * State value indicating that this promise has been cancelled (error set).
     */
    private static final int CANCELLED = 3;

    /**
     * Creates a new pending {@link Promise} implementation.
     *
     * @param <V>
     *            The type of the task's result, or {@link Void} if the task
     *            does not return anything (i.e. it only has side-effects).
     * @param <E>
     *            The type of the exception thrown by the task if it fails, or
     *            {@link NeverThrowsException} if the task cannot fail.
     * @return A new pending {@link Promise} implementation.
     */
    public static <V, E extends Exception> PromiseImpl<V, E> create() {
        return new PromiseImpl<V, E>();
    }

    private volatile int state = PENDING;
    private V result = null;
    private E error = null;

    private final Queue<StateListener<V, E>> listeners =
            new ConcurrentLinkedQueue<StateListener<V, E>>();

    /**
     * Creates a new pending {@link Promise} implementation. This constructor is
     * protected to allow for sub-classing.
     */
    protected PromiseImpl() {
        // No implementation.
    }

    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
        if (isDone()) {
            // Fail-fast.
            return false;
        }
        final E error = tryCancel(mayInterruptIfRunning);
        return error != null && setState(CANCELLED, null, error);
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        await(); // Publishes.
        return get0();
    }

    @Override
    public final V get(final long timeout, final TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        await(timeout, unit, false); // Publishes.
        return get0();
    }

    @Override
    public final V getOrThrow() throws InterruptedException, E {
        await(); // Publishes.
        return getOrThrow0();
    }

    @Override
    public final V getOrThrow(final long timeout, final TimeUnit unit) throws InterruptedException,
            E, TimeoutException {
        await(timeout, unit, false); // Publishes.
        return getOrThrow0();
    }

    @Override
    public final V getOrThrowUninterruptibly() throws E {
        boolean wasInterrupted = false;
        try {
            while (true) {
                try {
                    return getOrThrow();
                } catch (final InterruptedException e) {
                    wasInterrupted = true;
                }
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public final V getOrThrowUninterruptibly(final long timeout, final TimeUnit unit) throws E,
            TimeoutException {
        try {
            await(timeout, unit, true); // Publishes.
        } catch (InterruptedException ignored) {
            // Will never occur since interrupts are ignored.
        }
        return getOrThrow0();
    }

    /**
     * Signals that the asynchronous task represented by this promise has
     * failed. If the task has already completed (i.e. {@code isDone() == true})
     * then calling this method has no effect and the provided result will be
     * discarded.
     *
     * @param error
     *            The exception indicating why the task failed.
     * @see #tryHandleError(Exception)
     */
    @Override
    public final void handleError(final E error) {
        tryHandleError(error);
    }

    /**
     * Signals that the asynchronous task represented by this promise has
     * succeeded. If the task has already completed (i.e.
     * {@code isDone() == true}) then calling this method has no effect and the
     * provided result will be discarded.
     *
     * @param result
     *            The result of the asynchronous task (may be {@code null}).
     * @see #tryHandleResult(Object)
     */
    @Override
    public final void handleResult(final V result) {
        tryHandleResult(result);
    }

    /**
     * Attempts to signal that the asynchronous task represented by this promise
     * has failed. If the task has already completed (i.e.
     * {@code isDone() == true}) then calling this method has no effect and
     * {@code false} is returned.
     * <p>
     * This method should be used in cases where multiple threads may
     * concurrently attempt to complete a promise and need to release resources
     * if the completion attempt fails. For example, an asynchronous TCP connect
     * attempt may complete after a timeout has expired. In this case the
     * connection should be immediately closed because it is never going to be
     * used.
     *
     * @param error
     *            The exception indicating why the task failed.
     * @return {@code false} if this promise has already been completed, either
     *         due to normal termination, an exception, or cancellation (i.e.
     *         {@code isDone() == true}).
     * @see #handleError(Exception)
     * @see #isDone()
     */
    public final boolean tryHandleError(final E error) {
        return setState(FAILED, null, error);
    }

    /**
     * Attempts to signal that the asynchronous task represented by this promise
     * has succeeded. If the task has already completed (i.e.
     * {@code isDone() == true}) then calling this method has no effect and
     * {@code false} is returned.
     * <p>
     * This method should be used in cases where multiple threads may
     * concurrently attempt to complete a promise and need to release resources
     * if the completion attempt fails. For example, an asynchronous TCP connect
     * attempt may complete after a timeout has expired. In this case the
     * connection should be immediately closed because it is never going to be
     * used.
     *
     * @param result
     *            The result of the asynchronous task (may be {@code null}).
     * @return {@code false} if this promise has already been completed, either
     *         due to normal termination, an exception, or cancellation (i.e.
     *         {@code isDone() == true}).
     * @see #handleResult(Object)
     * @see #isDone()
     */
    public final boolean tryHandleResult(final V result) {
        return setState(SUCCEEDED, result, null);
    }

    @Override
    public final boolean isCancelled() {
        return state == CANCELLED;
    }

    @Override
    public final boolean isDone() {
        return state != PENDING;
    }

    @Override
    public final Promise<V, E> thenOnFailure(final FailureHandler<? super E> onFail) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E error) {
                if (newState != SUCCEEDED) {
                    onFail.handleError(error);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnSuccess(final SuccessHandler<? super V> onSuccess) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E error) {
                if (newState == SUCCEEDED) {
                    onSuccess.handleResult(result);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnSuccessOrFailure(final SuccessHandler<? super V> onSuccess,
        final FailureHandler<? super E> onFailure) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E error) {
                if (newState == SUCCEEDED) {
                    onSuccess.handleResult(result);
                } else {
                    onFailure.handleError(error);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnSuccessOrFailure(final Runnable onSuccessOrFail) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E error) {
                onSuccessOrFail.run();
            }
        });
        return this;
    }

    @Override
    public final <VOUT> Promise<VOUT, E> then(final Function<? super V, VOUT, E> onSuccess) {
        return then(onSuccess, Promises.<VOUT, E> failIdempotentFunction());
    }

    @Override
    public <EOUT extends Exception> Promise<V, EOUT> thenCatch(final Function<? super E, V, EOUT> onFailure) {
        return then(Promises.<V, EOUT> successIdempotentFunction(), onFailure);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
        final Function<? super V, VOUT, EOUT> onSuccess, final Function<? super E, VOUT, EOUT> onFailure) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<VOUT, EOUT>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            @SuppressWarnings("unchecked")
            public void handleStateChange(final int newState, final V result, final E error) {
                try {
                    if (newState == SUCCEEDED) {
                        chained.handleResult(onSuccess.apply(result));
                    } else {
                        chained.handleResult(onFailure.apply(error));
                    }
                } catch (final Exception e) {
                    chained.handleError((EOUT) e);
                }
            }
        });
        return chained;
    }

    @Override
    public final Promise<V, E> thenAlways(final Runnable onSuccessOrFailure) {
        return thenOnSuccessOrFailure(onSuccessOrFailure);
    }

    @Override
    public final Promise<V, E> thenFinally(final Runnable onSuccessOrFailure) {
        return thenOnSuccessOrFailure(onSuccessOrFailure);
    }

    @Override
    public final <VOUT> Promise<VOUT, E> thenAsync(final AsyncFunction<? super V, VOUT, E> onSuccess) {
        return thenAsync(onSuccess, Promises.<VOUT, E> failIdempotentAsyncFunction());
    }

    @Override
    public final <EOUT extends Exception> Promise<V, EOUT> thenCatchAsync(AsyncFunction<? super E, V, EOUT> onFailure) {
        return thenAsync(Promises.<V, EOUT> successIdempotentAsyncFunction(), onFailure);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            final AsyncFunction<? super V, VOUT, EOUT> onSuccess,
            final AsyncFunction<? super E, VOUT, EOUT> onFailure) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<VOUT, EOUT>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            @SuppressWarnings("unchecked")
            public void handleStateChange(final int newState, final V result, final E error) {
                try {
                    final Promise<VOUT, EOUT> nestedPromise;
                    if (newState == SUCCEEDED) {
                        nestedPromise = onSuccess.apply(result);
                    } else {
                        nestedPromise = onFailure.apply(error);
                    }
                    nestedPromise.thenOnSuccess(new SuccessHandler<VOUT>() {
                        @Override
                        public void handleResult(final VOUT value) {
                            chained.handleResult(value);
                        }
                    }).thenOnFailure(new FailureHandler<EOUT>() {
                        @Override
                        public void handleError(final EOUT error) {
                            chained.handleError(error);
                        };
                    });
                } catch (final Exception e) {
                    chained.handleError((EOUT) e);
                }
            }
        });
        return chained;
    }

    /**
     * Invoked when the client attempts to cancel the asynchronous task
     * represented by this promise. Implementations which support cancellation
     * should override this method to cancel the asynchronous task and, if
     * successful, return an appropriate exception which can be used to signal
     * that the task has failed.
     * <p>
     * By default cancellation is not supported and this method returns
     * {@code null}.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing this task should be
     *            interrupted; otherwise, in-progress tasks are allowed to
     *            complete.
     * @return {@code null} if cancellation was not supported or not possible,
     *         otherwise an appropriate exception.
     */
    protected E tryCancel(final boolean mayInterruptIfRunning) {
        return null;
    }

    private void addOrFireListener(final StateListener<V, E> listener) {
        final int stateBefore = state;
        if (stateBefore != PENDING) {
            listener.handleStateChange(stateBefore, result, error);
        } else {
            listeners.add(listener);
            final int stateAfter = state;
            if (stateAfter != PENDING && listeners.remove(listener)) {
                listener.handleStateChange(stateAfter, result, error);
            }
        }
    }

    private V get0() throws ExecutionException {
        if (error != null) {
            throw new ExecutionException(error);
        } else {
            return result;
        }
    }

    private V getOrThrow0() throws E {
        if (error != null) {
            throw error;
        } else {
            return result;
        }
    }

    private boolean setState(final int newState, final V result, final E error) {
        synchronized (this) {
            if (state != PENDING) {
                // Already completed.
                return false;
            }
            this.result = result;
            this.error = error;
            state = newState; // Publishes.
            notifyAll(); // Wake up any blocked threads.
        }
        StateListener<V, E> listener;
        while ((listener = listeners.poll()) != null) {
            listener.handleStateChange(newState, result, error);
        }
        return true;
    }

    private void await() throws InterruptedException {
        // Use double-check for fast-path.
        if (state == PENDING) {
            synchronized (this) {
                while (state == PENDING) {
                    wait();
                }
            }
        }
    }

    private void await(final long timeout, final TimeUnit unit, final boolean isUninterruptibly)
            throws InterruptedException, TimeoutException {
        // Use double-check for fast-path.
        if (state == PENDING) {
            final long timeoutMS = unit.toMillis(timeout);
            final long endTimeMS = System.currentTimeMillis() + timeoutMS;
            boolean wasInterrupted = false;
            try {
                synchronized (this) {
                    while (state == PENDING) {
                        final long remainingTimeMS = endTimeMS - System.currentTimeMillis();
                        if (remainingTimeMS <= 0) {
                            throw new TimeoutException();
                        }
                        try {
                            wait(remainingTimeMS);
                        } catch (final InterruptedException e) {
                            if (isUninterruptibly) {
                                wasInterrupted = true;
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            } finally {
                if (wasInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
