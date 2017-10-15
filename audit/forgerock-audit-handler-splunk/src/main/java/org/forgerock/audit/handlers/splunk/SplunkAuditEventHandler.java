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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.events.handlers.buffering.BatchConsumer;
import org.forgerock.audit.events.handlers.buffering.BatchException;
import org.forgerock.audit.events.handlers.buffering.BatchPublisher;
import org.forgerock.audit.events.handlers.buffering.BatchPublisherFactory;
import org.forgerock.audit.events.handlers.buffering.BatchPublisherFactoryImpl;
import org.forgerock.audit.handlers.splunk.SplunkAuditEventHandlerConfiguration.BufferingConfiguration;
import org.forgerock.audit.handlers.splunk.SplunkAuditEventHandlerConfiguration.ConnectionConfiguration;
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
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Audit event handler that writes out to Splunk's HTTP event collector RAW endpoint.
 */
public final class SplunkAuditEventHandler extends AuditEventHandlerBase implements BatchConsumer {

    /*
     * Value is used to initialize the size of buffers, but if the value
     * is too low, the buffers will automatically resize as needed.
     */
    private static final int BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE = 1280;

    /*
     * The Elasticsearch {@link AuditEventHandler} <b>always</b> flushes
     * events in the batch queue on shutdown or  configuration change.
     */
    private static final boolean ALWAYS_FLUSH_BATCH_QUEUE = true;

    /*
     * Using {@link ObjectMapper} in favour over {@link JsonValue#toString}
     * as this is considered to produce more reliable json.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SplunkAuditEventHandlerConfiguration configuration;
    private final Client client;
    private final HttpClientHandler defaultHttpClientHandler;
    private final String channelId;
    private final BatchPublisher batchPublisher;
    private final String serviceUrl;

    /**
     * Constructs a new Splunk audit event handler.
     *
     * @param configuration
     *         the Splunk audit event handler configuration
     * @param eventTopicsMetaData
     *         topic meta data
     * @param publisherFactory
     *         the batch publisher factory or {@code null}
     * @param client
     *         HTTP client or {@code null}
     */
    public SplunkAuditEventHandler(
            final SplunkAuditEventHandlerConfiguration configuration, final EventTopicsMetaData eventTopicsMetaData,
            @Audit BatchPublisherFactory publisherFactory, final @Audit Client client) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());

        this.configuration = configuration;
        if (client == null) {
            this.defaultHttpClientHandler = defaultHttpClientHandler();
            this.client = new Client(defaultHttpClientHandler);
        } else {
            this.defaultHttpClientHandler = null;
            this.client = client;
        }
        channelId = UUID.randomUUID().toString();

        final ConnectionConfiguration connection = configuration.getConnection();
        serviceUrl = connection.isUseSSL() ? "https://" : "http://"
                + configuration.getConnection().getHost()
                + ':'
                + configuration.getConnection().getPort()
                + "/services/collector/raw";

        final BufferingConfiguration bufferingConfiguration = configuration.getBuffering();
        final Duration writeInterval = isNullOrEmpty(bufferingConfiguration.getWriteInterval()) ? null
                : Duration.duration(bufferingConfiguration.getWriteInterval());

        if (publisherFactory == null) {
            publisherFactory = new BatchPublisherFactoryImpl();
        }
        batchPublisher = publisherFactory.newBufferedPublisher(this)
                .capacity(bufferingConfiguration.getMaxSize())
                .writeInterval(writeInterval)
                .maxBatchEvents(bufferingConfiguration.getMaxBatchedEvents())
                .averagePerEventPayloadSize(BATCH_INDEX_AVERAGE_PER_EVENT_PAYLOAD_SIZE)
                .autoFlush(ALWAYS_FLUSH_BATCH_QUEUE)
                .build();
    }

    @Override
    public void startup() throws ResourceException {
        batchPublisher.startup();
    }

    @Override
    public void shutdown() throws ResourceException {
        batchPublisher.shutdown();
        if (defaultHttpClientHandler != null) {
            try {
                defaultHttpClientHandler.close();
            } catch (IOException e) {
                throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR,
                        "An error occurred while closing the default HTTP client handler", e);
            }
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context,
            final String topic, final JsonValue event) {
        final String resourceId = event.get(ResourceResponse.FIELD_CONTENT_ID).asString();

        if (!batchPublisher.offer(topic, event)) {
            return new ServiceUnavailableException(
                    "Splunk batch buffer full, dropping audit event " + topic + "/" + resourceId).asPromise();
        }

        return newResourceResponse(resourceId, null, event).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context,
            final String topic, final String resourceId) {
        return new NotSupportedException(
                "Read operations are currently not supported by the Splunk handler").asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context,
            final String topic, final QueryRequest query, final QueryResourceHandler handler) {
        return new NotSupportedException(
                "Query operations are currently not supported by the Splunk handler").asPromise();
    }

    @Override
    public void addToBatch(final String topic, final JsonValue event,
            final StringBuilder payload) throws BatchException {
        event.put("_topic", topic);

        try {
            final String eventJsonString = OBJECT_MAPPER.writeValueAsString(event.getObject());
            payload.append(eventJsonString).append('\n');
        } catch (final JsonProcessingException e) {
            throw new BatchException("Unable to parse event object to JSON", e);
        } finally {
            event.remove("_topic");
        }
    }

    @Override
    public Promise<Void, BatchException> publishBatch(final String payload) {
        final Request request = new Request();
        request.setMethod("POST");

        try {
            request.setUri(serviceUrl);
        } catch (URISyntaxException e) {
            return newExceptionPromise(new BatchException("Incorrect URI " + serviceUrl, e));
        }

        request.getHeaders().put(ContentTypeHeader.NAME, "application/json; charset=UTF-8");
        request.getHeaders().put("Authorization", "Splunk " + configuration.getAuthzToken());
        request.getHeaders().put("X-Splunk-Request-Channel", channelId);
        request.setEntity(payload);

        return client.send(request).then(
                closeSilently(new Function<Response, Void, BatchException>() {

                    @Override
                    public Void apply(final Response response) throws BatchException {
                        if (!response.getStatus().isSuccessful()) {
                            throw new BatchException("Publishing to Splunk failed: " + response.getEntity());
                        }

                        return null;
                    }

                }), Responses.<Void, BatchException>noopExceptionFunction());
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
