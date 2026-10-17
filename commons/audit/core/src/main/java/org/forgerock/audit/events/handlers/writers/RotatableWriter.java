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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.audit.events.handlers.writers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.RetentionPolicy;
import org.forgerock.audit.rotation.RotatableObject;
import org.forgerock.audit.rotation.RotationContext;
import org.forgerock.audit.rotation.RotationHooks;
import org.forgerock.audit.rotation.RotationPolicy;
import org.forgerock.audit.util.GzipUtil;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.time.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an {@link RotatableWriter} that supports file rotation and retention.
 */
public class RotatableWriter implements TextWriter, RotatableObject {

    private static final Logger logger = LoggerFactory.getLogger(RotatableWriter.class);
    private static final Duration FIVE_SECONDS = Duration.duration("5s");

    private final List<RotationPolicy> rotationPolicies;
    private final List<RetentionPolicy> retentionPolicies;
    private final FileNamingPolicy fileNamingPolicy;
    private ScheduledExecutorService rotator;
    private DateTime lastRotationTime;
    private final boolean rotationEnabled;
    private final File file;
    private RotationHooks rotationHooks = new RotationHooks.NoOpRotatationHooks();
    private final AtomicBoolean isRotating = new AtomicBoolean(false);
    /** The underlying output stream. */
    private MeteredStream meteredStream;
    /**
     * The underlying buffered writer using the output stream. After a failure, it is not replaced until this writer
     * is closed: its next writes and flushes fail too, also for the texts that the failure dropped.
     */
    private FailFastWriter writer;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final RolloverLifecycleHook rolloverLifecycleHook;
    private final FileStreamOpener fileStreamOpener;

