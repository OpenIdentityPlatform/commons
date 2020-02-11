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
package org.forgerock.audit.handlers.splunk;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Configuration for the splunk audit event handler.
 */
public final class SplunkAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("audit.handlers.splunk.connection")
    private ConnectionConfiguration connection = new ConnectionConfiguration();

    @JsonPropertyDescription("audit.handlers.splunk.buffering")
    private BufferingConfiguration buffering = new BufferingConfiguration();

    @JsonPropertyDescription("audit.handlers.splunk.authzToken")
    private String authzToken;

    /**
     * Gets the configuration for buffering.
     *
     * @return the buffering configuration
     */
    public BufferingConfiguration getBuffering() {
        return buffering;
    }

    /**
     * Sets the configuration for buffering.
     *
     * @param buffering
     *         the buffering configuration
     */
    public void setBuffering(final BufferingConfiguration buffering) {
        this.buffering = buffering;
    }

    /**
     * Gets configuration of connection to Splunk.
     *
     * @return configuration of connection to Splunk
     */
    public ConnectionConfiguration getConnection() {
        return connection;
    }

    /**
     * Sets configuration of connection to Splunk.
     *
     * @param connection
     *         configuration of connection to Splunk
     */
    public void setConnection(final ConnectionConfiguration connection) {
        this.connection = connection;
    }

    /**
     * Gets the Splunk authorization token required for making HTTP event collector calls.
     *
     * @return the Splunk authorization token
     */
    public String getAuthzToken() {
        return authzToken;
    }

    /**
     * Sets the Splunk authorization token required for making HTTP event collector calls.
     *
     * @param authzToken
     *         the Splunk authorization token
     */
    public void setAuthzToken(final String authzToken) {
        this.authzToken = authzToken;
    }

    @Override
    public boolean isUsableForQueries() {
        return false;
    }

    /**
     * Configuration of connection to Splunk.
     */
    public final static class ConnectionConfiguration {

        // Splunk's default host in a development environment.
        private static final String DEFAULT_HOST = "localhost";

        // Splunk's default HTTP event collector port in a development environment.
        private static final int DEFAULT_PORT = 8088;

        @JsonPropertyDescription("audit.handlers.splunk.connection.useSSL")
        private boolean useSSL;

        @JsonPropertyDescription("audit.handlers.splunk.connection.host")
        private String host;

        @JsonPropertyDescription("audit.handlers.splunk.connection.port")
        private int port;

        /**
         * Indicates if the connection uses SSL.
         *
         * @return {@code true} when the connection uses SSL.
         */
        public boolean isUseSSL() {
            return useSSL;
        }

        /**
         * Sets the use of a SSL connection.
         *
         * @param useSSL
         *         {@code true} when the connection uses SSL.
         */
        public void setUseSSL(final boolean useSSL) {
            this.useSSL = useSSL;
        }

        /**
         * Gets the {@code host} for the connection (default {@code localhost}).
         *
         * @return The {@code host} for the connection.
         */
        public String getHost() {
            return host != null && !host.isEmpty() ? host : DEFAULT_HOST;
        }

        /**
         * Sets the {@code host} for the connection.
         *
         * @param host
         *         The {@code host} for the connection.
         */
        public void setHost(final String host) {
            this.host = host;
        }

        /**
         * Gets the {@code port} for the connection (default {@code 8088}).
         *
         * @return The {@code port} for the connection.
         */
        public int getPort() {
            return port > 0 ? port : DEFAULT_PORT;
        }

        /**
         * Sets the {@code port} for the connection.
         *
         * @param port
         *         The {@code port} for the connection.
         */
        public void setPort(final int port) {
            this.port = port;
        }

    }

    /**
     * Configuration of event buffering.
     */
    public final static class BufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.splunk.buffering.maxSize")
        private int maxSize;

        @JsonPropertyDescription("audit.handlers.splunk.buffering.writeInterval")
        private String writeInterval;

        @JsonPropertyDescription("audit.handlers.splunk.buffering.maxBatchedEvents")
        private int maxBatchedEvents;

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
         * @param maxSize
         *         buffer capacity
         */
        public void setMaxSize(final int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * Gets the interval for reading events from the buffer to transmit to splunk.
         *
         * @return Interval (e.g., "20 millis")
         */
        public String getWriteInterval() {
            return writeInterval;
        }

        /**
         * Sets the interval for reading events from the buffer to transmit to splunk.
         *
         * @param writeInterval
         *         Interval (e.g., "20 millis")
         */
        public void setWriteInterval(final String writeInterval) {
            this.writeInterval = writeInterval;
        }

        /**
         * Gets the maximum number of events to read from the buffer on each {@link #getWriteInterval() interval}.
         *
         * @return Batch size
         */
        public int getMaxBatchedEvents() {
            return maxBatchedEvents;
        }

        /**
         * Sets the maximum number of events to read from the buffer on each {@link #getWriteInterval() interval}.
         *
         * @param maxBatchedEvents
         *         Batch size
         */
        public void setMaxBatchedEvents(final int maxBatchedEvents) {
            this.maxBatchedEvents = maxBatchedEvents;
        }
    }

}
