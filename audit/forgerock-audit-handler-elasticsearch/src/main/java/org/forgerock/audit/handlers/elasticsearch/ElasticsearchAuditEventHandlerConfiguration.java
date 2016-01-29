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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

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
 *      "host" : "localhost:9200"
 *    },
 *    "indexMapping" : {
 *      "indexName" : "audit"
 *    },
 *    "buffering" : {
 *      "enabled" : "true"
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

    /**
     * Configuration of connection to Elasticsearch.
     */
    public static class ConnectionConfiguration {

        private static final String DEFAULT_HOST = "localhost:9200";

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.useSSL")
        private boolean useSSL;

        @JsonPropertyDescription("audit.handlers.elasticsearch.connection.host")
        private String host;

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
         * Gets the {@code host[:port]} for the connection (default {@code localhost:9200}).
         *
         * @return The {@code host[:port]} for the connection.
         */
        public String getHost() {
            return host != null && !host.isEmpty() ? host : DEFAULT_HOST;
        }

        /**
         * Sets the {@code host[:port]} for the connection.
         *
         * @param host The {@code host[:port]} for the connection.
         */
        public void setHost(String host) {
            this.host = host;
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
    }
}
