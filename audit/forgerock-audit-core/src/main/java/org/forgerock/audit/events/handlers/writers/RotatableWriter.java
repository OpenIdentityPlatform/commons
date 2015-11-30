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

package org.forgerock.audit.events.handlers.writers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;
import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRetention;
import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration.FileRotation;
import org.forgerock.audit.retention.DiskSpaceUsedRetentionPolicy;
import org.forgerock.audit.retention.FileNamingPolicy;
import org.forgerock.audit.retention.FreeDiskSpaceRetentionPolicy;
import org.forgerock.audit.retention.RetentionPolicy;
import org.forgerock.audit.retention.SizeBasedRetentionPolicy;
import org.forgerock.audit.retention.TimeStampFileNamingPolicy;
import org.forgerock.audit.rotation.FixedTimeRotationPolicy;
import org.forgerock.audit.rotation.RotatableObject;
import org.forgerock.audit.rotation.RotationContext;
import org.forgerock.audit.rotation.RotationHooks;
import org.forgerock.audit.rotation.RotationPolicy;
import org.forgerock.audit.rotation.SizeBasedRotationPolicy;
import org.forgerock.audit.rotation.TimeLimitRotationPolicy;
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
    private static final Duration ZERO = Duration.duration("zero");
    private static final Duration FIVE_SECONDS = Duration.duration("5s");

    private final List<RotationPolicy> rotationPolicies = new LinkedList<>();
    private final List<RetentionPolicy> retentionPolicies = new LinkedList<>();
    private final FileNamingPolicy fileNamingPolicy;
    private ScheduledExecutorService rotator;
    private DateTime lastRotationTime;
    private final boolean rotationEnabled;
    private final File file;
    private RotationHooks rotationHooks = new RotationHooks.NoOpRotatationHooks();
    private final AtomicBoolean isRotating = new AtomicBoolean(false);
    /** The underlying output stream. */
    private MeteredStream meteredStream;
    /** The underlying buffered writer using the output stream. */
    private BufferedWriter writer;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Constructs a {@link RotatableWriter} given an initial file to manage rotation/retention, and
     * a {@link FileBasedEventHandlerConfiguration}
     * @param file The initial file to manage rotation/retention.
     * @param configuration The configuration of the rotation and retention policies.
     * @param append Whether to append to the rotatable file or not.
     */
    public RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
            final boolean append) throws IOException {
        this(file, configuration, append,
                new TimeStampFileNamingPolicy(
                        file,
                        configuration.getFileRotation().getRotationFileSuffix(),
                        configuration.getFileRotation().getRotationFilePrefix()));
    }

    /**
     * This constructor allows tests to set an alternative FileNamingPolicy as TimeStampFileNamingPolicy lists files
     * for deletion by their last modified timestamps but these timestamps are only accurate to the nearest second.
     */
    @VisibleForTesting
    public RotatableWriter(final File file, final FileBasedEventHandlerConfiguration configuration,
                           final boolean append, final FileNamingPolicy fileNamingPolicy) throws IOException {
        this.file = file;
        this.fileNamingPolicy = fileNamingPolicy;
        this.rotationEnabled = configuration.getFileRotation().isRotationEnabled();
        this.lastRotationTime = new DateTime(file.lastModified(), DateTimeZone.UTC);
        this.writer = constructWriter(file, append);
        addRetentionPolicies(configuration.getFileRetention());
        addRotationPolicies(configuration.getFileRotation());
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
            if (currentFile.renameTo(newFile)) {
                rotationHappened = true;
                if (currentFile.createNewFile()) {
                    writer = constructWriter(currentFile, true);
                    context.setWriter(writer);
                    rotationHooks.postRotationAction(context);
                } else {
                    logger.error("Unable to resume writing to audit file {}; further events will not be logged",
                            currentFile.toString());
                }
            } else {
                logger.error("Unable to rename the audit file {}; further events will continue to be logged to " +
                        "the current file", currentFile.toString());
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
        readWriteLock.writeLock().lock();
        try {
            isRotating.set(true);
            return rotate();
        }
        finally {
            isRotating.set(false);
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    private BufferedWriter constructWriter(File csvFile, boolean append)
            throws IOException {
        FileOutputStream stream = new FileOutputStream(csvFile, append);
        meteredStream = new MeteredStream(stream, file.length());
        OutputStreamWriter osw = new OutputStreamWriter(meteredStream, StandardCharsets.UTF_8);
        return new BufferedWriter(osw);
    }

    private void addRotationPolicies(final FileRotation fileRotation) {
        final Duration rotationInterval = parseDuration("rotation interval", fileRotation.getRotationInterval(), ZERO);
        final List<Duration> dailyRotationTimes = new LinkedList<>();
        for (final String rotationTime : fileRotation.getRotationTimes()) {
            Duration duration = parseDuration("rotation time", rotationTime, null);
            if (duration != null && !duration.isUnlimited()) {
                dailyRotationTimes.add(duration);
            }
        }

        // add SizeBasedRotationPolicy if a non zero size is supplied
        final long maxFileSize = fileRotation.getMaxFileSize();
        if (maxFileSize > 0) {
            rotationPolicies.add(new SizeBasedRotationPolicy(maxFileSize));
        }

        // add FixedTimeRotationPolicy
        if (!dailyRotationTimes.isEmpty()) {
            rotationPolicies.add(new FixedTimeRotationPolicy(dailyRotationTimes));
        }

        // add TimeLimitRotationPolicy if enabled
        if (!(rotationInterval.isZero() || rotationInterval.isUnlimited())) {
            rotationPolicies.add(new TimeLimitRotationPolicy(rotationInterval));
        }
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
                        }
                    },
                    0,
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

    private void addRetentionPolicies(final FileRetention fileRetention) {
        // Add SizeBasedRetentionPolicy if the max number of files config value is more than 0
        final int maxNumberOfHistoryFiles = fileRetention.getMaxNumberOfHistoryFiles();
        if (maxNumberOfHistoryFiles > 0) {
            retentionPolicies.add(new SizeBasedRetentionPolicy(maxNumberOfHistoryFiles));
        }

        // Add DiskSpaceUsedRetentionPolicy if config value > 0
        final long maxDiskSpaceToUse = fileRetention.getMaxDiskSpaceToUse();
        if (maxDiskSpaceToUse > 0) {
            retentionPolicies.add(new DiskSpaceUsedRetentionPolicy(maxDiskSpaceToUse));
        }

        // Add FreeDiskSpaceRetentionPolicy if config value > 0
        final long minimumFreeDiskSpace = fileRetention.getMinFreeSpaceRequired();
        if (minimumFreeDiskSpace > 0) {
            retentionPolicies.add(new FreeDiskSpaceRetentionPolicy(minimumFreeDiskSpace));
        }
    }

}