    /**
     * Constructs a {@link RotatableWriter} given an initial file to manage rotation/retention, and
     * a {@link FileBasedEventHandlerConfiguration}.
     * @param file The initial file to manage rotation/retention.
     * @param configuration The configuration of the rotation and retention policies.
     * @param append Whether to append to the rotatable file or not.
     * @throws IOException If a problem occurs.
     */
    public RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append) throws IOException {
        this(file, configuration, append, configuration.getFileRotation().buildTimeStampFileNamingPolicy(file));
    }

    /**
     * Constructs a {@link RotatableWriter} given an initial file to manage rotation/retention, a
     * a {@link FileBasedEventHandlerConfiguration} and a {@link RolloverLifecycleHook}.
     *
     * @param file The initial file to manage rotation/retention.
     * @param configuration The configuration of the rotation and retention policies.
     * @param append Whether to append to the rotatable file or not.
     * @param rolloverLifecycleHook Hook to use before and after rotation/retention checks.
     * @throws IOException If a problem occurs.
     */
    public RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append, final RolloverLifecycleHook rolloverLifecycleHook) throws IOException {
        this(file, configuration, append, configuration.getFileRotation().buildTimeStampFileNamingPolicy(file),
                rolloverLifecycleHook);
    }

    /**
     * This constructor allows tests to set an alternative FileNamingPolicy as TimeStampFileNamingPolicy lists files
     * for deletion by their last modified timestamps but these timestamps are only accurate to the nearest second.
     */
    @VisibleForTesting
    RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
                           final boolean append, final FileNamingPolicy fileNamingPolicy) throws IOException {
        this(file, configuration, append, fileNamingPolicy, NOOP_ROLLOVER_LIFECYCLE_HOOK, FileOutputStream::new);
    }

    /** This constructor allows tests to open streams that fail, as on a full disk. */
    @VisibleForTesting
    RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append, final FileStreamOpener fileStreamOpener) throws IOException {
        this(file, configuration, append, configuration.getFileRotation().buildTimeStampFileNamingPolicy(file),
                NOOP_ROLLOVER_LIFECYCLE_HOOK, fileStreamOpener);
    }

    private RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append, final FileNamingPolicy fileNamingPolicy,
            final RolloverLifecycleHook rolloverLifecycleHook) throws IOException {
        this(file, configuration, append, fileNamingPolicy, rolloverLifecycleHook, FileOutputStream::new);
    }

    /** Constructor with all possible parameters. */
    private RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append, final FileNamingPolicy fileNamingPolicy,
            final RolloverLifecycleHook rolloverLifecycleHook, final FileStreamOpener fileStreamOpener)
            throws IOException {
        this.file = file;
        this.fileNamingPolicy = fileNamingPolicy;
        this.fileStreamOpener = fileStreamOpener;
        this.rotationEnabled = configuration.getFileRotation().isRotationEnabled();
        final long lastModified = file.lastModified();
        this.lastRotationTime = lastModified > 0
                ? new DateTime(file.lastModified(), DateTimeZone.UTC)
                : DateTime.now(DateTimeZone.UTC);
        this.rolloverLifecycleHook = rolloverLifecycleHook;
        this.writer = constructWriter(file, append);
        retentionPolicies = configuration.getFileRetention().buildRetentionPolicies();
        rotationPolicies = configuration.getFileRotation().buildRotationPolicies();
        scheduleRotationAndRetentionChecks(configuration);
    }

    /**
     * Rotate the log file if any of the configured rotation policies determine that rotation is required.
     *
     * @throws IOException If unable to rotate the log file.
     */
    @Override
    public void rotateIfNeeded() throws IOException {
        if (!rotationEnabled || isRotating.get()) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            for (RotationPolicy rotationPolicy : rotationPolicies) {
                if (rotationPolicy.shouldRotateFile(this)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Must rotate: {}", file.getAbsolutePath());
                    }
                    isRotating.set(true);
                    if (rotate()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Finished rotation for: {}", file.getAbsolutePath());
                        }
                    }
                    break;
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
            isRotating.set(false);
        }
    }

    /** Delete files if they need to be deleted as per enabled retention policies. */
    private void deleteFilesIfNeeded() throws IOException {
        readWriteLock.writeLock().lock();
        try {
            Set<File> filesToDelete = checkRetention(); // return the files to delete, but do not delete them
            if (!filesToDelete.isEmpty()) {
                deleteFiles(filesToDelete);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private boolean rotate() throws IOException {
        final IOException failure = writer.getFailure();
        if (failure != null) {
            // A new writer would succeed the flush of a text that the failure dropped, and closing this one would write
            // a part of a failed text into the archive.
            throw new IOException("The file is not rotated: a previous write to it failed", failure);
        }
        boolean rotationHappened = false;
        RotationContext context = new RotationContext();
        context.setWriter(writer);
        File currentFile = fileNamingPolicy.getInitialName();
        context.setInitialFile(currentFile);
        if (currentFile.exists()) {
            File newFile = fileNamingPolicy.getNextName();
            context.setNextFile(newFile);
            rotationHooks.preRotationAction(context);
            writer.close();
            if (logger.isTraceEnabled()) {
                logger.trace("Renaming {} to {}", currentFile.getAbsolutePath(), newFile.getAbsolutePath());
            }
            // a file to compress is renamed without the .gz suffix, and compressed into newFile below
            if (currentFile.renameTo(GzipUtil.removeGzipSuffix(newFile))) {
                rotationHappened = true;
                if (currentFile.createNewFile()) {
                    writer = constructWriter(currentFile, true);
                    context.setWriter(writer);
                    rotationHooks.postRotationAction(context);
                    GzipUtil.compressRotatedFile(newFile);
                } else {
                    logger.error("Unable to resume writing to audit file {}; further events will not be logged",
                            currentFile.toString());
                }
            } else {
                logger.error("Unable to rename the audit file {}; further events will continue to be logged to "
                        + "the current file", currentFile.toString());
                writer = constructWriter(currentFile, true);
            }
            lastRotationTime = DateTime.now(DateTimeZone.UTC);
        }
        return rotationHappened;
    }

    private Set<File> checkRetention() throws IOException {
        Set<File> filesToDelete = new HashSet<>();
        for (RetentionPolicy retentionPolicy : retentionPolicies) {
            filesToDelete.addAll(retentionPolicy.deleteFiles(fileNamingPolicy));
        }
        return filesToDelete;
    }

    private void deleteFiles(final Set<File> files) {
        for (final File file : files) {
            if (logger.isInfoEnabled()) {
                logger.info("Deleting file {}", file.getAbsolutePath());
            }
            if (!file.delete()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not delete file {}", file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBytesWritten() {
        logger.trace("bytes written={}", meteredStream.getBytesWritten());
        return meteredStream.getBytesWritten();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getLastRotationTime() {
        return lastRotationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (rotator != null) {
            boolean interrupted = false;
            rotator.shutdown();
            try {
                while (!rotator.awaitTermination(500, MILLISECONDS)) {
                    logger.debug("Waiting to terminate the rotator thread.");
                }
            } catch (InterruptedException ex) {
                logger.error("Unable to terminate the rotator thread", ex);
                interrupted = true;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        writer.close();
    }

    @Override
    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            logger.error("Error when performing shutdown", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerRotationHooks(final RotationHooks rotationHooks) {
        this.rotationHooks = rotationHooks;
    }

    @Override
    public void write(String str) throws IOException {
        ReadLock lock = readWriteLock.readLock();
        try {
            lock.lock();
            logger.trace("Actually writing to file: {}", str);
            writer.write(str);
        } finally {
            lock.unlock();
        }
        rotateIfNeeded();
    }

    /**
     * Forces a rotation of the writer.
     *
     * @return {@code true} if rotation was done, {@code false} otherwise.
     * @throws IOException
     *          If an error occurs
     */
    public boolean forceRotation() throws IOException {
        // As the scheduled checks do, run the hook before taking the write lock: the hook may take a lock that the
        // rotation hooks need, and that writers hold while they wait for the read lock.
        rolloverLifecycleHook.beforeRollingOver();
        try {
            return rotateUnderWriteLock();
        } finally {
            rolloverLifecycleHook.afterRollingOver();
        }
    }

    /** Rotates the file under the write lock, whatever the rotation policies say. */
    private boolean rotateUnderWriteLock() throws IOException {
        readWriteLock.writeLock().lock();
        try {
            isRotating.set(true);
            return rotate();
        } finally {
            isRotating.set(false);
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        // The rotation closes and replaces the writer under the write lock.
        final ReadLock lock = readWriteLock.readLock();
        lock.lock();
        try {
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    private FailFastWriter constructWriter(File csvFile, boolean append)
            throws IOException {
        OutputStream stream = fileStreamOpener.open(csvFile, append);
        meteredStream = new MeteredStream(stream, file.length());
        return new FailFastWriter(meteredStream, StandardCharsets.UTF_8);
    }

    /**
     * Schedule checks for rotations and retention policies.
     * <p>
     * The check interval is provided by the RotationRetentionCheckInterval property, which must have
     * a non-zero value if at least one policy is enabled.
     */
    private void scheduleRotationAndRetentionChecks(FileBasedEventHandlerConfiguration configuration)
            throws IOException {
        final Duration rotationCheckInterval = parseDuration("rotation and retention check interval",
                configuration.getRotationRetentionCheckInterval(), FIVE_SECONDS);

        if (!rotationPolicies.isEmpty() || !retentionPolicies.isEmpty()) {
            if (rotationCheckInterval.isUnlimited() || rotationCheckInterval.isZero()) {
                throw new IOException("Rotation and retention check interval set to an invalid value: "
                        + rotationCheckInterval);
            }
            rotator = Executors.newScheduledThreadPool(1);
            rotator.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            rolloverLifecycleHook.beforeRollingOver();
                            try {
                                try {
                                    rotateIfNeeded();
                                } catch (Exception e) {
                                    logger.error("Failure when applying a rotation policy to file {}",
                                            fileNamingPolicy.getInitialName(), e);
                                }
                                try {
                                    deleteFilesIfNeeded();
                                } catch (Exception e) {
                                    logger.error("Failure when applying a retention policy to file {}",
                                            fileNamingPolicy.getInitialName(), e);
                                }
                            } finally {
                                rolloverLifecycleHook.afterRollingOver();
                            }
                        }
                    },
                    rotationCheckInterval.to(TimeUnit.MILLISECONDS),
                    rotationCheckInterval.to(TimeUnit.MILLISECONDS),
                    TimeUnit.MILLISECONDS);
        }
    }

    private Duration parseDuration(String description, String duration, Duration defaultValue) {
        try {
            return Duration.duration(duration);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid {} value: '{}'", description, duration);
            return defaultValue;
        }
    }

    @VisibleForTesting
    List<RotationPolicy> getRotationPolicies() {
        return rotationPolicies;
    }

    /**
     * A RotationRetentionCheckHook that does nothing.
     */
    public static final RolloverLifecycleHook NOOP_ROLLOVER_LIFECYCLE_HOOK =
        new RolloverLifecycleHook() {
            @Override
            public void beforeRollingOver() {
                // nothing to do
            }

            @Override
            public void afterRollingOver() {
                // nothing to do
            }
    };

    /**
     * Callback hooks to allow custom action to be taken before and after the checks for rotation and
     * retention is performed.
     */
    public interface RolloverLifecycleHook {

        /**
         * This method is called before the rotation and retention checks are done, and before a forced rotation.
         */
        void beforeRollingOver();

        /**
         * This method is called after the rotation and retention checks are done, and after a forced rotation.
         */
        void afterRollingOver();
    }

    /** Opens the output stream of the files that the writer writes to. */
    @VisibleForTesting
    interface FileStreamOpener {
        /**
         * Opens the output stream of a file.
         *
         * @param file The file to write to.
         * @param append Whether to append to the file or not.
         * @return The output stream of the file.
         * @throws IOException If the file cannot be opened.
         */
        OutputStream open(File file, boolean append) throws IOException;
    }

}
