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
 *      Portions Copyrighted 2026 3A Systems, LLC
 */
package org.forgerock.audit.events.handlers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT;
import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_TIMEOUT_UNIT;
/**
 * A Text Writer which writes log records asynchronously to character-based stream.
 * <p>
 * The records are buffered in a queue and written asynchronously. If maximum CAPACITY of the queue is
 * reached, then calls to {@code write()} method are blocked. This prevent OOM errors while allowing
 * good write performances.
 * <p>
 * The records are written later, so the failure of a record cannot reach its caller. Once the wrapped writer fails,
 * every later {@code write()} and {@code flush()} fails instead, with that first failure as its cause, for the life of
 * this writer: the caller replaces it after a failure, as it replaces the writers of this package that it wraps, which
 * fail their later writes too.
 */
public class AsynchronousTextWriter implements TextWriter {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousTextWriter.class);
    /** System property overriding the default queue capacity. */
    private static final String CAPACITY_PROPERTY =
            "org.forgerock.audit.events.handlers.writers.AsynchronousTextWriter.CAPACITY";
    /** Default queue capacity used when {@link #CAPACITY_PROPERTY} is unset or invalid. */
    private static final int DEFAULT_CAPACITY = 32000;
    /** Maximum number of messages that can be queued before producers start to block. */
    private static final int CAPACITY = readCapacity();

    private static int readCapacity() {
        final String value = System.getProperty(CAPACITY_PROPERTY, String.valueOf(DEFAULT_CAPACITY));
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            logger.warn("Invalid value '{}' for system property {}; using default {}.",
                    value, CAPACITY_PROPERTY, DEFAULT_CAPACITY);
            return DEFAULT_CAPACITY;
        }
    }

    /** The wrapped Text Writer. */
    private final TextWriter writer;

    /** Queue to store unpublished records. */
    private final BlockingQueue<String> queue;
    /** Single threaded executor which runs the WriterTask. */
    private final ExecutorService executorService;
    /** Flag for determining if the wrapped TextWriter should be flushed after each event is written. */
    private final boolean autoFlush;
    /** Flag for notifying the WriterTask to exit. */
    private volatile boolean stopRequested;
    /**
     * The first write or flush of the wrapped writer that failed. The next records fail, so that the caller learns of
     * it and replaces this writer: the records are written later, so their caller cannot be told.
     */
    private volatile IOException failure;
    /** The number of records that the wrapped writer failed to write. */
    private final AtomicLong failedRecords = new AtomicLong();

    /**
     * Construct a new AsynchronousTextWriter wrapper.
     *
     * @param name
     *            the name of the thread.
     * @param autoFlush
     *            indicates if the underlying writer should be flushed after the queue is flushed.
     * @param writer
     *            a character stream used for output.
     */
    public AsynchronousTextWriter(final String name, final boolean autoFlush, final TextWriter writer) {
        Reject.ifNull(writer);
        this.autoFlush = autoFlush;
        this.writer = writer;
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

    /**
     * The publisher thread is responsible for emptying the queue of log records waiting to published.
     */
    private class WriterTask implements Runnable {

        /**
         * Runs until queue is empty AND we've been asked to terminate.
         */
        @Override
        public void run() {
        	final  List<String> drainList = new ArrayList<>();
        	while (!stopRequested || !queue.isEmpty()) {
        		try {
        			if (queue.remainingCapacity()==0) {
        				logger.error("increase queue size -Dorg.forgerock.audit.events.handlers.writers.AsynchronousTextWriter.CAPACITY={}",CAPACITY);
        			}
        			queue.drainTo(drainList);
                    if (drainList.isEmpty()) {
                    	final String message = queue.poll(POLLING_TIMEOUT, POLLING_TIMEOUT_UNIT);
    					if (message != null) {
                            writeMessage(message);
                        }
                    } else {
                        for (String message : drainList) {
                            writeMessage(message);
                        }
                        drainList.clear();
                    }
                    if (autoFlush) {
                        flushWrappedWriter();
                    }
				} catch (InterruptedException e) {
					stopRequested=true;
				}
            }
        }
    }

    private void writeMessage(String message) {
        try {
            writer.write(message);
        } catch (IOException e) {
            failedRecords.incrementAndGet();
            fail(e, "Error when writing a message, message size: " + message.length());
        }
    }

    private void flushWrappedWriter() {
        try {
            writer.flush();
        } catch (IOException e) {
            fail(e, "Error when flushing the writer");
        }
    }

    /** Logs the first failure of the wrapped writer with its stack trace, and only counts the next ones. */
    private IOException fail(final IOException e, final String message) {
        final boolean first;
        synchronized (this) {
            first = failure == null;
            if (first) {
                failure = e;
            }
        }
        if (first) {
            logger.error(message, e);
        } else {
            logger.debug(message, e);
        }
        return e;
    }

    private void checkNotFailed() throws IOException {
        final IOException firstFailure = failure;
        if (firstFailure != null) {
            throw new IOException("A previous write of the buffered records failed", firstFailure);
        }
    }

    /**
     * Write the log record asynchronously.
     *
     * @param record
     *            the log record to write.
     */
    @Override
    public void write(String record) throws IOException {
        checkNotFailed();
        boolean interrupted = false;
        boolean enqueued = false;
        while (!stopRequested) {
            // Put request on queue for writer
            try {
                queue.put(record);
                enqueued = true;
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
        // Inform caller if this writer has been shutdown
        if (!enqueued) {
            throw new IOException("Writer closed");
        }
    }

    @Override
    public void flush() throws IOException {
        checkNotFailed();
        try {
            writer.flush();
        } catch (IOException e) {
            throw fail(e, "Error when flushing the writer");
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
        stopRequested = true;

        // Wait for writer thread to terminate
        executorService.shutdown();
        boolean interrupted = false;
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        final long lostRecords = failedRecords.get();
        if (lostRecords > 0) {
            logger.error("{} buffered records could not be written", lostRecords);
        }

        // Shutdown the wrapped writer.
        if (shutdownWrapped) {
            writer.shutdown();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
