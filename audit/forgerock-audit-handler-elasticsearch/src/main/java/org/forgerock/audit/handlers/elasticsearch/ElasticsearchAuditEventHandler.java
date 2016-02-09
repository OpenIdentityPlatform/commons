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

import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.ConnectionConfiguration;
import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.IndexMappingConfiguration;
import static org.forgerock.http.util.Json.readJson;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CountPolicy;
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
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AuditEventHandler} for Elasticsearch.
 */
public class ElasticsearchAuditEventHandler extends AuditEventHandlerBase implements
        ElasticsearchBatchAuditEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAuditEventHandler.class);
    private static final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor =
            new ElasticsearchQueryFilterVisitor();

    private static final String QUERY = "query";
    private static final String GET = "GET";
    private static final String SEARCH = "/_search";
    private static final String BULK = "/_bulk";
    private static final String HITS = "hits";
    private static final String SOURCE = "_source";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String TOTAL = "total";
    private static final String PUT = "PUT";
    private static final String POST = "POST";

    /**
     * Average number of characters, per event, for batch indexing via Elasticsearch Bulk API. This value
     * is used to initialize the size of buffers, but if the value is too low, the buffers will automatically resize
     * as needed.
     */
    private static final int BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE = 1280;

    /**
     * The Elasticsearch {@link AuditEventHandler} <b>always</b> flushes events in the batch queue on shutdown or
     * configuration change.
     */
    private static final boolean ALWAYS_FLUSH_BATCH_QUEUE = true;
    private static final int DEFAULT_OFFSET = 0;

    private final String basicAuthHeaderValue;
    private final String baseUri;
    private final String bulkUri;
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
            @Audit final Client client) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.configuration = Reject.checkNotNull(configuration);
        this.client = Reject.checkNotNull(client);
        basicAuthHeaderValue = buildBasicAuthHeaderValue();
        baseUri = buildBaseUri();
        bulkUri = buildBulkUri();

        final EventBufferingConfiguration bufferConfig = configuration.getBuffering();
        if (bufferConfig.isEnabled()) {
            final Duration writeInterval =
                    bufferConfig.getWriteInterval() == null || bufferConfig.getWriteInterval().isEmpty()
                            ? null
                            : Duration.duration(bufferConfig.getWriteInterval());
            batchIndexer = new ElasticsearchBatchIndexer(bufferConfig.getMaxSize(),
                    writeInterval, bufferConfig.getMaxBatchedEvents(),
                    BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE, ALWAYS_FLUSH_BATCH_QUEUE, this);
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

    /**
     * Queries the elastic search
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html">Search API</a> for
     * audit events.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest query, final QueryResourceHandler handler) {
        try {
            final int pageSize = query.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : query.getPageSize();
            // set the offset to either first the offset provided, or second the paged result cookie value, or finally 0
            final int offset;
            if (query.getPagedResultsOffset() != 0) {
                offset = query.getPagedResultsOffset();
            } else if (query.getPagedResultsCookie() != null) {
                offset = Integer.valueOf(query.getPagedResultsCookie());
            } else {
                offset = DEFAULT_OFFSET;
            }

            final JsonValue payload =
                    json(object(field(
                            QUERY, query.getQueryFilter().accept(elasticsearchQueryFilterVisitor, null).getObject())));
            final Request request = createRequest(GET, buildSearchUri(topic, pageSize, offset), payload.getObject());
            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                final String message = "Elasticsearch response (audit/" + topic + SEARCH + "): " + response.getEntity();
                return newResourceException(response.getStatus().getCode(), message).asPromise();
            }
            JsonValue events = json(response.getEntity().getJson());
            for (JsonValue event : events.get(HITS).get(HITS)) {
                handler.handleResource(
                        newResourceResponse(event.get(FIELD_CONTENT_ID).asString(), null,
                                ElasticsearchUtil.denormalizeJson(event.get(SOURCE))));
            }
            final int totalResults = events.get(HITS).get(TOTAL).asInteger();
            final String pagedResultsCookie = (pageSize + offset) >= totalResults
                    ? null
                    : Integer.toString(pageSize + offset);
            return newQueryResponse(pagedResultsCookie, CountPolicy.EXACT, totalResults).asPromise();
        } catch (URISyntaxException | ExecutionException | InterruptedException | IOException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String topic,
            final String resourceId) {
        try {
            final Request request = createRequest(GET, buildEventUri(topic, resourceId), null);

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                return getResourceExceptionPromise(topic, resourceId, response);
            }

            // the original audit JSON is under _source, and we also add back the _id
            JsonValue jsonValue = json(readJson(response.getEntity().toString()));
            jsonValue = ElasticsearchUtil.denormalizeJson(jsonValue.get(SOURCE));
            jsonValue.put(FIELD_CONTENT_ID, resourceId);
            return newResourceResponse(resourceId, null, jsonValue).asPromise();
        } catch (Exception e) {
            final String error = String.format("Unable to read audit entry for topic=%s, _id=%s", topic, resourceId);
            logger.error(error, e);
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
            // _id is a protected Elasticsearch field, so read it and remove it
            resourceId = event.get(FIELD_CONTENT_ID).asString();
            event.remove(FIELD_CONTENT_ID);
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event).toString();
            event.put(FIELD_CONTENT_ID, resourceId);

            final Request request = createRequest(PUT, buildEventUri(topic, resourceId), jsonPayload);

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                return getResourceExceptionPromise(topic, resourceId, response);
            }
            return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null,
                    event).asPromise();
        } catch (Exception e) {
            final String error = String.format("Unable to create audit entry for topic=%s, _id=%s", topic,
                    resourceId);
            logger.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }
    }

    @Override
    public void addToBatch(final String topic, final JsonValue event, final StringBuilder payload) {
        try {
            // _id is a protected Elasticsearch field
            final String resourceId = event.get(FIELD_CONTENT_ID).asString();
            event.remove(FIELD_CONTENT_ID);
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event).toString();

            // newlines have special significance in the Bulk API
            // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
            payload.append("{ \"index\" : { \"_type\" : ")
                    .append(ElasticsearchUtil.objectMapper.writeValueAsString(topic))
                    .append(", \"_id\" : ")
                    .append(ElasticsearchUtil.objectMapper.writeValueAsString(resourceId))
                    .append(" } }\n")
                    .append(jsonPayload)
                    .append('\n');
        } catch (Exception e) {
            logger.error("Elasticsearch batch creation failed", e);
        }
    }

    @Override
    public void publishBatch(final String payload) {
        try {
            final Request request = createRequest(POST, buildBulkUri(), payload);

            final Response response = client.send(request).get();
            if (!response.getStatus().isSuccessful()) {
                logger.warn("Elasticsearch batch index failed: " + response.getEntity());
            }
        } catch (Exception e) {
            logger.error("Elasticsearch batch index failed unexpectedly", e);
        }
    }

    /**
     * Builds a basic authentication header-value, if username and password are provided in configuration.
     *
     * @return Basic authentication header-value or {@code null} if not configured
     */
    protected String buildBasicAuthHeaderValue() {
        if (basicAuthHeaderValue != null) {
            return basicAuthHeaderValue;
        }
        final ConnectionConfiguration connection = configuration.getConnection();
        if (connection.getUsername() == null || connection.getUsername().isEmpty() ||
                connection.getPassword() == null || connection.getPassword().isEmpty()) {
            return null;
        }
        final String credentials = connection.getUsername() + ":" + connection.getPassword();
        return "Basic " + Base64.encode(credentials.getBytes());
    }

    /**
     * Builds an Elasticsearch API URI for operating on a single event (e.g., index, get, etc.).
     *
     * @param topic Audit topic
     * @param eventId Event ID
     * @return URI
     */
    protected String buildEventUri(final String topic, final String eventId) {
        return buildBaseUri() + "/" + topic + "/" + eventId;
    }

    /**
     * Builds an Elasticsearch API URI for Bulk API.
     *
     * @return URI
     */
    protected String buildBulkUri() {
        if (bulkUri != null) {
            return bulkUri;
        }
        return buildBaseUri() + BULK;
    }

    /**
     * Builds an Elasticsearch API URI for Search API.
     *
     * @param topic The audit topic to search.
     * @param pageSize The number of results to return.
     * @param offset The number of results to skip.
     * @return The search uri.
     */
    protected String buildSearchUri(final String topic, final int pageSize, final int offset) {
        return buildBaseUri() + "/" + topic + SEARCH + "?size=" + pageSize + "&from=" + offset;
    }

    /**
     * Builds an Elasticsearch API base URI. The format is,
     * <pre>http[s]://host:port/indexName</pre>
     *
     * @return Base URI
     */
    protected String buildBaseUri() {
        if (baseUri != null) {
            return baseUri;
        }
        final IndexMappingConfiguration indexMapping = configuration.getIndexMapping();
        final ConnectionConfiguration connection = configuration.getConnection();
        return (connection.isUseSSL() ? "https" : "http") + "://" + connection.getHost() + ":" + connection.getPort() +
                "/" + indexMapping.getIndexName();
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
        return newResourceException(response.getStatus().getCode(), message).asPromise();
    }

    private Request createRequest(final String method, final String uri, final Object payload)
            throws URISyntaxException {
        final Request request = new Request();
        request.setMethod(method);
        request.setUri(uri);
        if (payload != null) {
            request.getHeaders().put(ContentTypeHeader.NAME, "application/json; charset=UTF-8");
            request.setEntity(payload);
        }
        if (basicAuthHeaderValue != null) {
            request.getHeaders().put("Authorization", basicAuthHeaderValue);
        }
        return request;
    }
}
