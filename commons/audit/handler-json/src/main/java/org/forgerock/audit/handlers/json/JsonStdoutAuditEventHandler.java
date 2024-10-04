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
 * Copyright 2023 3A Systems LLC
 */

package org.forgerock.audit.handlers.json;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ElasticsearchUtil;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import java.io.IOException;

import static org.forgerock.audit.util.ElasticsearchUtil.normalizeJson;
import static org.forgerock.audit.util.ElasticsearchUtil.renameField;
import static org.forgerock.audit.util.ResourceExceptionsUtil.notSupported;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newResourceResponse;

/**
 * {@link AuditEventHandler} for persisting raw JSON events to a file.
 * <p>
 * The file format is a UTF-8 text-file, with one JSON event per line, and each line terminated by a newline character.
 */
public class JsonStdoutAuditEventHandler extends AuditEventHandlerBase {

    /**
     * Name of the {@code _eventId} JSON field.
     * <p>
     * When {@link #elasticsearchCompatible} is enabled, this handler renames the {@code _id} field to {@code _eventId},
     * because {@code _id} is reserved by ElasticSearch. The operation is reversed after JSON serialization, so that
     * other handlers will see the original field name.
     */
    static final String EVENT_ID_FIELD = "_eventId";


    private final boolean elasticsearchCompatible;

    /**
     * Creates a {@code JsonAuditEventHandler} instances.
     *
     * @param configuration Configuration
     * @param eventTopicsMetaData Provides meta-data describing the audit event topics this handler may have to handle.
     */
    public JsonStdoutAuditEventHandler(
            final JsonStdoutAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
       elasticsearchCompatible = configuration.isElasticsearchCompatible();
    }

    @Override
    public void startup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String topic,
            final JsonValue event) {
        try {
            String eventStr = getEventAsString(topic, event);
            System.out.println(eventStr);
        } catch (Exception e) {
            return newResourceException(INTERNAL_ERROR, "Failed to add event to queue", e).asPromise();
        }
        return newResourceResponse(event.get(FIELD_CONTENT_ID).asString(), null, event).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String topic,
            final String resourceId) {
        return new NotSupportedException("query operations are not supported").asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest queryRequest, final QueryResourceHandler handler) {
        return notSupported(queryRequest).asPromise();
    }

    private String getEventAsString(String topic, JsonValue event) throws IOException {
        event.put("_topic", topic);
        final String eventStr;
        if (elasticsearchCompatible) {
            ElasticsearchUtil.renameField(event, FIELD_CONTENT_ID, EVENT_ID_FIELD);
            try {
                eventStr = normalizeJson(event);
            } finally {
                renameField(event, EVENT_ID_FIELD, FIELD_CONTENT_ID);
            }
        } else {
            eventStr = event.toString();
        }
        event.remove("_topic");
        return  eventStr;
    }
}
