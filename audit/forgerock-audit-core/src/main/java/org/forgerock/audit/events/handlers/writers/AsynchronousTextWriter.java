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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 *      Portions Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.audit.events.handlers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Text Writer which writes log records asynchronously to character-based stream.
 * <p>
 * The records are buffered in a queue and written asynchronously. If maximum capacity of the queue is
 * reached, then calls to {@code write()} method are blocked. This prevent OOM errors while allowing
 * good write performances.
 */
public class AsynchronousTextWriter implements TextWriter {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousTextWriter.class);

    /** The wrapped Text Writer. */
    private final TextWriter writer;

    /** Queue to store unpublished records. */
    private final LinkedBlockingQueue<String> queue;

    /** The capacity for the queue. */
    private final int capacity;

    private final AtomicBoolean stopRequested;
    private final WriterThread writerThread;
    private final boolean autoFlush;

    /**
     * Construct a new AsynchronousTextWriter wrapper.
     *
     * @param name
     *            the name of the thread.
     * @param capacity
     *            the size of the queue before it gets flushed.
     * @param autoFlush
     *            indicates if the underlying writer should be flushed after the queue is flushed.
     * @param writer
     *            a character stream used for output.
     */
    public AsynchronousTextWriter(String name, int capacity, boolean autoFlush, TextWriter writer) {
        Reject.ifNull(writer);
        this.autoFlush = autoFlush;
        this.writer = writer;

        this.queue = new LinkedBlockingQueue<>(capacity);
        this.capacity = capacity;
        this.stopRequested = new AtomicBoolean(false);

        this.writerThread = new WriterThread(name);
        this.writerThread.start();
    }

    /**
     * The publisher thread is responsible for emptying the queue of log records waiting to published.
     */
    private class WriterThread extends Thread {

        WriterThread(String name) {
            super(name);
        }

        /**
         * Runs until queue is empty AND we've been asked to terminate.
         */
        @Override
        public void run() {
            ArrayList<String> drainList = new ArrayList<>(capacity);

            while (!stopRequested.get() || !queue.isEmpty()) {
                try {
                    queue.drainTo(drainList, capacity);
                    if (drainList.isEmpty()) {
                        String message = queue.poll(10, TimeUnit.SECONDS);
                        if (message != null) {
                            writeMessage(message);
                            if (autoFlush) {
                                flush();
                            }
                        }
                    } else {
                        for (String message : drainList) {
                            writeMessage(message);
                        }
                        drainList.clear();
                        if (autoFlush) {
                            flush();
                        }
                    }
                } catch (InterruptedException ex) {
                    // Ignore. We'll rerun the loop
                    // and presumably fall out.
                }
            }
        }
    }

    private void writeMessage(String message) {
        try {
            writer.write(message);
        } catch (IOException e) {
            logger.error("Error when writing a message, message size: " + message.length(), e);
        }
    }

    /**
     * Write the log record asynchronously.
     *
     * @param record
     *            the log record to write.
     */
    @Override
    public void write(String record) {
        while (!stopRequested.get()) {
            // Put request on queue for writer
            try {
                queue.put(record);
                break;
            } catch (InterruptedException e) {
                // We expect this to happen. Just ignore it and hopefully
                // drop out in the next try.
            }
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            logger.error("Error  when flushing the writer", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getBytesWritten() {
        return writer.getBytesWritten();
    }

    /**
     * Retrieves the wrapped writer.
     *
     * @return The wrapped writer used by this asynchronous writer.
     */
    public TextWriter getWrappedWriter() {
        return writer;
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        shutdown(true);
    }

    /**
     * Releases any resources held by the writer.
     *
     * @param shutdownWrapped
     *            If the wrapped writer should be closed as well.
     */
    public void shutdown(boolean shutdownWrapped) {
        stopRequested.set(true);

        // Wait for writer thread to terminate
        while (writerThread.isAlive()) {
            try {
                // Interrupt the thread if its blocking
                writerThread.interrupt();
                writerThread.join();
            } catch (InterruptedException ex) {
                // Ignore; we gotta wait..
            }
        }

        // The writer writerThread SHOULD have drained the queue.
        // If not, handle outstanding requests ourselves,
        // and push them to the writer.
        if (!queue.isEmpty()) {
            while (!queue.isEmpty()) {
                String message = queue.poll();
                writeMessage(message);
            }
            if (autoFlush) {
                flush();
            }
        }

        // Shutdown the wrapped writer.
        if (shutdownWrapped) {
            writer.shutdown();
        }
    }
}
