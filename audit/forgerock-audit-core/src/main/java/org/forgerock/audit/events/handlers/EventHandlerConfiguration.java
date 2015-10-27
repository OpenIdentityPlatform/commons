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

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.util.Reject;

/**
 * Base class for audit event handler configuration.
 */
public abstract class EventHandlerConfiguration {

    /** Whether or not this handler is enabled. */
    @JsonPropertyDescription("audit.handlers.all.enabled")
    private boolean enabled = true;

    /** Name of this audit event handler. */
    @JsonPropertyDescription("audit.handlers.all.name")
    private String name;

    /** The set of topics that this audit event handler accepts. */
    @JsonPropertyDescription("audit.handlers.all.topics")
    private Set<String> topics;

    /** Event buffering is disabled by default. */
    @JsonPropertyDescription("audit.handlers.all.buffering")
    protected EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Checks if the audit event handler is enabled.
     * @return
     *      True - If the audit event handler is enabled.
     *      False - If the audit event handler is disabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled flag for an audit event handler.
     * @param enabled
     *      True - Enable the audit event handler.
     *      False - Disable the audit event handler.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the name of this handler.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this handler.
     *
     * @param name
     *          The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the names of the topics accepted by this handler.
     *
     * @return the set of topic names
     */
    public Set<String> getTopics() {
        return topics;
    }

    /**
     * Sets the topics accepted by this handler.
     *
     * @param topics
     *          The names of all accepted topics
     */
    public void setTopics(Set<String> topics) {
        this.topics = topics;
    }

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

        @JsonPropertyDescription("audit.handlers.all.buffering.autoFlush")
        private boolean autoFlush = true;

        @JsonPropertyDescription("audit.handlers.all.buffering.maxSize")
        private int maxSize = 5000;

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
         * Indicates if events are automatically flushed after being written.
         *
         * @return {@code true} if events must be flushed
         */
        public boolean isAutoFlush() {
            return autoFlush;
        }

        /**
         * Sets the auto flush indicator.
         *
         * @param auto
         *            Indicates if events are automatically flushed after being written.
         */
        public void setAutoFlush(boolean auto) {
            this.autoFlush = auto;
        }

        /**
         * Returns the maximum size of the queue.
         *
         * @return maxSize Maximum number of events in the queue.
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * Sets the maximum size of the events queue.
         *
         * @param maxSize
         *            Maximum number of events in the queue.
         */
        public void setMaxSize(int maxSize) {
            Reject.ifFalse(maxSize >= 1);
            this.maxSize = maxSize;
        }
    }
}
