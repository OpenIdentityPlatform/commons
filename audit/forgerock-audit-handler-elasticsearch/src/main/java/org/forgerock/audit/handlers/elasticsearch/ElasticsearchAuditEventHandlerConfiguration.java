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
package org.forgerock.audit.handlers.elasticsearch;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * A configuration for Elasticsearch audit event handler.
 * <p/>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <pre>
 *  {
 *    "name" : "elasticsearch",
 *    "topics": [ "access", "activity", "config", "authentication" ],
 *    "connection" : {
 *      "useSSL" : true,
 *      "host" : "localhost",
 *      "port" : 9200,
 *      "username" : "myUsername",
 *      "password" : "myPassword"
 *    },
 *    "indexMapping" : {
 *      "indexName" : "audit"
 *    },
 *    "buffering" : {
 *      "enabled" : true,
 *      "maxSize" : 10000,
 *      "writeInterval" : "250 millis",
 *      "maxBatchedEvents" : 500
 *    }
 *  }
 * </pre>
 */
public class ElasticsearchAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("audit.handlers.elasticsearch.connection")
    private ConnectionConfiguration connection = new ConnectionConfiguration();

    @JsonPropertyDescription("audit.handlers.elasticsearch.indexMapping")
    private IndexMappingConfiguration indexMapping = new IndexMappingConfiguration();

    @JsonPropertyDescription("audit.handlers.elasticsearch.buffering")
    private EventBufferingConfiguration buffering = new EventBufferingConfiguration();

    /**
     * Gets configuration of connection to Elasticsearch.
     *
     * @return configuration of connection to Elasticsearch
     */
    public ConnectionConfiguration getConnection() {
        return connection;
    }

    /**
     * Sets configuration of connection to Elasticsearch.
     *
     * @param connection configuration of connection to Elasticsearch
     */
    public void setConnection(ConnectionConfiguration connection) {
        this.connection = connection;
    }

    /**
     * Sets configuration of index mapping.
     *
     * @return configuration of index mapping
     */
    public IndexMappingConfiguration getIndexMapping() {
        return indexMapping;
    }

    /**
     * Gets configuration of index mapping.
     *
     * @param indexMapping configuration of index mapping
     */
    public void setIndexMapping(IndexMappingConfiguration indexMapping) {
        this.indexMapping = indexMapping;
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
     * Configuration of connection to Elasticsearch.
     */
    public static class ConnectionConfiguration {

        /**
         * Elasticsearch default host ({@code localhost}) in a development environment.
         */
        private static final String DEFAULT_HOST = "localhost";

        /**
         * Elasticsearch default port ({@code 9200}) in a development environment.
         */
        private static final int DEFAULT_PORT = 9200;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.useSSL")
        private boolean useSSL;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.host")
        private String host;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.port")
        private int port;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.username")
        private String username;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.password")
        private String password;

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
         * @param useSSL {@code true} when the connection uses SSL.
         */
        public void setUseSSL(boolean useSSL) {
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
         * @param host The {@code host} for the connection.
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * Gets the {@code port} for the connection (default {@code 9200}).
         *
         * @return The {@code port} for the connection.
         */
        public int getPort() {
            return port > 0 ? port : DEFAULT_PORT;
        }

        /**
         * Sets the {@code port} for the connection.
         *
         * @param port The {@code port} for the connection.
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Gets Elasticsearch password for HTTP basic authentication.
         *
         * @return The password.
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets Elasticsearch password for HTTP basic authentication.
         *
         * @param password The password.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets Elasticsearch username for HTTP basic authentication.
         *
         * @return The username.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets Elasticsearch username for HTTP basic authentication.
         *
         * @param username The username.
         */
        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * Configuration of index mapping.
     */
    public static class IndexMappingConfiguration {

        private static final String DEFAULT_INDEX_NAME = "audit";

        @JsonPropertyDescription("audit.handlers.elasticsearch.indexMapping.indexName")
        private String indexName;

        /**
         * Gets primary index name (default is {@code audit}).
         *
         * @return Index name
         */
        public String getIndexName() {
            return indexName != null && !indexName.isEmpty() ? indexName : DEFAULT_INDEX_NAME;
        }

        /**
         * Sets primary index name.
         *
         * @param indexName Index name
         */
        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }
    }

    /**
     * Configuration of event buffering.
     */
    public static class EventBufferingConfiguration {

        @JsonPropertyDescription("audit.handlers.elasticsearch.buffering.enabled")
        private boolean enabled;

        @JsonPropertyDescription("audit.handlers.elasticsearch.buffering.maxSize")
        private int maxSize;

        @JsonPropertyDescription("audit.handlers.elasticsearch.buffering.writeInterval")
        private String writeInterval;

        @JsonPropertyDescription("audit.handlers.elasticsearch.buffering.maxBatchedEvents")
        private int maxBatchedEvents;

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
         * @param enabled Indicates if buffering is enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

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
         * Gets the interval for reading events from the buffer to transmit to Elasticsearch.
         *
         * @return Interval (e.g., "20 millis")
         */
        public String getWriteInterval() {
            return writeInterval;
        }

        /**
         * Sets the interval for reading events from the buffer to transmit to Elasticsearch.
         *
         * @param writeInterval Interval (e.g., "20 millis")
         */
        public void setWriteInterval(String writeInterval) {
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
         * @param maxBatchedEvents Batch size
         */
        public void setMaxBatchedEvents(int maxBatchedEvents) {
            this.maxBatchedEvents = maxBatchedEvents;
        }
    }
}
