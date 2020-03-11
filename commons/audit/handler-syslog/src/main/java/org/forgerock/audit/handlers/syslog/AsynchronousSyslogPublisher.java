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
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.audit.handlers.syslog;

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT;
import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT_UNIT;

/**
 * SyslogPublisher that offloads message transmission to a separate thread.
 */
class AsynchronousSyslogPublisher implements SyslogPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousSyslogPublisher.class);

    /** Maximum number of messages that can be queued before producers start to block. */
    private static final int CAPACITY = 5000;

    /** SyslogConnection through which buffered messages are sent. */
    private final SyslogConnection connection;
    /** Queue to store unpublished records. */
    private final BlockingQueue<byte[]> queue;
    /** Single threaded executor which runs the WriterTask. */
    private final ExecutorService executorService;
    /** Flag for notifying the WriterTask to exit. */
    private volatile boolean stopRequested;

    /**
     * Construct a new BufferedSyslogPublisher.
     *
     * @param name
     *            the name of the thread.
     * @param connection
     *            a SyslogConnection used for output.
     */
    AsynchronousSyslogPublisher(final String name, final SyslogConnection connection) {
        Reject.ifNull(connection);
        this.connection = connection;
        this.queue = new LinkedBlockingQueue<>(CAPACITY);
        this.stopRequested = false;
        this.executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, name);
            }
        });
        executorService.execute(new WriterTask());
    }

    @Override
    public void publishMessage(String syslogMessage) throws IOException {
        boolean interrupted = false;
        while (!stopRequested) {
            // Put request on queue for writer
            try {
                queue.put(syslogMessage.getBytes(StandardCharsets.UTF_8));
                break;
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

    @Override
    public void close() {
        stopRequested = true;

        executorService.shutdown();
        boolean interrupted = false;
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        // Close the wrapped publisher.
        connection.close();

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void publishBufferedMessages(List<byte[]> syslogMessages) {
        for (byte[] syslogMessage : syslogMessages) {
            try {
                connection.reconnect();
                connection.send(syslogMessage);
            } catch (IOException ex) {
                logger.error("Error when writing a message, message size: " + syslogMessage.length, ex);
                connection.close();
            }
        }
        try {
            connection.flush();
        } catch (IOException ex) {
            logger.error("Error when flushing the connection", ex);
        }
    }

    /**
     * The publisher thread is responsible for emptying the queue of log records waiting to published.
     */
    private class WriterTask implements Runnable {

        /**
         * Runs until queue is empty AND we've been asked to terminate.
         */
        @Override
        public void run() {
            List<byte[]> drainList = new ArrayList<>(CAPACITY);

            boolean interrupted = false;
            while (!stopRequested || !queue.isEmpty()) {
                try {
                    queue.drainTo(drainList, CAPACITY);
                    if (drainList.isEmpty()) {
                        byte[] message = queue.poll(POLLING_TIMEOUT, POLLING_TIMEOUT_UNIT);
                        if (message != null) {
                            publishBufferedMessages(Arrays.asList(message));
                        }
                    } else {
                        publishBufferedMessages(drainList);
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
