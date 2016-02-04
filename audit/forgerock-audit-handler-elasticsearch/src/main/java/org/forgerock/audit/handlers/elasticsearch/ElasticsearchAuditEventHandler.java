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

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.*;

/**
 * {@link AuditEventHandler} for Elasticsearch.
 */
public class ElasticsearchAuditEventHandler extends AuditEventHandlerBase implements
        ElasticsearchBatchAuditEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchAuditEventHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Average number of characters, per event, for batch indexing via Elasticsearch Bulk API. This value
     * is used to initialize the size of buffers, but if the value is too low, the buffers will automatically resize
     * as needed.
     */
    private static final int BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE = 1280;

    private final ElasticsearchAuditEventHandlerConfiguration configuration;
    private final Client client;
    private final ElasticsearchBatchIndexer batchIndexer;

    /**
     * Create a new {@code ElasticsearchAuditEventHandler} instance.
     *
     * @param configuration Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData Meta-data for all audit event topics.
     * @param client HTTP client.
     */
    public ElasticsearchAuditEventHandler(
            final ElasticsearchAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final Client client
    ) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.configuration = Reject.checkNotNull(configuration);
        this.client = Reject.checkNotNull(client);

        final EventBufferingConfiguration bufferConfig = configuration.getBuffering();
        if (bufferConfig.isEnabled()) {
            final Duration writeInterval =
                    bufferConfig.getWriteInterval() == null || bufferConfig.getWriteInterval().isEmpty() ?
                            null : Duration.duration(bufferConfig.getWriteInterval());
            batchIndexer = new ElasticsearchBatchIndexer(bufferConfig.getMaxSize(),
                    writeInterval, bufferConfig.getMaxBatchedEvents(),
                    BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE, bufferConfig.isAutoFlush(), this);
        } else {
            batchIndexer = null;
        }
    }

    @Override
    public void startup() throws ResourceException {
        if (batchIndexer != null) {
            batchIndexer.startup();
        }
    }

    @Override
    public void shutdown() throws ResourceException {
        if (batchIndexer != null) {
            batchIndexer.shutdown();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest query, final QueryResourceHandler handler) {
        // TODO
        return newQueryResponse().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String topic,
            final String resourceId) {
        try {
            final Request request = new Request();
            request.setMethod("GET");
            request.setUri(buildEventUri(topic, resourceId));

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                return getResourceExceptionPromise(topic, resourceId, response);
            }

            // the original audit JSON is under _source, and we also add back the _id
            JsonValue jsonValue = toJsonValue(response.getEntity().toString());
            jsonValue = jsonValue.get("_source");
            jsonValue.put("_id", resourceId);
            return newResourceResponse(resourceId, null, jsonValue).asPromise();
        } catch (Exception e) {
            final String error = String.format("Unable to read audit entry for topic=%s, _id=%s", topic, resourceId);
            LOGGER.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String topic,
            final JsonValue event) {
        if (batchIndexer == null) {
            return publishSingleEvent(topic, event);
        } else {
            if (!batchIndexer.offer(topic, event)) {
                return new ServiceUnavailableException("Elasticsearch batch indexer full, so dropping audit event " +
                        "audit/" + topic + "/" + event.get("_id").asString()).asPromise();
            }
            return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null,
                    event).asPromise();
        }
    }

    /**
     * Publishes a single event to the provided topic.
     *
     * @param topic The topic where to publish the event.
     * @param event The event to publish.
     * @return a promise with either a response or an exception
     */
    protected Promise<ResourceResponse, ResourceException> publishSingleEvent(final String topic,
            final JsonValue event) {
        String resourceId = null;
        try {
            // _id is a protected Elasticsearch field
            resourceId = event.get("_id").asString();
            event.remove("_id");

            // normalize JSON and put _id back for call to newResourceResponse below
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event);
            event.put("_id", resourceId);

            final Request request = new Request();
            request.setMethod("PUT");
            request.setUri(buildEventUri(topic, resourceId));
            if (jsonPayload != null) {
                request.getHeaders().put(ContentTypeHeader.NAME, "application/json; charset=UTF-8");
                request.getEntity().setBytes(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                return getResourceExceptionPromise(topic, resourceId, response);
            }
            return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null,
                    event).asPromise();
        } catch (Exception e) {
            final String error = String.format("Unable to create audit entry for topic=%s, _id=%s", topic,
                    resourceId);
            LOGGER.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }
    }

    @Override
    public void addToBatch(final String topic, final JsonValue event, final StringBuilder payload) {
        try {
            // _id is a protected Elasticsearch field
            final String resourceId = event.get("_id").asString();
            event.remove("_id");
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event);

            // newlines have special significance in the Bulk API
            // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
            payload.append("{ \"index\" : { \"_type\" : ")
                    .append(OBJECT_MAPPER.writeValueAsString(topic))
                    .append(", \"_id\" : ")
                    .append(OBJECT_MAPPER.writeValueAsString(resourceId))
                    .append(" } }\n")
                    .append(jsonPayload)
                    .append('\n');
        } catch (Exception e) {
            LOGGER.error("Elasticsearch batch creation failed", e);
        }
    }

    @Override
    public void publishBatch(final String payload) {
        try {
            final Request request = new Request();
            request.setMethod("POST");
            request.setUri(buildBulkUri());
            request.getHeaders().put(ContentTypeHeader.NAME, "application/json; charset=UTF-8");
            request.getEntity().setBytes(payload.getBytes(StandardCharsets.UTF_8));

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                LOGGER.warn("Elasticsearch batch index failed: " + response.getEntity());
            }
        } catch (Exception e) {
            LOGGER.error("Elasticsearch batch index failed unexpectedly", e);
        }
    }

    /**
     * Builds an Elasticsearch API URI for operating on a single event (e.g., index, get, etc.).
     *
     * @param topic Audit topic
     * @param eventId Event ID
     * @return URI
     */
    protected String buildEventUri(final String topic, final String eventId) {
        final IndexMappingConfiguration indexMapping = configuration.getIndexMapping();
        final ConnectionConfiguration connection = configuration.getConnection();
        return (connection.isUseSSL() ? "https" : "http") + "://" + connection.getHost() + "/" +
                indexMapping.getIndexName() + "/" + topic + "/" + eventId;
    }

    /**
     * Builds an Elasticsearch API URI for Bulk API.
     *
     * @return URI
     */
    protected String buildBulkUri() {
        final IndexMappingConfiguration indexMapping = configuration.getIndexMapping();
        final ConnectionConfiguration connection = configuration.getConnection();
        return (connection.isUseSSL() ? "https" : "http") + "://" + connection.getHost() + "/" +
                indexMapping.getIndexName() + "/_bulk";
    }

    /**
     * Gets an {@code Exception} {@link Promise} containing an Elasticsearch HTTP response status and payload.
     *
     * @param response HTTP response
     * @return {@code Exception} {@link Promise}
     */
    protected static Promise<ResourceResponse, ResourceException> getResourceExceptionPromise(
            final String topic, final String resourceId, final Response response) {
        if (response.getStatus().getCode() == ResourceException.NOT_FOUND) {
            return new NotFoundException("Object " + resourceId + " not found in audit/" + topic).asPromise();
        }
        final String message = "Elasticsearch response (audit/" + topic + "/" + resourceId + "): "
                + response.getEntity();
        return ResourceException.newResourceException(response.getStatus().getCode(), message).asPromise();
    }

    /**
     * Parses a JSON string into a {@link JsonValue}.
     *
     * @param json JSON string
     * @return {@link JsonValue}
     */
    protected static JsonValue toJsonValue(final String json) {
        try {
            final Object parsedValue = OBJECT_MAPPER.readValue(json, Object.class);
            return new JsonValue(parsedValue);
        } catch (IOException ex) {
            throw new JsonException("String is not valid JSON", ex);
        }
    }
}
