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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

/**
 * An interface for implementing different completion handling strategies.
 * Depending on the available Servlet API version a different strategy may be
 * used:
 * <ul>
 * <li>2.5 (JavaEE 5) - synchronous processing and synchronous IO
 * <li>3.0 (JavaEE 6) - asynchronous (non-blocking) processing and synchronous
 * IO
 * <li>3.1 (JavaEE 7) - asynchronous (non-blocking) processing and asynchronous
 * IO (NIO)
 * </ul>
 */
public interface CompletionHandler {

    /**
     * Registers a call-back which will be invoked when asynchronous processing
     * has completed, either successfully, or due to a timeout or error.
     *
     * @param runnable
     *            The call-back to be invoked.
     * @throws IllegalStateException
     *             If this completion handler is not asynchronous.
     */
    void addCompletionListener(Runnable runnable);

    /**
     * Blocks if needed until the response has been sent.
     *
     * @throws Exception
     *             If an unexpected error occurred while waiting.
     */
    void awaitIfNeeded() throws Exception;

    /**
     * Returns {@code true} if this completion handler is non-blocking.
     *
     * @return {@code true} if this completion handler is non-blocking.
     */
    boolean isAsynchronous();

    /**
     * Performs post-completion processing such as completing the AsyncContext
     * (Servlet3) or a latch (Servlet2).
     */
    void onComplete();

    /**
     * Performs post-error processing such as sending error (Servlet3) or a
     * latch (Servlet2).
     *
     * @param t
     *            The error that occurred.
     */
    void onError(Throwable t);
}
