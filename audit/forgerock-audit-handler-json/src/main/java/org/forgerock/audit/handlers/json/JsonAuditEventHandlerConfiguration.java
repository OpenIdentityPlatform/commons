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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.events.handlers.FileBasedEventHandlerConfiguration;

/**
 * Configuration for {@link JsonAuditEventHandler}.
 */
public class JsonAuditEventHandlerConfiguration extends FileBasedEventHandlerConfiguration {

    @JsonProperty(required = true)
    @JsonPropertyDescription("audit.handlers.json.logDirectory")
    private String logDirectory;

    @JsonPropertyDescription("audit.handlers.json.elasticsearchCompatible")
    private boolean elasticsearchCompatible;

    @JsonPropertyDescription("audit.handlers.json.buffering")
    private EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Gets the directory where the JSON file is located.
     *
     * @return location of the JSON file
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * Sets the directory where the JSON file is located.
     *
     * @param directory location of the JSON file
     */
    public void setLogDirectory(final String directory) {
        logDirectory = directory;
    }

    /**
     * Determines if JSON format should be transformed to be compatible with ElasticSearch format restrictions.
     *
     * @return {@code true} for ElasticSearch JSON format compatibility enforcement and {@code false} otherwise
     */
    public boolean isElasticsearchCompatible() {
        return elasticsearchCompatible;
    }

    /**
     * Specifies if JSON format should be transformed to be compatible with ElasticSearch format restrictions.
     *
     * @param elasticsearchCompatible {@code true} for ElasticSearch JSON format compatibility enforcements and
     * {@code false} otherwise
     */
    public void setElasticsearchCompatible(boolean elasticsearchCompatible) {
        this.elasticsearchCompatible = elasticsearchCompatible;
    }

    /**
     * Gets configuration of event buffering.
     *
     * @return configuration of event buffering
     */
    public EventBufferingConfiguration getBuffering() {
        return buffering;
    }

    /**
     * Sets configuration of event buffering.
     *
     * @param buffering configuration of event buffering
     */
    public void setBuffering(EventBufferingConfiguration buffering) {
        this.buffering = buffering;
    }

    @Override
    public boolean isUsableForQueries() {
        return true;
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.json.buffering.maxSize")
        private int maxSize;

        @JsonPropertyDescription("audit.handlers.json.buffering.writeInterval")
        private String writeInterval;

        /**
         * Gets the buffer capacity, which are the maximum number of events that can be buffered.
         *
         * @return buffer capacity
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * Sets the buffer capacity, which are the maximum number of events that can be buffered.
         *
         * @param maxSize buffer capacity
         */
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * Gets delay after which the file-writer thread is scheduled to run after encountering an empty event buffer
         * (units of 'ms' are recommended).
         *
         * @return Interval (e.g., "20 millis")
         */
        public String getWriteInterval() {
            return writeInterval;
        }

        /**
         * Sets delay after which the file-writer thread is scheduled to run after encountering an empty event buffer
         * (units of 'ms' are recommended).
         *
         * @param writeInterval Interval (e.g., "20 millis")
         */
        public void setWriteInterval(String writeInterval) {
            this.writeInterval = writeInterval;
        }
    }
}
