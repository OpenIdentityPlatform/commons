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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;

/**
 * A {@code Promise} represents the result of an asynchronous task.
 *
 * @param <V>
 *            The type of the task's result, or {@link Void} if the task does
 *            not return anything (i.e. it only has side-effects).
 * @param <E>
 *            The type of the exception thrown by the task if it fails, or
 *            {@link NeverThrowsException} if the task cannot fail.
 * @see PromiseImpl
 * @see Promises
 */
public interface Promise<V, E extends Exception> extends Future<V> {
    // TODO: progressible promise

    /**
     * Attempts to cancel the asynchronous task associated with this
     * {@code Promise}. Cancellation will fail if this {@code Promise} has
     * already completed or has already been cancelled. If successful, then
     * cancellation will complete this {@code Promise} with an appropriate
     * exception and notify any registered functions and completion handlers.
     * <p>
     * After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}. Subsequent calls to {@link #isCancelled} will
     * always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing executing the response
     *            handler should be interrupted; otherwise, in-progress response
     *            handlers are allowed to complete.
     * @return {@code false} if {@code Promise} could not be cancelled,
     *         typically because it has already completed normally; {@code true}
     *         otherwise.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Waits if necessary for this {@code Promise} to complete, and then returns
     * the result if it completed successfully, or throws an
     * {@code ExecutionException} containing the cause of the failure.
     *
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws ExecutionException
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully. The {@code ExecutionException} will contain the
     *             cause of the failure.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    @Override
    V get() throws ExecutionException, InterruptedException;

    /**
     * Waits if necessary for at most the given time for this {@code Promise} to
     * complete, and then returns the result if it completed successfully, or
     * throws an {@code ExecutionException} containing the cause of the failure.
     *
     * @param timeout
     *            The maximum time to wait.
     * @param unit
     *            The time unit of the timeout argument.
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws ExecutionException
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully. The {@code ExecutionException} will contain the
     *             cause of the failure.
     * @throws TimeoutException
     *             If the wait timed out.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    @Override
    V get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException,
            InterruptedException;

    /**
     * Waits if necessary for this {@code Promise} to complete, and then returns
     * the result if it completed successfully, or throws an exception
     * representing the cause of the failure.
     *
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws E
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    V getOrThrow() throws InterruptedException, E;

    /**
     * Waits if necessary for at most the given time for this {@code Promise} to
     * complete, and then returns the result if it completed successfully, or
     * throws an exception representing the cause of the failure.
     *
     * @param timeout
     *            The maximum time to wait.
     * @param unit
     *            The time unit of the timeout argument.
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws E
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully.
     * @throws TimeoutException
     *             If the wait timed out.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    V getOrThrow(long timeout, TimeUnit unit) throws InterruptedException, E, TimeoutException;

    /**
     * Waits if necessary for this {@code Promise} to complete, and then returns
     * the result if it completed successfully, or throws an exception
     * representing the cause of the failure.
     * <p>
     * This method is similar to {@link #getOrThrow()} except that it will
     * ignore thread interrupts. When this method returns the status of the
     * current thread will be interrupted if an interrupt was received while
     * waiting.
     *
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws E
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully.
     */
    V getOrThrowUninterruptibly() throws E;

    /**
     * Waits if necessary for at most the given time for this {@code Promise} to
     * complete, and then returns the result if it completed successfully, or
     * throws an exception representing the cause of the failure.
     * <p>
     * This method is similar to {@link #getOrThrow(long, TimeUnit)} except that
     * it will ignore thread interrupts. When this method returns the status of
     * the current thread will be interrupted if an interrupt was received while
     * waiting.
     *
     * @param timeout
     *            The maximum time to wait.
     * @param unit
     *            The time unit of the timeout argument.
     * @return The result, but only if this {@code Promise} completed
     *         successfully.
     * @throws E
     *             If this {@code Promise} was cancelled or did not complete
     *             successfully.
     * @throws TimeoutException
     *             If the wait timed out.
     */
    V getOrThrowUninterruptibly(long timeout, TimeUnit unit) throws E, TimeoutException;

    /**
     * Returns {@code true} if this {@code Promise} was cancelled before it
     * completed normally.
     *
     * @return {@code true} if this {@code Promise} was cancelled before it
     *         completed normally, otherwise {@code false}.
     */
    @Override
    boolean isCancelled();

