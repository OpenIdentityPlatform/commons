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

package org.forgerock.audit.handlers.json;

import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.audit.batch.CommonAuditBatchConfiguration.POLLING_INTERVAL;
import static org.forgerock.audit.handlers.json.JsonAuditEventHandler.OBJECT_MAPPER;
import static org.forgerock.audit.handlers.json.JsonAuditEventHandler.EVENT_ID_FIELD;
import static org.forgerock.audit.util.ElasticsearchUtil.normalizeJson;
import static org.forgerock.audit.util.ElasticsearchUtil.renameField;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.util.Reject.checkNotNull;
import static org.forgerock.util.Utils.closeSilently;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.RetentionPolicy;
import org.forgerock.audit.rotation.RotatableObject;
import org.forgerock.audit.rotation.RotationHooks;
import org.forgerock.audit.rotation.RotationPolicy;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Utils;
import org.forgerock.util.time.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically writes JSON events to a file.
 */
class JsonFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileWriter.class);

    private static final int MIN_QUEUE_SIZE = 100_000;

    static final String LOG_FILE_NAME_SUFFIX = "audit.json";

    private final boolean elasticsearchCompatible;
    private final BlockingQueue<QueueEntry> queue;
    private final ScheduledExecutorService scheduler;
    private final QueueConsumer queueConsumer;
    private final Duration writeInterval;

    /**
     * Creates a {@link JsonFileWriter}. For arguments with minimum values, the minimum will be used
     * without warning if the provided value is lower than that minimum.
     *
     * @param topics Supported topics
     * @param configuration Configuration
     * @param autoFlush {@code true} when data in queue should always be flushed on shutdown and {@code false} when
     * it may be discarded
     */
    JsonFileWriter(final Set<String> topics, final JsonAuditEventHandlerConfiguration configuration,
            final boolean autoFlush) {
        elasticsearchCompatible = configuration.isElasticsearchCompatible();
        queue = new ArrayBlockingQueue<>(max(configuration.getBuffering().getMaxSize(), MIN_QUEUE_SIZE));
        scheduler = Executors.newScheduledThreadPool(1, Utils.newThreadFactory(null, "audit-json-%d", false));
        queueConsumer = new QueueConsumer(LOG_FILE_NAME_SUFFIX, topics, configuration, autoFlush, queue, scheduler);
        writeInterval = parseWriteInterval(configuration);
    }

    private Duration parseWriteInterval(final JsonAuditEventHandlerConfiguration configuration) {
        final String writeIntervalString = configuration.getBuffering().getWriteInterval();
        Duration writeInterval;
        try {
            writeInterval = Duration.duration(writeIntervalString);
        } catch (Exception e) {
            writeInterval = null;
        }
        if (writeInterval == null || writeInterval.getValue() <= 0) {
            logger.info("writeInterval '{}' is invalid, so falling back to {}", writeIntervalString, POLLING_INTERVAL);
            return POLLING_INTERVAL;
        }
        return writeInterval;
    }

    /**
     * Starts periodically writing JSON events to a file.
     */
    void startup() {
        scheduler.scheduleAtFixedRate(queueConsumer, 0, writeInterval.to(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops writing JSON events to a file, and awaits termination of pending queue tasks when {@code autoFlush}
     * is enabled.
     */
    void shutdown() {
        if (!scheduler.isShutdown()) {
            queueConsumer.shutdown();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue, and blocks if this queue is full.
     *
     * @param topic Event topic
     * @param event Event payload to index, where {@code _id} field is the identifier
     * @throws InterruptedException thread interrupted while blocking on a full queue
     * @throws IOException failed to serialize JSON
     */
    void put(final String topic, final JsonValue event) throws InterruptedException, IOException {
        if (elasticsearchCompatible) {
            // rename _id field to be _eventId, because _id is reserved by ElasticSearch
            renameField(event, FIELD_CONTENT_ID, EVENT_ID_FIELD);
            try {
                // apply ElasticSearch JSON normalization, if necessary
                final byte[] bytes = normalizeJson(event).getBytes(UTF_8);
                queue.put(new QueueEntry(topic, bytes));
            } finally {
                // restore _id field, because original event is same instance as normalizedEvent
                renameField(event, EVENT_ID_FIELD, FIELD_CONTENT_ID);
            }
        } else {
            queue.put(new QueueEntry(topic, OBJECT_MAPPER.writeValueAsBytes(event.getObject())));
        }
    }

    /**
     * Requests an unscheduled rotation of the underlying JSON audit file.
     * <p>
     * Rotation is only possible when enabled in the {@link JsonAuditEventHandlerConfiguration configuration},
     * and will happen after all preexisting events in the queue have been processed.
     *
     * @param topic Event topic
     * @return {@code true} if rotation is enabled, and {@code false} otherwise
     * @throws InterruptedException thread interrupted while blocking on a full queue
     */
    boolean rotateFile(final String topic) throws InterruptedException {
        if (queueConsumer.isRotationEnabled()) {
            queue.put(new QueueEntry(topic, QueueEntry.ROTATE_FILE_ENTRY));
            return true;
        }
        return false;
    }

    /**
     * Requests an unscheduled buffer-flush of the underlying JSON audit file, which is useful for testing.
     * <p>
     * The flush will happen after all preexisting events in the queue have been processed.
     *
     * @param topic Event topic
     * @throws InterruptedException thread interrupted while blocking on a full queue
     */
    void flushFileBuffer(final String topic) throws InterruptedException {
        queue.put(new QueueEntry(topic, QueueEntry.FLUSH_FILE_ENTRY));
    }

    /**
     * Gets the current log-file for the given topic.
     *
     * @param topic Topic name (case-sensitive)
     * @return {@link Path} or {@code null} if topic is unrecognised
     */
    Path getTopicFilePath(final String topic) {
        final QueueConsumer.TopicEntry topicEntry = queueConsumer.topicEntryMap.get(topic);
        return topicEntry == null ? null : topicEntry.filePath;
    }

    /**
     * A single audit-event entry.
     */
    private static class QueueEntry {

        static final byte[] ROTATE_FILE_ENTRY = new byte[0];
        static final byte[] FLUSH_FILE_ENTRY = new byte[0];

        private final String topic;
        private final byte[] event;

        /**
         * Creates a new audit-event batch entry.
         *
         * @param topic Event topic
         * @param event Event JSON payload
         */
        QueueEntry(final String topic, final byte[] event) {
            this.topic = checkNotNull(topic);
            this.event = checkNotNull(event);
        }

        boolean isRotateEntry() {
            return event == ROTATE_FILE_ENTRY;
        }

        boolean isFlushEntry() {
            return event == FLUSH_FILE_ENTRY;
        }
    }

    /**
     * Consumer of the audit-event batch queue, which can be scheduled to run periodically. This class is not
     * thread-safe, and is intended to be run by a single thread.
     */
    private static final class QueueConsumer implements Runnable {

        private static final int BATCH_SIZE = 5000;
        private static final int OUTPUT_BUF_INITIAL_SIZE = 16 * 1024;
        private static final byte[] NEWLINE_UTF_8_BYTES = "\n".getBytes(UTF_8);

        private final boolean flushOnShutdown;
        private final boolean rotationEnabled;
        private final boolean hasRotationOrRetentionPolicies;
        private final List<RotationPolicy> rotationPolicies;
        private final List<RetentionPolicy> retentionPolicies;
        private final Set<File> filesToDelete;
        private final BlockingQueue<QueueEntry> queue;
        private final ScheduledExecutorService scheduler;
        private final Map<String, TopicEntry> topicEntryMap;
        private final List<QueueEntry> drainList;

        private volatile boolean shutdown;

        /**
         * Creates a {@code QueueConsumer}.
         *
         * @param fileNameSuffix Log file-name suffix
         * @param topics Supported topics
         * @param configuration Configuration
         * @param flushOnShutdown When {@code true}, the queue will be flushed on shutdown and when {@code false},
         * items in the queue will be dropped
         * @param queue Audit-event queue
         * @param scheduler This runnable's scheduler
         */
        private QueueConsumer(final String fileNameSuffix, final Set<String> topics,
                final JsonAuditEventHandlerConfiguration configuration, final boolean flushOnShutdown,
                final BlockingQueue<QueueEntry> queue, final ScheduledExecutorService scheduler) {
            this.queue = queue;
            this.scheduler = scheduler;
            this.flushOnShutdown = flushOnShutdown;
            drainList = new ArrayList<>(BATCH_SIZE);
            rotationEnabled = configuration.getFileRotation().isRotationEnabled();
            rotationPolicies = configuration.getFileRotation().buildRotationPolicies();
            retentionPolicies = configuration.getFileRetention().buildRetentionPolicies();
            hasRotationOrRetentionPolicies = (rotationEnabled && !rotationPolicies.isEmpty())
                    || !retentionPolicies.isEmpty();
            filesToDelete = new HashSet<>();

            // build map of topic files
            final Map<String, TopicEntry> topicEntryMap = new HashMap<>();
            for (final String topic : topics) {
                final String fileName = topic + '.' + fileNameSuffix;
                topicEntryMap.put(topic, new TopicEntry(fileName, configuration));
            }
            this.topicEntryMap = Collections.unmodifiableMap(topicEntryMap);
        }

        /**
         * Informs queue consumer that shutdown has been triggered, and when {@code flushOnShutdown} is enabled,
         * blocks until all events have been flushed from the queue.
         */
        void shutdown() {
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

        @Override
        public void run() {
            if (!shutdown) {
                // following loop will run at least once, even if queue is empty, so that rotation policies will run
                do {
                    writeEvents();
                } while (!queue.isEmpty() && !shutdown);
            }

            if (shutdown) {
                // we shutdown this runnable's scheduler here, so that we can guarantee that flush will proceed
                scheduler.shutdown();
                try {
                    if (flushOnShutdown) {
                        while (!queue.isEmpty()) {
                            writeEvents();
                        }
                        for (final TopicEntry topicEntry : topicEntryMap.values()) {
                            topicEntry.flush();
                        }
                    }
                } finally {
                    closeSilently(topicEntryMap.values());
                }
            }
        }

        private void writeEvents() {
            drainList.clear();
            try {
                // handle one batch of events
                final int n = queue.drainTo(drainList, BATCH_SIZE);
                for (int i = 0; i < n; ++i) {
                    final QueueEntry entry = drainList.get(i);
                    final TopicEntry topicEntry = topicEntryMap.get(entry.topic);
                    if (topicEntry == null) {
                        logger.warn("Unrecognised topic: " + entry.topic);
                    } else {
                        if (entry.isRotateEntry()) {
                            topicEntry.rotateNow();
                        } else if (entry.isFlushEntry()) {
                            topicEntry.flush();
                        } else {
                            topicEntry.write(entry.event);
                        }
                    }
                }
                if (n == 0) {
                    // no new events, so flush all file buffers, to prevent appearance that events are stuck/lost
                    for (final TopicEntry topicEntry : topicEntryMap.values()) {
                        topicEntry.flush();
                    }
                }

                if (hasRotationOrRetentionPolicies) {
                    // enforce rotation and/or retention policies for all topic files
                    for (final TopicEntry topicEntry : topicEntryMap.values()) {
                        topicEntry.rotateIfNeeded();
                    }
                }
            } catch (IOException e) {
                logger.error("JSON file write failed", e);
            } catch (Exception e) {
                logger.error("Unexpected failure", e);
            }
        }

        /**
         * Checks if rotation is enabled.
         *
         * @return {@code true} if rotation is enabled and {@code false} otherwise
         */
        boolean isRotationEnabled() {
            return rotationEnabled;
        }

        /**
         * Represents state for a single topic audit-file.
         */
        private class TopicEntry implements RotatableObject, Closeable {
            private static final int FILE_BUFFER_THRESHOLD = 8 * 1024;

            private final Path filePath;
            private final FileNamingPolicy fileNamingPolicy;
            private final ByteBufferOutputStream outputStream;
            private DateTime lastRotationTime;
            private FileChannel fileChannel;
            private long positionInFile;

            TopicEntry(final String fileName, final JsonAuditEventHandlerConfiguration configuration) {
                try {
                    outputStream = new ByteBufferOutputStream(ByteBuffer.allocateDirect(OUTPUT_BUF_INITIAL_SIZE));

                    final Path directoryPath = Paths.get(configuration.getLogDirectory());
                    if (Files.notExists(directoryPath)) {
                        Files.createDirectory(directoryPath);
                    }
                    filePath = directoryPath.resolve(fileName);
                    if (Files.notExists(filePath)) {
                        fileChannel = FileChannel.open(filePath, StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.WRITE);
                    } else {
                        fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE);
                        positionInFile = fileChannel.size();
                    }

                    final File currentFile = filePath.toFile();
                    fileNamingPolicy = configuration.getFileRotation().buildTimeStampFileNamingPolicy(currentFile);

                    final long lastModified = currentFile.lastModified();
                    this.lastRotationTime = lastModified > 0
                            ? new DateTime(lastModified, DateTimeZone.UTC)
                            : DateTime.now(DateTimeZone.UTC);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create or open file", e);
                }
            }

            void write(final byte[] bytes) throws IOException {
                // newline delimited JSON with UTF-8 character encoding
                outputStream.write(bytes);
                outputStream.write(NEWLINE_UTF_8_BYTES);
                if (outputStream.byteBuffer().position() >= FILE_BUFFER_THRESHOLD) {
                    outputStream.byteBuffer().flip();
                    try {
                        // write buffer to file
                        positionInFile += fileChannel.write(outputStream.byteBuffer(), positionInFile);
                    } finally {
                        outputStream.clear();
                    }
                }
            }

            void flush() {
                if (outputStream.byteBuffer().position() != 0) {
                    // write buffer to file
                    outputStream.byteBuffer().flip();
                    try {
                        positionInFile += fileChannel.write(outputStream.byteBuffer(), positionInFile);
                    } catch (IOException e) {
                        logger.error("Failed to flush file buffer", e);
                    } finally {
                        outputStream.clear();
                    }
                }
            }

            @Override
            public long getBytesWritten() {
                return positionInFile;
            }

            @Override
            public DateTime getLastRotationTime() {
                return lastRotationTime;
            }

            @Override
            public void rotateIfNeeded() throws IOException {
                if (rotationEnabled && !rotationPolicies.isEmpty()) {
                    for (final RotationPolicy rotationPolicy : rotationPolicies) {
                        if (rotationPolicy.shouldRotateFile(this)) {
                            rotateNow();
                            break;
                        }
                    }
                }
                if (!retentionPolicies.isEmpty()) {
                    filesToDelete.clear();
                    for (final RetentionPolicy retentionPolicy : retentionPolicies) {
                        filesToDelete.addAll(retentionPolicy.deleteFiles(fileNamingPolicy));
                    }
                    if (!filesToDelete.isEmpty()) {
                        for (final File file : filesToDelete) {
                            if (!file.delete() && logger.isWarnEnabled()) {
                                logger.warn("Could not delete file {}", file.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            /**
             * Rotates the underlying JSON audit file.
             *
             * @throws IOException error rotating file
             */
            void rotateNow() throws IOException {
                // close and rename current file
                fileChannel.close();
                final Path archivedFilePath = fileNamingPolicy.getNextName().toPath();
                Files.move(filePath, archivedFilePath);
                // create new file
                fileChannel = FileChannel.open(filePath, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
                positionInFile = 0;
                lastRotationTime = DateTime.now(DateTimeZone.UTC);
            }

            @Override
            public void close() throws IOException {
                fileChannel.close();
            }

            @Override
            public void registerRotationHooks(RotationHooks rotationHooks) {
                // not implemented
            }
        }
    }

}
