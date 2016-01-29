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

import java.nio.charset.StandardCharsets;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandlerConfiguration.*;

/**
 * {@link AuditEventHandler} for Elasticsearch.
 */
public class ElasticsearchAuditEventHandler extends AuditEventHandlerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchAuditEventHandler.class);

    private final ElasticsearchAuditEventHandlerConfiguration configuration;
    private final Client client;

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
    }

    @Override
    public void startup() throws ResourceException {
        // do nothing
    }

    @Override
    public void shutdown() throws ResourceException {
        // do nothing
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
        // TODO
        return newResourceResponse(resourceId, null, null).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String topic,
            final JsonValue event) {
        try {
            // _id is a protected Elasticsearch field, so read it and remove it
            final String eventId = event.get("_id").asString();
            event.remove("_id");
            final String jsonPayload = ElasticsearchUtil.normalizeJson(event);

            final Request request = new Request();
            request.setMethod("PUT");
            request.setUri(buildEventUri(topic, eventId));
            if (jsonPayload != null) {
                request.getHeaders().put(ContentTypeHeader.NAME, "application/json; charset=UTF-8");
                request.getEntity().setBytes(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            final Response response = client.send(request).get();

            final Status status = response.getStatus();
            if (!status.isSuccessful()) {
                LOGGER.warn("Request failed: " + status);
            }
        } catch (Exception e) {
            final String error = String.format("Unable to create audit entry for %s", topic);
            LOGGER.error(error, e);
            return new InternalServerErrorException(error, e).asPromise();
        }
        return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
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
}
