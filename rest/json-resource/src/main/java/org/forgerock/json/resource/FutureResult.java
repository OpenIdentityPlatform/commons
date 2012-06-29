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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.forgerock.json.resource.exception.ResourceException;

/**
 * A handle which can be used to retrieve the result of an asynchronous request.
 *
 * @param <V>
 *            The type of result returned by this future.
 */
public interface FutureResult<V> extends Future<V> {
    /**
     * Attempts to cancel the request. This attempt will fail if the request has
     * already completed or has already been cancelled.
     * <p>
     * After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}. Subsequent calls to {@link #isCancelled} will
     * always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing executing the response
     *            handler should be interrupted; otherwise, in-progress response
     *            handlers are allowed to complete.
     * @return {@code false} if the request could not be cancelled, typically
     *         because it has already completed normally; {@code true}
     *         otherwise.
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Waits if necessary for the request to complete, and then returns the
     * result if the request succeeded.
     *
     * @return The result but only if the request succeeded.
     * @throws ResourceException
     *             If request failed for some reason.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    V get() throws ResourceException, InterruptedException;

    /**
     * Waits if necessary for at most the given time for the request to
     * complete, and then returns the result if the request succeeded.
     *
     * @param timeout
     *            The maximum time to wait.
     * @param unit
     *            The time unit of the timeout argument.
     * @return The result, but only if the request succeeded.
     * @throws ResourceException
     *             If the request failed for some reason.
     * @throws TimeoutException
     *             If the wait timed out.
     * @throws InterruptedException
     *             If the current thread was interrupted while waiting.
     */
    V get(long timeout, TimeUnit unit) throws ResourceException, TimeoutException,
            InterruptedException;

    /**
     * Returns {@code true} if the request was cancelled before it completed
     * normally.
     *
     * @return {@code true} if the request was cancelled before it completed
     *         normally, otherwise {@code false}.
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if the request has completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation. In all of these cases, this method will return {@code true}.
     *
     * @return {@code true} if the request has completed, otherwise
     *         {@code false}.
     */
    boolean isDone();
}
