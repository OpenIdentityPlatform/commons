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

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.forgerock.util.Reject;
import org.forgerock.util.time.Duration;

/**
 * Configures time based or size based log file rotation.
 */
public class FileBasedEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("audit.handlers.file.fileRotation")
    private FileRotation fileRotation = new FileRotation();
    @JsonPropertyDescription("audit.handlers.file.fileRetention")
    private FileRetention fileRetention = new FileRetention();
    @JsonPropertyDescription("audit.handlers.file.rotationRetentionCheckInterval")
    private String rotationRetentionCheckInterval = "5s";

    /**
     * Gets the {@link FileRotation}.
     * @return Not-null, The {@link FileRotation}.
     */
    public FileRotation getFileRotation() {
        return fileRotation;
    }

    /**
     * Sets the {@link FileRotation}.
     *
     * @param fileRotation Not-null, The {@link FileRotation}.
     */
    public void setFileRotation(final FileRotation fileRotation) {
        Reject.ifNull(fileRotation);
        this.fileRotation = fileRotation;
    }

    /**
     * Gets the {@link FileRetention}.
     * @return Not-null, The {@link FileRetention}.
     */
    public FileRetention getFileRetention() {
        return fileRetention;
    }

    /**
     * Sets the {@link FileRetention}.
     *
     * @param fileRetention Not-null, The {@link FileRetention}.
     */
    public void setFileRetention(final FileRetention fileRetention) {
        Reject.ifNull(fileRetention);
        this.fileRetention = fileRetention;
    }

    /**
     * Gets the interval to check time-based file rotation policies. The interval should be set as a {@link Duration}.
     * <p/>
     * Examples of valid durations are:
     * <pre>
     *      5 seconds
     *      5 minutes
     *      5 hours
     * </pre>
     * <p/>
     * Value of "zero" or "disabled" are not acceptable.
     *
     * @return The interval duration.
     */
    public String getRotationRetentionCheckInterval() {
        return rotationRetentionCheckInterval;
    }

    /**
     * Sets the interval to check time-based file rotation policies. The interval should be set as a {@link Duration}.
     * <p/>
     * Examples of valid durations are:
     * <pre>
     *      5 seconds
     *      5 minutes
     *      5 hours
     * </pre>
     * <p/>
     * Value of "zero" or "disabled" are not acceptable.
     *
     * @param rotationRetentionCheckInterval The interval duration.
     */
    public void setRotationRetentionCheckInterval(String rotationRetentionCheckInterval) {
        this.rotationRetentionCheckInterval = rotationRetentionCheckInterval;
    }

    /**
     * Groups the file rotation config parameters.
     */
    public static class FileRotation {

        public static final long NO_MAX_FILE_SIZE = -1;
        public static final String DEFAULT_ROTATION_FILE_SUFFIX = "-yyyy.MM.dd-HH.mm.ss";

        @JsonPropertyDescription("audit.handlers.file.rotationEnabled")
        private boolean rotationEnabled = false;

        // size based rotation config parameters
        @JsonPropertyDescription("audit.handlers.file.maxFileSize")
        private long maxFileSize = NO_MAX_FILE_SIZE;

        // time Based Rotation config parameters
        @JsonPropertyDescription("audit.handlers.file.rotationFilePrefix")
        private String rotationFilePrefix = null;

        // fixed time based rotation config parameters
        @JsonPropertyDescription("audit.handlers.file.rotationTimes")
        private final List<String> rotationTimes = new LinkedList<>();

        @JsonPropertyDescription("audit.handlers.file.rotationFileSuffix")
        private String rotationFileSuffix = DEFAULT_ROTATION_FILE_SUFFIX;

        @JsonPropertyDescription("audit.handlers.file.rotationInterval")
        private String rotationInterval = "disabled";

        /**
         * Gets log rotation enabled state. By default log rotation is disabled.
         * @return True - If log rotation is enabled.
         *         False - If log rotation is disabled.
         */
        public boolean isRotationEnabled() {
            return rotationEnabled;
        }

        /**
         * Sets log rotation enabled state. By default log rotation is disabled.
         * @param rotationEnabled True - Enabled log rotation.
         *                        False - Disables log rotation.
         */
        public void setRotationEnabled(boolean rotationEnabled) {
            this.rotationEnabled = rotationEnabled;
        }

        /**
         * Gets the maximum file size of an audit log file in bytes.
         * @return The maximum file size in bytes.
         */
        public long getMaxFileSize() {
            return maxFileSize;
        }

        /**
         * Sets the maximum file size of an audit log file in bytes.
         * @param maxFileSize The maximum file size in bytes.
         */
        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        /**
         * Gets the prefix to add to a log file on rotation. This is only used when time based rotation is enabled.
         * @return The prefix to add to the file.
         */
        public String getRotationFilePrefix() {
            return rotationFilePrefix;
        }

        /**
         * Sets the prefix to add to a log file on rotation. This is only used when time based rotation is enabled.
         * @param rotationFilePrefix The prefix to add to the file.
         */
        public void setRotationFilePrefix(String rotationFilePrefix) {
            this.rotationFilePrefix = rotationFilePrefix;
        }

        /**
         * Gets the suffix to add to a log file on rotation. This is only used when time based rotation is enabled.
         * The suffix allows use of Date and Time patterns defined in {@link SimpleDateFormat}. The default suffix is
         * "-yyyy.MM.dd-HH.mm.ss".
         * @return The suffix to add to the file.
         */
        public String getRotationFileSuffix() {
            return rotationFileSuffix;
        }

        /**
         * Sets the suffix to add to a log file on rotation. This is only used when time based rotation is enabled.
         * The suffix allows use of Date and Time patterns defined in {@link SimpleDateFormat}. The default suffix is
         * "-yyyy.MM.dd-HH.mm.ss".
         * @param rotationFileSuffix The suffix to add to the file.
         */
        public void setRotationFileSuffix(String rotationFileSuffix) {
            this.rotationFileSuffix = rotationFileSuffix;
        }

        /**
         * Gets the interval to trigger a file rotation. The interval should be set as a {@link Duration}.
         * <p/>
         * Examples of valid durations are:
         * <pre>
         *      5 seconds
         *      5 minutes
         *      5 hours
         *      disabled
         * </pre>
         * <p/>
         * A value of "zero" or "disabled" means that time based file rotation is disabled.
         *
         * @return The interval duration.
         */
        public String getRotationInterval() {
            return rotationInterval;
        }

        /**
         * Sets the interval to trigger a file rotation. The interval should be set as a {@link Duration}.
         * <p/>
         * Examples of valid durations are:
         * <pre>
         *      5 seconds
         *      5 minutes
         *      5 hours
         *      disabled
         * </pre>
         * <p/>
         * A value of "zero" or "disabled" disables time based file rotation.
         *
         * @param rotationInterval A String that can be parsed as a {@link Duration}, specifying rotation interval.
         */
        public void setRotationInterval(String rotationInterval) {
            this.rotationInterval = rotationInterval;
        }

        /**
         * Gets a list of times at which file rotation should be triggered; times should be provided as Strings that can
         * be parsed by {@link Duration} that each specify an offset from midnight.
         * <p/>
         * For example the list of [10 milliseconds, 20 milliseconds, 30 milliseconds] will
         * cause a file rotation to happen 10 milliseconds, 20 milliseconds and 30 milliseconds after midnight.
         *
         * @return The list of durations after midnight that rotation should happen.
         */
        public List<String> getRotationTimes() {
            return rotationTimes;
        }

        /**
         * Sets a list of times at which file rotation should be triggered; times should be provided as Strings that can
         * be parsed by {@link Duration} that each specify an offset from midnight.
         * <p/>
         * For example the list of [10 milliseconds, 20 milliseconds, 30 milliseconds] will
         * cause a file rotation to happen 10 milliseconds, 20 milliseconds and 30 milliseconds after midnight.
         *
         * @param rotationTimes The list of durations after midnight that rotation should happen.
         */
        public void setRotationTimes(List<String> rotationTimes) {
            this.rotationTimes.addAll(rotationTimes);
        }
    }

    /**
     * Groups the file retention config parameters.
     */
    public static class FileRetention {

        public static final int UNLIMITED_HISTORY_FILES = -1;
        public static final long ANY_DISK_SPACE = -1;

        @JsonPropertyDescription("audit.handlers.file.maxNumberOfHistoryFiles")
        private int maxNumberOfHistoryFiles = UNLIMITED_HISTORY_FILES;

        @JsonPropertyDescription("audit.handlers.file.maxDiskSpaceToUse")
        private long maxDiskSpaceToUse = ANY_DISK_SPACE;

        @JsonPropertyDescription("audit.handlers.file.minFreeSpaceRequired")
        private long minFreeSpaceRequired = ANY_DISK_SPACE;

        /**
         * Gets the maximum number of historical log files to retain. -1 disables pruning of old history files.
         * @return The maximum number of log files. -1 disables pruning of old history files.
         */
        public int getMaxNumberOfHistoryFiles() {
            return maxNumberOfHistoryFiles;
        }

        /**
         * Sets the maximum number of historical log files to retain. -1 disables pruning of old history files.
         * @return The maximum number of log files. -1 disables pruning of old history files.
         */
        public void setMaxNumberOfHistoryFiles(int maxNumberOfHistoryFiles) {
            this.maxNumberOfHistoryFiles = maxNumberOfHistoryFiles;
        }

        /**
         * Gets the maximum disk space the audit logs can occupy. A negative or zero value indicates this
         * policy is disabled.
         * @return The maximum disk space the audit logs can occupy.
         */
        public long getMaxDiskSpaceToUse() {
            return maxDiskSpaceToUse;
        }

        /**
         * Sets the maximum disk space the audit logs can occupy. A negative or zero value indicates this
         * policy is disabled.
         * @param maxDiskSpaceToUse The maximum disk space the audit logs can occupy.
         */
        public void setMaxDiskSpaceToUse(final long maxDiskSpaceToUse) {
            this.maxDiskSpaceToUse = maxDiskSpaceToUse;
        }

        /**
         * Gets the minimum free space the system must contain. A negative or zero value indicates this
         * policy is disabled.
         * @return The minimum free space the system must contain.
         */
        public long getMinFreeSpaceRequired() {
            return minFreeSpaceRequired;
        }

        /**
         * Sets the minimum free space the system must contain. A negative or zero value indicates this
         * policy is disabled.
         * @param minFreeSpaceRequired The minimum free space the system must contain.
         */
        public void setMinFreeSpaceRequired(final long minFreeSpaceRequired) {
            this.minFreeSpaceRequired = minFreeSpaceRequired;
        }
    }
}
