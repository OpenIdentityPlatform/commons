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

import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Base class for audit event handler configuration.
 */
public abstract class EventHandlerConfiguration {

    /** Event buffering is disabled by default. */
    @JsonPropertyDescription("The configuration for optional buffering of events.")
    protected EventBufferingConfiguration bufferingConfig = new EventBufferingConfiguration();

    /**
     * Returns the configuration for events buffering.
     *
     * @return the configuration
     */
    public EventBufferingConfiguration getBufferingConfig() {
        return bufferingConfig;
    }

    /**
     * Sets the configuration for events buffering.
     *
     * @param bufferingConfiguration
     *            The configuration
     */
    public void setBufferingConfiguration(EventBufferingConfiguration bufferingConfiguration) {
        this.bufferingConfig = bufferingConfiguration;
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("Indicates if buffering of events is enabled.")
        private boolean enabled;

        @JsonPropertyDescription("Indicates if buffer must be flushed before a read operation is done, " +
        "in order to ensure the latest events are available.")
        private boolean forceFlushBeforeRead;

        @JsonPropertyDescription("The maximum time before the buffer is automatically flushed.")
        private long maxTime;

        @JsonPropertyDescription("The maximum size of buffer before the buffer is automatically flushed.")
        private int maxSize = 1;

        /**
         * Indicates if event buffering is enabled.
         *
         * @return {@code true} if buffering is enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the buffering status.
         *
         * @param enabled
         *            Indicates if buffering is enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Indicates if a buffer must be flushed before reading the events.
         *
         * @return {@code true} if buffer must be flushed
         */
        public boolean isForceFlushBeforeRead() {
            return forceFlushBeforeRead;
        }

        /**
         * Sets the force flush indicator.
         *
         * @param forceFlush
         *            Indicates if a buffer must be flushed before reading the events.
         */
        public void setForceFlushBeforeRead(boolean forceFlush) {
            this.forceFlushBeforeRead = forceFlush;
        }

        /**
         * Returns the maximum time to wait before flushing the buffer.
         *
         * @return maxTime Maximum time in milliseconds.
         */
        public long getMaxTime() {
            return maxTime;
        }

        /**
         * Sets the maximum time to wait before flushing the buffer.
         *
         * @param maxTime
         *            Maximum time in milliseconds.
         */
        public void setMaxTime(long maxTime) {
            this.maxTime = maxTime;
        }

        /**
         * Returns the maximum size allowed before flushing the buffer.
         *
         * @return maxSize Maximum number of events.
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * Sets the maximum size allowed before flushing the buffer.
         *
         * @param maxSize
         *            Maximum number of events.
         */
        public void setMaxSize(int maxSize) {
            Reject.ifFalse(maxSize >= 1);
            this.maxSize = maxSize;
        }
    }
}
