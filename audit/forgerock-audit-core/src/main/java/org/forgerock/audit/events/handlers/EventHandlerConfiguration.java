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
    @JsonPropertyDescription("audit.handlers.all.buffering")
    protected EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Returns the configuration for events buffering.
     *
     * @return the configuration
     */
    public EventBufferingConfiguration getBuffering() {
        return buffering;
    }

    /**
     * Sets the configuration for events buffering.
     *
     * @param bufferingConfiguration
     *            The configuration
     */
    public void setBufferingConfiguration(EventBufferingConfiguration bufferingConfiguration) {
        this.buffering = bufferingConfiguration;
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.all.buffering.enabled")
        private boolean enabled;

        @JsonPropertyDescription("audit.handlers.all.buffering.forceFlushBeforeRead")
        private boolean forceFlushBeforeRead;

        @JsonPropertyDescription("audit.handlers.all.buffering.maxTime")
        private long maxTime;

        @JsonPropertyDescription("audit.handlers.all.buffering.maxSize")
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
