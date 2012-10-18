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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * An abstract future result which acts as a result handler.
 */
abstract class AbstractFutureResultHandler<V, H extends ResultHandler<V>> implements
        FutureResult<V>, ResultHandler<V> {
    private ResourceException error = null;
    private final H innerHandler;
    private final CountDownLatch latch = new CountDownLatch(1);
    private V result = null;

    /**
     * Creates a new future.
     */
    AbstractFutureResultHandler(final H innerHandler) {
        this.innerHandler = innerHandler;
    }

    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
        // Cancellation not supported.
        return false;
    }

    @Override
    public final V get() throws ResourceException, InterruptedException {
        latch.await();
        return get0();
    }

    @Override
    public final V get(final long timeout, final TimeUnit unit) throws ResourceException,
            TimeoutException, InterruptedException {
        if (latch.await(timeout, unit)) {
            return get0();
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public final void handleError(final ResourceException error) {
        try {
            if (innerHandler != null) {
                innerHandler.handleError(error);
            }
        } finally {
            this.error = error;
            latch.countDown();
        }
    }

    @Override
    public final void handleResult(final V result) {
        try {
            if (innerHandler != null) {
                innerHandler.handleResult(result);
            }
        } finally {
            this.result = result;
            latch.countDown();
        }
    }

    @Override
    public final boolean isCancelled() {
        // Cancellation not supported.
        return false;
    }

    @Override
    public final boolean isDone() {
        return latch.getCount() == 0;
    }

    final H getInnerHandler() {
        return innerHandler;
    }

    private V get0() throws ResourceException {
        if (error == null) {
            return result;
        } else {
            throw error;
        }
    }

}
