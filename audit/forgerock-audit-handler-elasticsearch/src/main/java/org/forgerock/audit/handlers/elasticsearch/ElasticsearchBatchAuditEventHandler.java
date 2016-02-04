package org.forgerock.audit.handlers.elasticsearch;

import org.forgerock.json.JsonValue;

/**
 * Elasticsearch batch audit event handler.
 */
interface ElasticsearchBatchAuditEventHandler {

    /**
     * Adds an audit event to an Elasticsearch Bulk API payload.
     *
     * @param topic Event topic
     * @param event Event JSON payload
     * @param payload Elasticsearch Bulk API payload
     */
    void addToBatch(String topic, JsonValue event, StringBuilder payload);

    /**
     * Publishes a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API</a>
     * payload to Elasticsearch.
     *
     * @param payload Elasticsearch Bulk API payload
     */
    void publishBatch(String payload);
}
