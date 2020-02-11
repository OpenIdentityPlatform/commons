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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.jms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT;
import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT_UNIT;

/**
 * Generic publisher that will queue anything for batch processing.
 *
 * @param <T> This is the type of object that will be queued before publishing.
 */
public abstract class BatchPublisher<T> implements Publisher<T> {
    private static final Logger logger = LoggerFactory.getLogger(BatchPublisher.class);

    private final BlockingQueue<T> queue;
    private final ExecutorService executorService;
    private final long insertTimeoutSec;
    private final long shutdownTimeoutSec;
    private volatile boolean stopRequested;
    private final int maxBatchedEvents;

    /**
     * This constructs the thread pool of worker threads.  The pool is not executed until {@link #startup()}.
     *
     * @param name Name given to the thread pool worker threads.
     * @param configuration queue management and thread pool configuration settings.
     */
    public BatchPublisher(final String name, final BatchPublisherConfiguration configuration) {
        Reject.ifNull(configuration, "Batch configuration can't be null.");
        Reject.ifFalse(configuration.getThreadCount() > 0, "ThreadCount must be greater than 0");
        Reject.ifFalse(configuration.getCapacity() > 0, "Capacity must be greater than 0");
        Reject.ifFalse(configuration.getMaxBatchedEvents() > 0, "MaxBatchedEvents must be greater than 0");
        this.queue = new LinkedBlockingQueue<>(configuration.getCapacity());
        this.maxBatchedEvents = configuration.getMaxBatchedEvents();
        this.insertTimeoutSec = configuration.getInsertTimeoutSec();
        this.shutdownTimeoutSec = configuration.getShutdownTimeoutSec();
        this.stopRequested = false;
        this.executorService = Executors.newFixedThreadPool(configuration.getThreadCount(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, name);
            }
        });
    }

    /**
     * This is invoked by {@link #startup()}.  This should be implemented to initialize any resources that need
     * to be started when the publisher is started. For example, opening shared connections to remote services.
     *
     * @throws ResourceException if there is trouble starting the publisher.
     */
    protected abstract void startupPublisher() throws ResourceException;

    /**
     * This is invoked by {@link #shutdown()}.  This should be implemented to clean up any resources that were
     * initialized in startup. For exmaple, closing the connections to remote services.
     *
     * @throws ResourceException if there is trouble shutting down the publisher.
     */
    protected abstract void shutdownPublisher() throws ResourceException;

    /**
     * This is invoked by the worker threads to have the passed in messages published immediately.
     *
     * @param messages the messages to publish immediately.
     */
    protected abstract void publishMessages(List<T> messages);

    /**
     * This first initializes the worker threads that monitor the queue of items to publish, and then calls
     * {@link #startupPublisher()}.
     *
     * @throws ResourceException If there is trouble starting up the publisher or starting the worker threads.
     */
    @Override
    public final void startup() throws ResourceException {
        stopRequested = false;
        this.executorService.execute(new PublishTask());
        startupPublisher();
    }

    /**
     * This shutdowns the worker threads, and then calls {@link #shutdownPublisher()}.
     *
     * @throws ResourceException if there is trouble shutting down the publisher or stopping the worker threads.
     */
    @Override
    public final void shutdown() throws ResourceException {
        stopRequested = true;
        executorService.shutdown();
        boolean interrupted = false;
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(shutdownTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        shutdownPublisher();
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Offers the message to the queue.  If the offer isn't accepted for 1 minute, the message is lost.
     *
     * @param message the message to queue.
     */
    @Override
    public final void publish(T message) {
        boolean interrupted = false;
        while (!stopRequested) {
            // Put request on queue for worker thread
            try {
                if (queue.offer(message, insertTimeoutSec, TimeUnit.SECONDS)) {
                    break;
                } else {
                    logger.info(getClass() + " was blocked from queueing. Perhaps more worker threads are needed.");
                }
            } catch (InterruptedException e) {
                // We expect this to happen. Just ignore it and hopefully
                // drop out in the next try.
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This runnable defines the logic of the worker threads that process the queue.
     *
     * @see BlockingQueue#drainTo(java.util.Collection, int)
     * @see BlockingQueue#poll(long, TimeUnit)
     * @see Executors#newFixedThreadPool(int, ThreadFactory)
     */
    private class PublishTask implements Runnable {

        /**
         * While the queue isn't empty this will drain the queue into a list and process them in a single call to
         * {@link #publishMessages(List)}. <br/>
         * If the drain results in an empty list, then this will poll for a single item and process that item as a
         * singleton batch. <br/>
         * If the poll timeouts ({@link BatchPublisherConfiguration#pollTimeoutSec }), and the queue is still
         * empty, then the run will exit.<br/>
         */
        @Override
        public void run() {
            List<T> drainList = new ArrayList<>(maxBatchedEvents);

            boolean interrupted = false;
            while (!stopRequested || !queue.isEmpty()) {
                try {
                    queue.drainTo(drainList, maxBatchedEvents);
                    if (drainList.isEmpty()) {
                        T message = queue.poll(POLLING_TIMEOUT, POLLING_TIMEOUT_UNIT);
                        if (message != null) {
                            publishMessages(Collections.singletonList(message));
                        }
                    } else {
                        publishMessages(drainList);
                        drainList.clear();
                    }
                } catch (InterruptedException ex) {
                    // Ignore. We'll rerun the loop
                    // and presumably fall out.
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