    /**
     * Returns {@code true} if this {@code Promise} has completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation. In all of these cases, this method will return {@code true}.
     *
     * @return {@code true} if this {@code Promise} has completed, otherwise
     *         {@code false}.
     */
    @Override
    boolean isDone();

    /**
     * Registers the provided completion handler for notification if this
     * {@code Promise} cannot be completed due to an exception. If this
     * {@code Promise} completes with a result then the completion handler
     * will not be notified.
     * <p>
     * This method can be used for asynchronous completion notification.
     *
     * @param onException
     *            The completion handler which will be notified upon failure
     *            completion of this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenOnException(ExceptionHandler<? super E> onException);

    /**
     * Registers the provided completion handler for notification once this
     * {@code Promise} has completed with a result. If this {@code Promise}
     * completes with an exception then the completion handler will not be
     * notified.
     * <p>
     * This method can be used for asynchronous completion notification and is
     * equivalent to {@link #then(Function)}.
     *
     * @param onResult
     *            The completion handler which will be notified upon successful
     *            completion of this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenOnResult(ResultHandler<? super V> onResult);

    /**
     * Registers the provided completion handlers for notification once this
     * {@code Promise} has completed (with a result or an exception). If this
     * {@code Promise} completes with a result then {@code onResult} will be
     * notified with the result, otherwise {@code onException} will be notified
     * with the exception that occurred.
     * <p>
     * This method can be used for asynchronous completion notification.
     *
     * @param onResult
     *            The completion handler which will be notified upon completion
     *            with a result of this {@code Promise}.
     * @param onException
     *            The completion handler which will be notified upon failure of
     *            this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenOnResultOrException(ResultHandler<? super V> onResult, ExceptionHandler<? super E> onException);

    /**
     * Submits the provided runnable for execution once this {@code Promise} has
     * completed, and regardless of whether it has a result or an exception.
     * <p>
     * This method can be used for resource cleanup after a series of
     * asynchronous tasks have completed. More specifically, this method should
     * be used in a similar manner to {@code finally} statements in
     * {@code try...catch} expressions.
     * <p>
     * This method is equivalent to {@link #thenAlways(Runnable)}.
     *
     * @param onResultOrException
     *            The runnable which will be notified regardless of the final
     *            outcome of this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenOnResultOrException(Runnable onResultOrException);

    /**
     * Submits the provided function for execution once this {@code Promise} has
     * completed with a result, and returns a new {@code Promise} representing
     * the outcome of the function. If this {@code Promise} does not
     * complete with a result then the function will not be invoked and the exception
     * will be forwarded to the returned {@code Promise}.
     * <p>
     * This method can be used for transforming the result of an asynchronous
     * task.
     *
     * @param <VOUT>
     *            The type of the function's result, or {@link Void} if the
     *            function does not return anything (i.e. it only has
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param onResult
     *            The function which will be executed upon successful completion
     *            of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    <VOUT> Promise<VOUT, E> then(Function<? super V, VOUT, E> onResult);

    /**
     * Submits the provided function for execution once this {@code Promise} has
     * not completed with a result (has completed with an exception), and returns
     * a new {@code Promise} representing the outcome of the function.
     * If this {@code Promise} completes with a result then the function will not
     * be invoked and the result notification will be forwarded to the returned
     * {@code Promise}.
     * <p>
     * This method can be used for transforming the result of an asynchronous
     * task.
     *
     * @param <EOUT>
     *            The type of the exception thrown by the function if it
     *            fails, or {@link NeverThrowsException} if it cannot fails.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onException
     *            The function which will be executed upon failure completion
     *            of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    <EOUT extends Exception> Promise<V, EOUT> thenCatch(Function<? super E, V, EOUT> onException);

    /**
     * Submits the provided function for execution once this {@code Promise} has
     * not completed with a result nor with an exception but with a {@link RuntimeException}, and returns
     * a new {@code Promise} representing the outcome of the function.
     * If this {@code Promise} completes with a result or an exception then the function will not
     * be invoked and the result notification will be forwarded to the returned
     * {@code Promise}.
     * <p>
     * This method can be used for transforming the result of an asynchronous
     * task.
     *
     * @param onRuntimeException
     *            The function which will be executed upon failure completion
     *            of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    Promise<V, E> thenCatchRuntimeException(Function<? super RuntimeException, V, E> onRuntimeException);

    /**
     * Submits the provided asynchronous function for execution once this
     * {@code Promise} has completed with a {@link RuntimeException}, and returns a new
     * {@code Promise} representing the outcome of the function. If this
     * {@code Promise} completes with a result or the typed exception then the
     * completion asynchronous function will not be called.
     * <p>
     * This method may be used for chaining together a series of asynchronous
     * tasks.
     *
     * @param onRuntimeException
     *            The asynchronous function which will be executed upon failure completion
     *            with a {@link RuntimeException} of this {@code Promise}.
     *
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    Promise<V, E> thenCatchRuntimeExceptionAsync(AsyncFunction<? super RuntimeException, V, E> onRuntimeException);

    /**
     * Submits the provided functions for execution once this {@code Promise}
     * has completed (with a result or an exception), and returns a new
     * {@code Promise} representing the outcome of the invoked function. If
     * this {@code Promise} completes with a result then {@code onResult}
     * will be invoked with the result, otherwise {@code onException} will
     * be invoked with the exception that occurred.
     * <p>
     * This method can be used for transforming the outcome of an
     * asynchronous task.
     *
     * @param <VOUT>
     *            The type of the functions' result, or {@link Void} if the
     *            functions do not return anything (i.e. they only have
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param <EOUT>
     *            The type of the exception thrown by the functions if they
     *            fail, or {@link NeverThrowsException} if they cannot fail.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onResult
     *            The function which will be executed upon successful completion
     *            of this {@code Promise}.
     * @param onException
     *            The function which will be executed upon failure of this
     *            {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         invoked function.
     */
    <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
            Function<? super V, VOUT, EOUT> onResult, Function<? super E, VOUT, EOUT> onException);

    /**
     * Submits the provided functions for execution once this {@code Promise}
     * has completed (with a result or an exception or a {@link RuntimeException}), and returns a new
     * {@code Promise} representing the outcome of the invoked function. If
     * this {@code Promise} completes with a result then {@code onResult}
     * will be invoked with the result, with a {@link RuntimeException} then {@code onRuntimeException}
     * will be invoked with the runtime exception that occurred, otherwise {@code onException} will
     * be invoked with the exception that occurred.
     * <p>
     * This method can be used for transforming the outcome of an
     * asynchronous task.
     *
     * @param <VOUT>
     *            The type of the functions' result, or {@link Void} if the
     *            functions do not return anything (i.e. they only have
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param <EOUT>
     *            The type of the exception thrown by the functions if they
     *            fail, or {@link NeverThrowsException} if they cannot fail.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onResult
     *            The function which will be executed upon successful completion
     *            of this {@code Promise}.
     * @param onException
     *            The function which will be executed upon failure of this
     *            {@code Promise}.
     * @param onRuntimeException
     *            The function which will be executed upon failure with
     *            {@link RuntimeException} of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         invoked function.
     */
    <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> then(
            Function<? super V, VOUT, EOUT> onResult, Function<? super E, VOUT, EOUT> onException,
            Function<? super RuntimeException, VOUT, EOUT> onRuntimeException);


    /**
     * Submits the provided runnable for execution once this {@code Promise} has
     * completed, and regardless of whether it has a result or an exception.
     * <p>
     * This method can be used for resource cleanup after a series of
     * asynchronous tasks have completed. More specifically, this method should
     * be used in a similar manner to {@code finally} statements in
     * {@code try...catch} expressions.
     * <p>
     * This method is equivalent to {@link #thenOnResultOrException(Runnable)}.
     *
     * @param onResultOrException
     *            The runnable which will be notified regardless of the final
     *            outcome of this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenAlways(Runnable onResultOrException);

    /**
     * Submits the provided runnable for execution once this {@code Promise} has
     * completed, and regardless of whether of its outcome.
     * <p>
     * This method can be used for resource cleanup after a series of
     * asynchronous tasks have completed. More specifically, this method should
     * be used in a similar manner to {@code finally} statements in
     * {@code try...catch} expressions.
     * <p>
     * This method is equivalent to {@link #thenAlways(Runnable)}.
     *
     * @param onResultOrException
     *            The runnable which will be notified regardless of the final
     *            outcome of this {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenFinally(Runnable onResultOrException);

    /**
     * Submits the provided asynchronous function for execution once this
     * {@code Promise} has completed with a result, and returns a new
     * {@code Promise} representing the outcome of the function. If
     * this {@code Promise} complete with an exception then the function
     * will not be invoked and the error will be forwarded to the returned
     * {@code Promise}.
     * <p>
     * This method may be used for chaining together a series of asynchronous
     * tasks.
     *
     * @param <VOUT>
     *            The type of the function's result, or {@link Void} if the
     *            function does not return anything (i.e. it only has
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param onResult
     *            The asynchronous function which will be executed upon
     *            successful completion of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    <VOUT> Promise<VOUT, E> thenAsync(AsyncFunction<? super V, VOUT, E> onResult);

    /**
     * Submits the provided asynchronous function for execution once this
     * {@code Promise} has completed with an exception, and returns a new
     * {@code Promise} representing the outcome of the function. If
     * this {@code Promise} completes with a result then the function
     * will not be invoked and the exception will be forwarded to the returned
     * {@code Promise}.
     * <p>
     * This method may be used for chaining together a series of asynchronous
     * tasks.
     *
     * @param <EOUT>
     *            The type of the exception thrown by the function if it
     *            fails, or {@link NeverThrowsException} if it cannot fails.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onException
     *            The asynchronous function which will be executed upon failure completion
     *            of this {@code Promise}.
     *
     * @return A new {@code Promise} representing the outcome of the
     *         function.
     */
    <EOUT extends Exception> Promise<V, EOUT> thenCatchAsync(AsyncFunction<? super E, V, EOUT> onException);

    /**
     * Submits the provided asynchronous functions for execution once this
     * {@code Promise} has completed, and returns a new {@code Promise}
     * representing the outcome of the invoked function. If this
     * {@code Promise} completes with a result then {@code onResult} will be
     * invoked with the result, otherwise {@code onException} will be invoked with
     * the exception that occurred.
     * <p>
     * This method may be used for chaining together a series of asynchronous
     * tasks.
     *
     * @param <VOUT>
     *            The type of the functions' result, or {@link Void} if the
     *            functions do not return anything (i.e. they only have
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param <EOUT>
     *            The type of the exception thrown by the functions if they
     *            fail, or {@link NeverThrowsException} if they cannot fail.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onResult
     *            The asynchronous function which will be executed upon
     *            successful completion of this {@code Promise}.
     * @param onException
     *            The asynchronous function which will be executed upon failure
     *            of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         invoked function.
     */
    <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            AsyncFunction<? super V, VOUT, EOUT> onResult,
            AsyncFunction<? super E, VOUT, EOUT> onException);

    /**
     * Submits the provided asynchronous functions for execution once this
     * {@code Promise} has completed, and returns a new {@code Promise}
     * representing the outcome of the invoked function. If this
     * {@code Promise} completes with a result then {@code onResult} will be
     * invoked with the result, otherwise {@code onException} will be invoked with
     * the exception that occurred, or {@code onRuntimeException} will be invoked with
     * the runtime exception that occurred.
     * <p>
     * This method may be used for chaining together a series of asynchronous
     * tasks.
     *
     * @param <VOUT>
     *            The type of the functions' result, or {@link Void} if the
     *            functions do not return anything (i.e. they only have
     *            side-effects). Note that the type may be different to the type
     *            of this {@code Promise}.
     * @param <EOUT>
     *            The type of the exception thrown by the functions if they
     *            fail, or {@link NeverThrowsException} if they cannot fail.
     *            Note that the type may be different to the type of this
     *            {@code Promise}.
     * @param onResult
     *            The asynchronous function which will be executed upon
     *            successful completion of this {@code Promise}.
     * @param onException
     *            The asynchronous function which will be executed upon failure
     *            of this {@code Promise}.
     * @param onRuntimeException
     *            The asynchronous function which will be executed upon failure
     *            with {@link RuntimeException} of this {@code Promise}.
     * @return A new {@code Promise} representing the outcome of the
     *         invoked function.
     */
    <VOUT, EOUT extends Exception> Promise<VOUT, EOUT> thenAsync(
            AsyncFunction<? super V, VOUT, EOUT> onResult,
            AsyncFunction<? super E, VOUT, EOUT> onException,
            AsyncFunction<? super RuntimeException, VOUT, EOUT> onRuntimeException);

    /**
     * Registers the provided completion handler for notification if this
     * {@code Promise} cannot be completed due to an runtime exception. If this
     * {@code Promise} completes with a result or the typed exception then the
     * completion handler will not be notified.
     * <p>
     * This method can be used for asynchronous completion notification.
     *
     * @param onRuntimeException
     *            The completion handler which will be notified upon an
     *            uncaught runtime exception completion of this
     *            {@code Promise}.
     * @return This {@code Promise}.
     */
    Promise<V, E> thenOnRuntimeException(RuntimeExceptionHandler onRuntimeException);
}
