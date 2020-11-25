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

import static org.forgerock.audit.util.ElasticsearchUtil.OBJECT_MAPPER;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.events.handlers.buffering.BufferedBatchPublisher;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.ConnectionConfiguration;
import org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.events.handlers.buffering.BatchConsumer;
import org.forgerock.audit.events.handlers.buffering.BatchPublisher;
import org.forgerock.audit.events.handlers.buffering.BatchException;
import org.forgerock.audit.util.ElasticsearchUtil;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.spi.Loader;
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
import org.forgerock.util.Function;
import org.forgerock.util.Options;
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
        BatchConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchAuditEventHandler.class);
    private static final ElasticsearchQueryFilterVisitor ELASTICSEARCH_QUERY_FILTER_VISITOR =
            new ElasticsearchQueryFilterVisitor();

    private static final String QUERY = "query";
    private static final String GET = "GET";
    private static final String SEARCH = "/_search";
    private static final String BULK = "/_bulk";
    private static final String HITS = "hits";
    private static final String SOURCE = "_source";
    private static final String DOC = "_doc/";
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

    private final String indexName;
    private final String basicAuthHeaderValue;
    private final String baseUri;
    private final String bulkUri;
    private final ElasticsearchAuditEventHandlerConfiguration configuration;
    private final Client client;
    private final BatchPublisher batchIndexer;
    private final HttpClientHandler defaultHttpClientHandler;

    /**
     * Create a new {@code ElasticsearchAuditEventHandler} instance.
     *
     * @param configuration Configuration parameters that can be adjusted by system administrators.
     * @param eventTopicsMetaData Meta-data for all audit event topics.
     * @param client HTTP client or {@code null} to use default client.
     */
    public ElasticsearchAuditEventHandler(
            final ElasticsearchAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final Client client) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.configuration = Reject.checkNotNull(configuration);
        if (client == null) {
            this.defaultHttpClientHandler = defaultHttpClientHandler();
            this.client = new Client(defaultHttpClientHandler);
        } else {
            this.defaultHttpClientHandler = null;
            this.client = client;
        }
        indexName = configuration.getIndexMapping().getIndexName();
        basicAuthHeaderValue = buildBasicAuthHeaderValue();
        baseUri = buildBaseUri();
        bulkUri = buildBulkUri();

        final EventBufferingConfiguration bufferConfig = configuration.getBuffering();
        if (bufferConfig.isEnabled()) {
            final Duration writeInterval =
                    bufferConfig.getWriteInterval() == null || bufferConfig.getWriteInterval().isEmpty()
                            ? null
                            : Duration.duration(bufferConfig.getWriteInterval());
            batchIndexer = BufferedBatchPublisher.newBuilder(this)
                    .capacity(bufferConfig.getMaxSize())
                    .writeInterval(writeInterval)
                    .maxBatchEvents(bufferConfig.getMaxBatchedEvents())
                    .averagePerEventPayloadSize(BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE)
                    .autoFlush(ALWAYS_FLUSH_BATCH_QUEUE)
                    .build();
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
        if (defaultHttpClientHandler != null) {
            try {
                defaultHttpClientHandler.close();
            } catch (IOException e) {
                throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR,
                        "An error occurred while closing the default HTTP client handler", e);
            }
        }
    }

    /**
     * Queries the Elasticsearch
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html">Search API</a> for
     * audit events.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
           final QueryRequest query, final QueryResourceHandler handler) {
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
                        QUERY, query.getQueryFilter().accept(ELASTICSEARCH_QUERY_FILTER_VISITOR, null).getObject())));
        try {
            final Request request = createRequest(GET, buildSearchUri(topic, pageSize, offset), payload.getObject());
            return client.send(request).then(closeSilently(new Function<Response, QueryResponse, ResourceException>() {
                    @Override
                    public QueryResponse apply(Response response) throws ResourceException {
                        if (!response.getStatus().isSuccessful()) {
                            final String message =
                                    "Elasticsearch response (" + indexName + "_" + topic + SEARCH + "): "
                                    + response.getEntity();
                            throw newResourceException(response.getStatus().getCode(), message);
                        }
                        try {
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
                            return newQueryResponse(pagedResultsCookie,
                                    CountPolicy.EXACT,
                                    totalResults);
                        } catch (IOException e) {
                            throw new InternalServerErrorException(e.getMessage(), e);
                        }
                    }
            }), Responses.<QueryResponse, ResourceException>noopExceptionFunction());
        } catch (URISyntaxException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String topic,
            final String resourceId) {
        final Request request;
        try {
            request = createRequest(GET, buildEventUri(topic, resourceId), null);
        } catch (Exception e) {
            final String error = String.format("Unable to read audit entry for topic=%s, _id=%s", topic, resourceId);
            LOGGER.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }

        return client.send(request).then(closeSilently(new Function<Response, ResourceResponse, ResourceException>() {
                @Override
                public ResourceResponse apply(Response response) throws ResourceException {
                    if (!response.getStatus().isSuccessful()) {
                        throw resourceException(indexName, topic, resourceId, response);
                    }

                    try {
                        // the original audit JSON is under _source, and we also add back the _id
                        JsonValue jsonValue = json(response.getEntity().getJson());
                        jsonValue = ElasticsearchUtil.denormalizeJson(jsonValue.get(SOURCE));
                        jsonValue.put(FIELD_CONTENT_ID, resourceId);
                        return newResourceResponse(resourceId, null, jsonValue);
                    } catch (IOException e) {
                        throw new InternalServerErrorException(e.getMessage(), e);
                    }
                }
        }), Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String topic,
            final JsonValue event) {
        if (batchIndexer == null) {
            return publishSingleEvent(topic, event);
        } else {
            if (!batchIndexer.offer(topic, event)) {
                return new ServiceUnavailableException("Elasticsearch batch indexer full, so dropping audit event "
                        + indexName + "_" + topic + "/" + event.get("_id").asString()).asPromise();
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
        // _id is a protected Elasticsearch field, so read it and remove it
        final String resourceId = event.get(FIELD_CONTENT_ID).asString();
        event.remove(FIELD_CONTENT_ID);

        try {
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event);
            event.put(FIELD_CONTENT_ID, resourceId);
            final Request request = createRequest(POST, buildEventUri(topic, DOC + resourceId), jsonPayload);
            return client.send(request).then(
                    closeSilently(new Function<Response, ResourceResponse, ResourceException>() {
                        @Override
                        public ResourceResponse apply(Response response) throws ResourceException {
                            if (!response.getStatus().isSuccessful()) {
                                throw resourceException(indexName, topic, resourceId, response);
                            }
                            return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null,
                                    event);
                        }
                    }), Responses.<ResourceResponse, ResourceException>noopExceptionFunction());
        } catch (Exception e) {
            final String error = String.format("Unable to create audit entry for topic=%s, _id=%s", topic, resourceId);
            LOGGER.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }
    }

    /**
     * Adds an audit event to an Elasticsearch Bulk API payload.
     *
     * @param topic Event topic
     * @param event Event JSON payload
     * @param payload Elasticsearch Bulk API payload
     * @throws BatchException indicates failure to add-to-batch
     */
    @Override
    public void addToBatch(final String topic, final JsonValue event, final StringBuilder payload)
            throws BatchException {
        try {
            final String fullIndexName = indexName + "_" + topic;
            // _id is a protected Elasticsearch field
            final String resourceId = event.get(FIELD_CONTENT_ID).asString();
            event.remove(FIELD_CONTENT_ID);
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event);
            event.put(FIELD_CONTENT_ID, resourceId);

            // newlines have special significance in the Bulk API
            // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
            payload.append("{ \"index\" : { \"_index\" : ")
                    .append(OBJECT_MAPPER.writeValueAsString( fullIndexName ))
                    .append(", \"_id\" : ")
                    .append(OBJECT_MAPPER.writeValueAsString(resourceId))
                    .append(" } }\n")
                    .append(jsonPayload)
                    .append('\n');
        } catch (IOException e) {
            throw new BatchException("Unexpected error while adding to batch", e);
        }
    }

    /**
     * Publishes a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API</a>
     * payload to Elasticsearch.
     *
     * @param payload Elasticsearch Bulk API payload
     * @throws BatchException indicates (full or partial) failure to publish batch
     */
    @Override
    public Promise<Void, BatchException> publishBatch(final String payload) {
        final Request request;
        try {
            request = createRequest(POST, buildBulkUri(), payload);
        } catch (URISyntaxException e) {
            return newExceptionPromise(new BatchException("Incorrect URI", e));
        }

        return client.send(request)
                .then(closeSilently(processBatchResponse()), Responses.<Void, BatchException>noopExceptionFunction());
    }

    private Function<Response, Void, BatchException> processBatchResponse() {
        return new Function<Response, Void, BatchException>() {
            @Override
            public Void apply(Response response) throws BatchException {
                try {
                    if (!response.getStatus().isSuccessful()) {
                        throw new BatchException("Elasticsearch batch index failed: " + response.getEntity());
                    } else {
                        final JsonValue responseJson = json(response.getEntity().getJson());
                        if (responseJson.get("errors").asBoolean()) {
                            // one or more batch index operations failed, so log failures
                            final JsonValue items = responseJson.get("items");
                            final int n = items.size();
                            final List<Object> failureItems = new ArrayList<>(n);
                            for (int i = 0; i < n; ++i) {
                                final JsonValue item = items.get(i).get("index");
                                final Integer status = item.get("status").asInteger();
                                if (status >= 400) {
                                    failureItems.add(item);
                                }
                            }
                            final String message = "One or more Elasticsearch batch index entries failed: "
                                    + OBJECT_MAPPER.writeValueAsString(failureItems);
                            throw new BatchException(message);
                        }
                    }
                } catch (IOException e) {
                    throw new BatchException("Unexpected error while publishing batch", e);
                }
                return null;
            }
        };
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
        if (connection.getUsername() == null || connection.getUsername().isEmpty()
                || connection.getPassword() == null || connection.getPassword().isEmpty()) {
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
        return buildBaseUri() + "_" + topic + "/" + eventId;
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
        final ConnectionConfiguration connection = configuration.getConnection();
        return (connection.isUseSSL() ? "https" : "http") + "://" + connection.getHost() + ":" + connection.getPort() + BULK;
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
        return buildBaseUri() + "_" + topic + SEARCH + "?size=" + pageSize + "&from=" + offset;
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
        final ConnectionConfiguration connection = configuration.getConnection();
        return (connection.isUseSSL() ? "https" : "http") + "://" + connection.getHost() + ":" + connection.getPort()
                + "/" + indexName;
    }

    /**
     * Gets an {@code Exception} {@link Promise} containing an Elasticsearch HTTP response status and payload.
     *
     * @param indexName Index name
     * @param topic Event topic
     * @param resourceId Event ID
     * @param response HTTP response
     * @return {@code Exception} {@link Promise}
     */
    protected static ResourceException resourceException(
            final String indexName, final String topic, final String resourceId, final Response response) {
        if (response.getStatus().getCode() == ResourceException.NOT_FOUND) {
            return new NotFoundException("Object " + resourceId + " not found in " + indexName + "-" + topic);
        }
        final String message = "Elasticsearch response (" + indexName + "_" + topic + "/" + resourceId + "): "
                + response.getEntity();
        return newResourceException(response.getStatus().getCode(), message);
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

    private HttpClientHandler defaultHttpClientHandler() {
        try {
            return new HttpClientHandler(
                    Options.defaultOptions()
                            .set(OPTION_LOADER, new Loader() {
                                @Override
                                public <S> S load(Class<S> service, Options options) {
                                    return service.cast(new AsyncHttpClientProvider());
                                }
                            }));
        } catch (HttpApplicationException e) {
            throw new RuntimeException("Error while building default HTTP Client", e);
        }
    }
}

