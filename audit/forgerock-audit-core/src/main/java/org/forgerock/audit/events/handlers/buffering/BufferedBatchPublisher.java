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
package org.forgerock.audit.events.handlers.buffering;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.audit.batch.CommonAuditBatchConfiguration;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffers audit events to a bounded queue, periodically flushing the queue to a provided {@link BatchConsumer}.
 * If the bounded queue becomes full, further events are dropped until the queue is next flushed.
 */
public final class BufferedBatchPublisher implements BatchPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BufferedBatchPublisher.class);

    private final BlockingQueue<BatchEntry> queue;
    private final ScheduledExecutorService scheduler;
    private final QueueConsumer queueConsumer;
    private final Duration writeInterval;

    private BufferedBatchPublisher(BuilderImpl builder) {
        queue = new ArrayBlockingQueue<>(builder.capacity);
        scheduler = Executors.newScheduledThreadPool(1);
        queueConsumer = new QueueConsumer(builder.maxBatchedEvents, builder.averagePerEventPayloadSize,
                builder.autoFlush, queue, scheduler, builder.batchConsumer);
        this.writeInterval = builder.writeInterval;
    }

    /**
     * Starts periodically sending batch data.
     */
    @Override
    public void startup() {
        scheduler.scheduleAtFixedRate(queueConsumer, 0, writeInterval.to(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops sending batch data, and awaits termination of pending queue tasks when {@code autoFlush} is enabled.
     */
    @Override
    public void shutdown() {
        if (!scheduler.isShutdown()) {
            queueConsumer.shutdown();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue if it is possible to do so immediately without
     * exceeding the queue's capacity, returning {@code true} upon success and {@code false} if this queue is full.
     *
     * @param topic
     *         Event topic
     * @param event
     *         Event payload to index, where {@code _id} field is the identifier
     *
     * @return {@code true} if the element was added to this queue, else {@code false}
     */
    @Override
    public boolean offer(final String topic, final JsonValue event) {
        return queue.offer(new BatchEntry(topic, event));
    }

    /**
     * A single audit-event batch entry.
     */
    private static class BatchEntry {

        private final String topic;
        private final JsonValue event;

        /**
         * Creates a new audit-event batch entry.
         *
         * @param topic
         *         Event topic
         * @param event
         *         Event JSON payload
         */
        public BatchEntry(final String topic, final JsonValue event) {
            this.topic = topic;
            this.event = event;
        }

        /**
         * Gets the event JSON payload.
         *
         * @return Event JSON payload
         */
        public JsonValue getEvent() {
            return event;
        }

        /**
         * Gets the event topic.
         *
         * @return Event topic
         */
        public String getTopic() {
            return topic;
        }
    }

    /**
     * Consumer of the audit-event batch queue, which can be scheduled to run periodically. This class is not
     * thread-safe, and is intended to be run by a single thread.
     */
    private static class QueueConsumer implements Runnable {

        private final int maxBatchedEvents;
        private final boolean flushOnShutdown;
        private final BlockingQueue<BatchEntry> queue;
        private final List<BatchEntry> batch;
        private final StringBuilder payload;
        private final BatchConsumer batchEventHandler;
        private final ScheduledExecutorService scheduler;

        private volatile boolean shutdown;

        /**
         * Creates a {@code QueueConsumer}.
         *
         * @param maxBatchedEvents
         *         Batch size
         * @param averagePerEventPayloadSize
         *         Average number of characters, per event, in a batch payload
         * @param flushOnShutdown
         *         When {@code true}, the queue will be flushed on shutdown and when {@code false},
         *         items in the queue will be dropped
         * @param queue
         *         Audit-event queue
         * @param scheduler
         *         This runnable's scheduler
         * @param batchEventHandler
         *         Batch audit event handler
         */
        public QueueConsumer(final int maxBatchedEvents, final int averagePerEventPayloadSize,
                final boolean flushOnShutdown, final BlockingQueue<BatchEntry> queue,
                final ScheduledExecutorService scheduler, final BatchConsumer batchEventHandler) {
            this.queue = queue;
            this.flushOnShutdown = flushOnShutdown;
            this.scheduler = scheduler;
            this.batchEventHandler = batchEventHandler;
            this.maxBatchedEvents = maxBatchedEvents;
            batch = new ArrayList<>(maxBatchedEvents);
            payload = new StringBuilder(maxBatchedEvents * averagePerEventPayloadSize);
        }

        /**
         * Informs queue consumer that shutdown has been triggered, and when {@code flushOnShutdown} is enabled,
         * blocks until all events have been flushed from the queue.
         */
        public void shutdown() {
            if (!shutdown) {
                shutdown = true;

                if (flushOnShutdown) {
                    // flush requested, so block in an non-cancelable way
                    boolean interrupted = false;
                    while (!scheduler.isTerminated()) {
                        try {
                            scheduler.awaitTermination(1L, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        /**
         * Performs batch operation.
         */
        private void batch() {
            queue.drainTo(batch, maxBatchedEvents);
            if (!batch.isEmpty()) {
                try {
                    // add to batch
                    for (final BatchEntry entry : batch) {
                        try {
                            batchEventHandler.addToBatch(entry.getTopic(), entry.getEvent(), payload);
                        } catch (Exception e) {
                            logger.error("addToBatch failed", e);
                        }
                    }

                    // send batch
                    if (payload.length() != 0) {
                        batchEventHandler.publishBatch(payload.toString())
                                .thenCatch(new Function<BatchException, Void, BatchException>() {
                                    @Override
                                    public Void apply(BatchException e) throws BatchException {
                                        logger.error("publishBatch failed", e);
                                        return null;
                                    }
                                });
                    }
                } finally {
                    // clear buffers to prepare for next batch
                    batch.clear();
                    payload.setLength(0);
                }
            }
        }

        @Override
        public void run() {
            if (shutdown) {
                // we shutdown this runnable's scheduler here, so that we can guarantee that flush will proceed
                scheduler.shutdown();
                if (flushOnShutdown) {
                    // flush queue
                    while (!queue.isEmpty()) {
                        batch();
                    }
                }
            }

            // normal run of batch operation
            batch();
        }
    }

    /**
     * Provides a new builder.
     *
     * @param batchConsumer
     *         a non-null batch consumer
     *
     * @return a new builder
     */
    public static Builder newBuilder(final BatchConsumer batchConsumer) {
        return new BuilderImpl(batchConsumer);
    }

    /**
     * Builder used to construct a new {@link BufferedBatchPublisher}.
     */
    public interface Builder {

        /**
         * Sets the maximum queue capacity. Must be >= 10000.
         *
         * @param capacity
         *         queue capacity
         *
         * @return this builder
         */
        Builder capacity(int capacity);

        /**
         * Sets the maximum number of events in a given batch. Must be >= 500.
         *
         * @param maxBatchedEvents
         *         maximum number of batched events
         *
         * @return this builder
         */
        Builder maxBatchEvents(int maxBatchedEvents);

        /**
         * Sets the average event payload size, used to initialise string buffers. Must be >= 32.
         *
         * @param averagePerEventPayloadSize
         *         average event payload size
         *
         * @return this builder
         */
        Builder averagePerEventPayloadSize(int averagePerEventPayloadSize);

        /**
         * The interval duration between each write. Must be > 0.
         *
         * @param writeInterval
         *         write interval
         *
         * @return this builder
         */
        Builder writeInterval(Duration writeInterval);

        /**
         * Whether events to should be automatically flushed on shutdown.
         *
         * @param autoFlush
         *         whether to auto flush
         *
         * @return this builder
         */
        Builder autoFlush(boolean autoFlush);

        /**
         * Constructs a new {@link BatchPublisher}.
         *
         * @return a new {@link BatchPublisher}
         */
        BatchPublisher build();
    }

    private static final class BuilderImpl implements Builder {

        private static final int MIN_QUEUE_SIZE = 10000;
        private static final int MIN_BATCH_SIZE = 500;
        private static final int MIN_PER_EVENT_PAYLOAD_SIZE = 32;

        private final BatchConsumer batchConsumer;

        private int capacity;
        private int maxBatchedEvents;
        private int averagePerEventPayloadSize;
        private Duration writeInterval;
        private boolean autoFlush;

        private BuilderImpl(final BatchConsumer batchConsumer) {
            Reject.ifNull(batchConsumer, "batchConsumer must not be null");
            this.batchConsumer = batchConsumer;
            capacity = MIN_QUEUE_SIZE;
            maxBatchedEvents = MIN_BATCH_SIZE;
            averagePerEventPayloadSize = MIN_PER_EVENT_PAYLOAD_SIZE;
            writeInterval = CommonAuditBatchConfiguration.POLLING_INTERVAL;
        }

        @Override
        public Builder capacity(final int capacity) {
            this.capacity = max(capacity, MIN_QUEUE_SIZE);
            return this;
        }

        @Override
        public Builder maxBatchEvents(final int maxBatchedEvents) {
            this.maxBatchedEvents = max(maxBatchedEvents, MIN_BATCH_SIZE);
            return this;
        }

        @Override
        public Builder averagePerEventPayloadSize(final int averagePerEventPayloadSize) {
            this.averagePerEventPayloadSize = max(averagePerEventPayloadSize, MIN_PER_EVENT_PAYLOAD_SIZE);
            return this;
        }

        @Override
        public Builder writeInterval(final Duration writeInterval) {
            this.writeInterval = (writeInterval != null && writeInterval.getValue() > 0)
                    ? writeInterval : CommonAuditBatchConfiguration.POLLING_INTERVAL;
            return this;
        }

        @Override
        public Builder autoFlush(final boolean autoFlush) {
            this.autoFlush = autoFlush;
            return this;
        }

        @Override
        public BatchPublisher build() {
            return new BufferedBatchPublisher(this);
        }

    }

}
