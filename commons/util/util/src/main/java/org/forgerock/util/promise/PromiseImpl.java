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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.util.promise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ExceptionHandler<E>, RuntimeExceptionHandler {
    // TODO: Is using monitor based sync better than AQS?

    private static final Logger LOGGER = LoggerFactory.getLogger(PromiseImpl.class);

    private interface StateListener<V, E extends Exception> {
        void handleStateChange(int newState, V result, E exception, RuntimeException runtimeException);
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
     * State value indicating that this promise has failed with a runtime exception.
     */
    private static final int HAS_RUNTIME_EXCEPTION = 4;

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
        return new PromiseImpl<>();
    }

    private volatile int state = PENDING;
    private V result = null;
    private E exception = null;
    private RuntimeException runtimeException = null;

    private final Queue<StateListener<V, E>> listeners =
            new ConcurrentLinkedQueue<>();

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
        return exception != null && setState(CANCELLED, null, exception, null);
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

    @Override
    public void handleRuntimeException(RuntimeException exception) {
        setState(HAS_RUNTIME_EXCEPTION, null, null, exception);
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
        return setState(HAS_EXCEPTION, null, exception, null);
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
        return setState(HAS_RESULT, result, null, null);
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
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                if (newState == HAS_EXCEPTION || newState == CANCELLED) {
                    try {
                        onException.handleException(exception);
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by ExceptionHandler", e);
                    }
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnResult(final ResultHandler<? super V> onResult) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                if (newState == HAS_RESULT) {
                    try {
                        onResult.handleResult(result);
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by ResultHandler", e);
                    }
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
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                if (newState == HAS_RESULT) {
                    try {
                        onResult.handleResult(result);
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by ResultHandler", e);
                    }
                } else if (newState == HAS_EXCEPTION || newState == CANCELLED) {
                    try {
                        onException.handleException(exception);
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by ExceptionHandler", e);
                    }
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenOnResultOrException(final Runnable onResultOrException) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                if (newState != HAS_RUNTIME_EXCEPTION) {
                    try {
                        onResultOrException.run();
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by Runnable", e);
                    }
                }
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
    public Promise<V, E> thenCatchRuntimeException(
            Function<? super RuntimeException, V, E> onRuntimeException) {
        return then(Promises.<V, E>resultIdempotentFunction(), Promises.<V, E>exceptionIdempotentFunction(),
                onRuntimeException);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
            final Function<? super V, VOUT, EOUT> onResult, final Function<? super E, VOUT, EOUT> onException) {
        return then(onResult, onException, Promises.<VOUT, EOUT>runtimeExceptionIdempotentFunction());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
            final Function<? super V, VOUT, EOUT> onResult, final Function<? super E, VOUT, EOUT> onException,
            final Function<? super RuntimeException, VOUT, EOUT> onRuntimeException) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception,
                                          final RuntimeException runtimeException) {
                try {
                    switch (newState) {
                        case HAS_RESULT:
                            chained.handleResult(onResult.apply(result));
                            break;
                        case HAS_EXCEPTION:
                        case CANCELLED:
                            chained.handleResult(onException.apply(exception));
                            break;
                        case HAS_RUNTIME_EXCEPTION:
                            chained.handleResult(onRuntimeException.apply(runtimeException));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state : " + newState);
                    }
                } catch (final RuntimeException e) {
                    tryHandlingRuntimeException(e, chained);
                } catch (final Exception e) {
                    chained.handleException((EOUT) e);
                }
            }
        });
        return chained;
    }

    private <VOUT, EOUT extends Exception> void tryHandlingRuntimeException(final RuntimeException runtimeException,
            final PromiseImpl<VOUT, EOUT> chained) {
        try {
            chained.handleRuntimeException(runtimeException);
        } catch (Exception ignored) {
            LOGGER.error("Runtime exception handler threw a RuntimeException which cannot be handled!", ignored);
        }
    }

    @Override
    public final Promise<V, E> thenAlways(final Runnable always) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                try {
                    always.run();
                } catch (RuntimeException e) {
                    LOGGER.error("Ignored unexpected exception thrown by Runnable", e);
                }
            }
        });
        return this;
    }

    @Override
    public final Promise<V, E> thenFinally(final Runnable onFinally) {
        return thenAlways(onFinally);
    }

    @Override
    public final <VOUT> Promise<VOUT, E> thenAsync(final AsyncFunction<? super V, VOUT, E> onResult) {
        return thenAsync(onResult, Promises.<VOUT, E>exceptionIdempotentAsyncFunction());
    }

    @Override
    public final <EOUT extends Exception> Promise<V, EOUT> thenCatchAsync(
            AsyncFunction<? super E, V, EOUT> onException) {
        return thenAsync(Promises.<V, EOUT>resultIdempotentAsyncFunction(), onException);
    }

    @Override
    public final Promise<V, E> thenCatchRuntimeExceptionAsync(
            AsyncFunction<? super RuntimeException, V, E> onRuntimeException) {
        return thenAsync(Promises.<V, E>resultIdempotentAsyncFunction(),
                Promises.<V, E>exceptionIdempotentAsyncFunction(), onRuntimeException);
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            final AsyncFunction<? super V, VOUT, EOUT> onResult,
            final AsyncFunction<? super E, VOUT, EOUT> onException) {
        return thenAsync(onResult, onException, Promises.<VOUT, EOUT>runtimeExceptionIdempotentAsyncFunction());
    }

    @Override
    public final <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            final AsyncFunction<? super V, VOUT, EOUT> onResult,
            final AsyncFunction<? super E, VOUT, EOUT> onException,
            final AsyncFunction<? super RuntimeException, VOUT, EOUT> onRuntimeException) {
        final PromiseImpl<VOUT, EOUT> chained = new PromiseImpl<>();
        addOrFireListener(new StateListener<V, E>() {
            @Override
            @SuppressWarnings("unchecked")
            public void handleStateChange(final int newState, final V result, final E exception,
                    final RuntimeException runtimeException) {
                try {
                    switch (newState) {
                        case HAS_RESULT:
                            callNestedPromise(onResult.apply(result));
                            break;
                        case HAS_EXCEPTION:
                        case CANCELLED:
                            callNestedPromise(onException.apply(exception));
                            break;
                        case HAS_RUNTIME_EXCEPTION:
                            callNestedPromise(onRuntimeException.apply(runtimeException));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state : " + newState);
                    }
                } catch (final RuntimeException e) {
                    tryHandlingRuntimeException(e, chained);
                } catch (final Exception e) {
                    chained.handleException((EOUT) e);
                }
            }

            private void callNestedPromise(Promise<? extends VOUT, ? extends EOUT> nestedPromise) {
                nestedPromise
                        .thenOnResult(chained)
                        .thenOnException(chained)
                        .thenOnRuntimeException(chained);
            }
        });
        return chained;
    }

    @Override
    public final Promise<V, E> thenOnRuntimeException(final RuntimeExceptionHandler onRuntimeException) {
        addOrFireListener(new StateListener<V, E>() {
            @Override
            public void handleStateChange(int newState, V result, E exception, RuntimeException runtimeException) {
                if (newState == HAS_RUNTIME_EXCEPTION) {
                    try {
                        onRuntimeException.handleRuntimeException(runtimeException);
                    } catch (RuntimeException e) {
                        LOGGER.error("Ignored unexpected exception thrown by RuntimeExceptionHandler", e);
                    }
                }
            }
        });
        return this;
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
            handleCompletion(listener, stateBefore);
        } else {
            listeners.add(listener);
            final int stateAfter = state;
            if (stateAfter != PENDING && listeners.remove(listener)) {
                handleCompletion(listener, stateAfter);
            }
        }
    }

    private void handleCompletion(final StateListener<V, E> listener, final int completedState) {
        try {
            listener.handleStateChange(completedState, result, exception, runtimeException);
        } catch (RuntimeException ignored) {
            LOGGER.error("State change listener threw a RuntimeException which cannot be handled!", ignored);
        }
    }

    private V get0() throws ExecutionException {
        if (runtimeException != null) {
            throw new ExecutionException(runtimeException);
        } else if (exception != null) {
            throw new ExecutionException(exception);
        } else {
            return result;
        }
    }

    private V getOrThrow0() throws E {
        if (runtimeException != null) {
            throw runtimeException;
        } else if (exception != null) {
            throw exception;
        } else {
            return result;
        }
    }

    private boolean setState(final int newState, final V result, final E exception,
            final RuntimeException runtimeException) {
        synchronized (this) {
            if (state != PENDING) {
                // Already completed.
                return false;
            }
            this.result = result;
            this.exception = exception;
            this.runtimeException = runtimeException;
            state = newState; // Publishes.
            notifyAll(); // Wake up any blocked threads.
        }
        StateListener<V, E> listener;
        while ((listener = listeners.poll()) != null) {
            handleCompletion(listener, newState);
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
