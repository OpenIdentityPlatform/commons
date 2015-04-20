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
 * <li>{@link #handleException} - marks the promise as having failed with the
 * provided exception
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
public class PromiseImpl<V, E extends Exception> implements Promise<V, E>, ResultHandler<V>,
        ExceptionHandler<E> {
    // TODO: Is using monitor based sync better than AQS?

    private static interface StateListener<V, E extends Exception> {
        void handleStateChange(int newState, V result, E exception);
    }

    /**
     * State value indicating that this promise has not completed.
     */
    private static final int PENDING = 0;

    /**
     * State value indicating that this promise has completed successfully
     * (result set).
     */
    private static final int HAS_RESULT = 1;

    /**
     * State value indicating that this promise has failed (exception set).
     */
    private static final int HAS_EXCEPTION = 2;

    /**
     * State value indicating that this promise has been cancelled (exception set).
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
    private E exception = null;

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
        final E exception = tryCancel(mayInterruptIfRunning);
        return exception != null && setState(CANCELLED, null, exception);
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
     * @param exception
     *            The exception indicating why the task failed.
     * @see #tryHandleException(Exception)
     */
    @Override
    public final void handleException(final E exception) {
        tryHandleException(exception);
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
     * @param exception
     *            The exception indicating why the task failed.
     * @return {@code false} if this promise has already been completed, either
     *         due to normal termination, an exception, or cancellation (i.e.
     *         {@code isDone() == true}).
     * @see #handleException(Exception)
     * @see #isDone()
     */
    public final boolean tryHandleException(final E exception) {
        return setState(HAS_EXCEPTION, null, exception);
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
        return setState(HAS_RESULT, result, null);
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
    public final Promise<V, E> thenOnException(final ExceptionHandler<? super E> onException) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception) {
                if (newState != HAS_RESULT) {
                    onException.handleException(exception);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnResult(final ResultHandler<? super V> onResult) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception) {
                if (newState == HAS_RESULT) {
                    onResult.handleResult(result);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnResultOrException(final ResultHandler<? super V> onResult,
                                                   final ExceptionHandler<? super E> onException) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception) {
                if (newState == HAS_RESULT) {
                    onResult.handleResult(result);
                } else {
                    onException.handleException(exception);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnResultOrException(final Runnable onResultOrException) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception) {
                onResultOrException.run();
            }
        });
        return this;
    }

    @Override
    public final <VOUT> Promise<VOUT, E> then(final Function<? super V, VOUT, E> onResult) {
        return then(onResult, Promises.<VOUT, E>exceptionIdempotentFunction());
    }

    @Override
    public <EOUT extends Exception> Promise<V, EOUT> thenCatch(final Function<? super E, V, EOUT> onException) {
        return then(Promises.<V, EOUT>resultIdempotentFunction(), onException);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
        final Function<? super V, VOUT, EOUT> onResult, final Function<? super E, VOUT, EOUT> onException) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<VOUT, EOUT>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            @SuppressWarnings("unchecked")
            public void handleStateChange(final int newState, final V result, final E exception) {
                try {
                    if (newState == HAS_RESULT) {
                        chained.handleResult(onResult.apply(result));
                    } else {
                        chained.handleResult(onException.apply(exception));
                    }
                } catch (final Exception e) {
                    chained.handleException((EOUT) e);
                }
            }
        });
        return chained;
    }

    @Override
    public final Promise<V, E> thenAlways(final Runnable onResultOrException) {
        return thenOnResultOrException(onResultOrException);
    }

    @Override
    public final Promise<V, E> thenFinally(final Runnable onResultOrException) {
        return thenOnResultOrException(onResultOrException);
    }

    @Override
    public final <VOUT> Promise<VOUT, E> thenAsync(final AsyncFunction<? super V, VOUT, E> onResult) {
        return thenAsync(onResult, Promises.<VOUT, E>exceptionIdempotentAsyncFunction());
    }

    @Override
    public final <EOUT extends Exception> Promise<V, EOUT> thenCatchAsync(AsyncFunction<? super E, V, EOUT> onException) {
        return thenAsync(Promises.<V, EOUT>resultIdempotentAsyncFunction(), onException);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            final AsyncFunction<? super V, VOUT, EOUT> onResult,
            final AsyncFunction<? super E, VOUT, EOUT> onException) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<VOUT, EOUT>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            @SuppressWarnings("unchecked")
            public void handleStateChange(final int newState, final V result, final E exception) {
                try {
                    final Promise<VOUT, EOUT> nestedPromise;
                    if (newState == HAS_RESULT) {
                        nestedPromise = onResult.apply(result);
                    } else {
                        nestedPromise = onException.apply(exception);
                    }
                    nestedPromise.thenOnResult(new ResultHandler<VOUT>() {
                        @Override
                        public void handleResult(final VOUT value) {
                            chained.handleResult(value);
                        }
                    }).thenOnException(new ExceptionHandler<EOUT>() {
                        @Override
                        public void handleException(final EOUT exception) {
                            chained.handleException(exception);
                        }

                        ;
                    });
                } catch (final Exception e) {
                    chained.handleException((EOUT) e);
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
            listener.handleStateChange(stateBefore, result, exception);
        } else {
            listeners.add(listener);
            final int stateAfter = state;
            if (stateAfter != PENDING && listeners.remove(listener)) {
                listener.handleStateChange(stateAfter, result, exception);
            }
        }
    }

    private V get0() throws ExecutionException {
        if (exception != null) {
            throw new ExecutionException(exception);
        } else {
            return result;
        }
    }

    private V getOrThrow0() throws E {
        if (exception != null) {
            throw exception;
        } else {
            return result;
        }
    }

    private boolean setState(final int newState, final V result, final E exception) {
        synchronized (this) {
            if (state != PENDING) {
                // Already completed.
                return false;
            }
            this.result = result;
            this.exception = exception;
            state = newState; // Publishes.
            notifyAll(); // Wake up any blocked threads.
        }
        StateListener<V, E> listener;
        while ((listener = listeners.poll()) != null) {
            listener.handleStateChange(newState, result, exception);
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
