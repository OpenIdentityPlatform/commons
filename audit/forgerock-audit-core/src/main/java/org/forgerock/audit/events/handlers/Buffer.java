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
package org.forgerock.audit.events.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * A buffer of arbitrary elements, which provides time-based and size-based triggers.
 * <p>
 * This class is thread-safe.
 *
 * @param <T>
 *            The type of elements in the buffer.
 */
public class Buffer<T> implements Closeable {

    private List<T> buffer;
    private final long maxTime;
    private final int maxSize;
    private final ScheduledExecutorService pool;
    private final BufferCallback<T> callback;
    private ScheduledFuture<?> currentTask;

    /** Lock for access to the buffer. */
    private final Lock lock = new ReentrantLock();

    /**
     * Creates a new buffer with the provided pool, callback, max time and max size.
     *
     * @param pool
     *          The thread pool to use for the timer and non-blocking flush.
     * @param callback
     *          The callback called when elements are flushed.
     * @param time
     *            Max time in milliseconds before the events are flushed from the buffer. Zero indicates no flushing is
     *            based on time.
     * @param size
     *            Max size in number of events before the events are flushed from the buffer. It must be strictly
     *            superior to zero.
     */
    public Buffer(ScheduledExecutorService pool, BufferCallback<T> callback, long time, int size) {
        Reject.ifFalse(size > 0);
        this.pool = pool;
        this.callback = callback;
        this.maxTime = time;
        this.maxSize = size;
        this.buffer = new ArrayList<>(maxSize);
        startTimer();
    }

    /**
     * Adds an element to the buffer.
     *
     * @param element
     *            The element to add.
     */
    public void add(T element) {
        lock.lock();
        try {
            buffer.add(element);
            if (buffer.size() >= maxSize) {
                cancelTimer();
                flushEvents(false);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the buffer, thus forcing the flush of the buffer.
     */
    public void reset() {
        lock.lock();
        try {
            cancelTimer();
            flushEvents(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            flushEvents(false);
            cancelTimer();
            pool.shutdown();
        } finally {
            lock.unlock();
        }

    }

    /**
     * Flushes the buffer and reinitializes it.
     *
     * @param blockingCall
     *            Indicates if flushing is performed in blocking mode.
     *            If {@code false} flushing is done in a separate thread.
     */
    private void flushEvents(boolean blockingCall) {
        final List<T> bufferCopy = buffer;
        buffer = new ArrayList<>(maxSize);
        if (!bufferCopy.isEmpty()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    callback.bufferFlush(bufferCopy);
                }
            };
            if (blockingCall) {
                runnable.run();
            }
            else
            {
                pool.execute(runnable);
            }
        }
        startTimer();
    }

    private void startTimer() {
        if (maxTime > 0) {
            currentTask = pool.schedule(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        flushEvents(false);
                    } finally {
                        lock.unlock();
                    }
                }
            }, maxTime, TimeUnit.MILLISECONDS);
        }
    }

    private void cancelTimer() {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
    }

    /** Indicates if the buffer of events is empty. */
    @VisibleForTesting
    boolean isEmpty() {
        lock.lock();
        try {
            return buffer.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
