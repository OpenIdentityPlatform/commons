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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * This class holds the configuration properties that are used by the {#link BatchPublisher} to control the batch queue
 * and worker threads that process the items in the queue.
 */
public class BatchPublisherConfiguration {

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.batchEnabled")
    private boolean batchEnabled = false;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.capacity")
    private int capacity = 1;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.threadCount")
    private int threadCount = 1;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.maxBatchedEvents")
    private int maxBatchedEvents = 1;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.insertTimeoutSec")
    private long insertTimeoutSec = 60L;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.pollTimeoutSec")
    private long pollTimeoutSec = 10L;

    @JsonPropertyDescription("audit.handlers.jms.publisher.batch.shutdownTimeoutSec")
    private long shutdownTimeoutSec = 60L;

    /**
     * Returns the maximum capacity of the publishing queue.  Execution will block if the queue size is at capacity.
     *
     * @return the maximum capacity of the publishing queue
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the maximum capacity of the publishing queue.
     *
     * @param capacity the maximum capacity of the publishing queue
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the count of worker threads to have processing the queue.
     *
     * @return the count of worker threads to have processing the queue.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Sets the count of worker threads to have processing the queue.
     *
     * @param threadCount the count of worker threads to have processing the queue.
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Returns the maximum count of events that will be expected to be delivered in a single publish call.
     *
     * @return the maximum count of events that will be expected to be delivered in a single publish call.
     */
    public int getMaxBatchedEvents() {
        return maxBatchedEvents;
    }

    /**
     * Sets the maximum count of events that will be expected to be delivered in a single publish call.
     *
     * @param maxBatchedEvents the maximum count of events
     */
    public void setMaxBatchedEvents(int maxBatchedEvents) {
        this.maxBatchedEvents = maxBatchedEvents;
    }

    /**
     * Returns the timeout in seconds the duration that the queue should block while attempting to offer a new item
     * for the queue.
     *
     * @return timeout in seconds
     */
    public long getInsertTimeoutSec() {
        return insertTimeoutSec;
    }

    /**
     * Sets the timeout in seconds the duration that the queue should block while attempting to offer a new item
     * for the queue.
     *
     * @param insertTimeoutSec timeout in seconds
     */
    public void setInsertTimeoutSec(long insertTimeoutSec) {
        this.insertTimeoutSec = insertTimeoutSec;
    }

    /**
     * Returns the timeout in seconds for the worker threads to wait for a new item to be available in the queue
     * before exiting.
     *
     * @return timeout in seconds
     */
    public long getPollTimeoutSec() {
        return pollTimeoutSec;
    }

    /**
     * Sets the timeout in seconds for the worker threads to wait for a new item to be available in the queue before
     * exiting.
     *
     * @param pollTimeoutSec timeout in seconds
     */
    public void setPollTimeoutSec(long pollTimeoutSec) {
        this.pollTimeoutSec = pollTimeoutSec;
    }

    /**
     * Returnds the timeout in seconds for the publisher to wait for all worker threads to terminate at shutdown.
     *
     * @return timeout in seconds
     */
    public long getShutdownTimeoutSec() {
        return shutdownTimeoutSec;
    }

    /**
     * Sets  the timeout in seconds for the publisher to wait for all worker threads to terminate at shutdown.
     *
     * @param shutdownTimeoutSec timeout in seconds
     */
    public void setShutdownTimeoutSec(long shutdownTimeoutSec) {
        this.shutdownTimeoutSec = shutdownTimeoutSec;
    }

    /**
     * Returns true if handling of audit events should be done in batches.
     *
     * @return true if handling of audit events should be done in batches.
     */
    public boolean isBatchEnabled() {
        return batchEnabled;
    }

    /**
     * sets if handling of audit events should be done in batches.
     *
     * @param batchEnabled true if handling of audit events should be done in batches.
     */
    public void setBatchEnabled(boolean batchEnabled) {
        this.batchEnabled = batchEnabled;
    }
}
