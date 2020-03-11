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

package org.forgerock.audit.handlers.json;

import static org.forgerock.audit.util.JsonValueUtils.JSONVALUE_FILTER_VISITOR;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ElasticsearchUtil;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

/**
 * {@link AuditEventHandler} for persisting raw JSON events to a file.
 * <p>
 * The file format is a UTF-8 text-file, with one JSON event per line, and each line terminated by a newline character.
 */
public class JsonAuditEventHandler extends AuditEventHandlerBase {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Name of the {@code _eventId} JSON field.
     * <p>
     * When {@link #elasticsearchCompatible} is enabled, this handler renames the {@code _id} field to {@code _eventId},
     * because {@code _id} is reserved by ElasticSearch. The operation is reversed after JSON serialization, so that
     * other handlers will see the original field name.
     */
    static final String EVENT_ID_FIELD = "_eventId";

    /**
     * Name of action to force file rotation.
     */
    public static final String ROTATE_FILE_ACTION_NAME = "rotate";

    /**
     * Name of action to force flushing of file-buffer, which is not the same as flushing buffered audit events,
     * and is primarily used for testing purposes.
     */
    public static final String FLUSH_FILE_ACTION_NAME = "flush";

    private static final String ID_FIELD_PATTERN_PREFIX = "\"" + FIELD_CONTENT_ID + "\"\\s*:\\s*\"";
    private static final String EVENT_ID_FIELD_PATTERN_PREFIX = "\"" + EVENT_ID_FIELD + "\"\\s*:\\s*\"";
    private static final String FIELD_PATTERN_SUFFIX = "\"";

    private final JsonFileWriter jsonFileWriter;
    private final boolean elasticsearchCompatible;

    /**
     * Creates a {@code JsonAuditEventHandler} instances.
     *
     * @param configuration Configuration
     * @param eventTopicsMetaData Provides meta-data describing the audit event topics this handler may have to handle.
     */
    public JsonAuditEventHandler(
            final JsonAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        jsonFileWriter = new JsonFileWriter(configuration.getTopics(), configuration, true);
        elasticsearchCompatible = configuration.isElasticsearchCompatible();
    }

    @Override
    public void startup() throws ResourceException {
        jsonFileWriter.startup();
    }

    @Override
    public void shutdown() throws ResourceException {
        jsonFileWriter.shutdown();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String topic,
            final JsonValue event) {
        try {
            jsonFileWriter.put(topic, event);
        } catch (Exception e) {
            return newResourceException(INTERNAL_ERROR, "Failed to add event to queue", e).asPromise();
        }
        return newResourceResponse(event.get(FIELD_CONTENT_ID).asString(), null, event).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String topic,
            final String resourceId) {
        final Path jsonFilePath = jsonFileWriter.getTopicFilePath(topic);
        if (jsonFilePath == null) {
            return newResourceException(NOT_FOUND, "Topic not found: " + topic).asPromise();
        }
        final String fieldPatternPrefix = elasticsearchCompatible
                ? EVENT_ID_FIELD_PATTERN_PREFIX : ID_FIELD_PATTERN_PREFIX;
        final Matcher idMatcher = Pattern.compile(fieldPatternPrefix + resourceId + FIELD_PATTERN_SUFFIX).matcher("");
        String line;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(jsonFilePath), StandardCharsets.UTF_8))) {
            line = reader.readLine();
            while (line != null) {
                if (idMatcher.reset(line).find()) {
                    final JsonValue event = denormalizeJsonEvent(new JsonValue(
                            OBJECT_MAPPER.readValue(line, Map.class)));
                    return newResourceResponse(resourceId, null, event).asPromise();
                }
                line = reader.readLine();
            }
            return newResourceException(NOT_FOUND, "Resource not found with ID: " + resourceId).asPromise();
        } catch (Exception e) {
            return newResourceException(INTERNAL_ERROR, "Failed to read json file: " + jsonFilePath, e).asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String topic,
            final QueryRequest query, final QueryResourceHandler handler) {
        final Path jsonFilePath = jsonFileWriter.getTopicFilePath(topic);
        if (jsonFilePath == null) {
            return newResourceException(NOT_FOUND, "Topic not found: " + topic).asPromise();
        }
        final QueryFilter<JsonPointer> queryFilter = query.getQueryFilter();
        int results = 0;
        String line;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(jsonFilePath), StandardCharsets.UTF_8))) {
            line = reader.readLine();
            while (line != null) {
                final JsonValue event = denormalizeJsonEvent(new JsonValue(OBJECT_MAPPER.readValue(line, Map.class)));
                if (queryFilter.accept(JSONVALUE_FILTER_VISITOR, event)) {
                    ++results;
                    final ResourceResponse resourceResponse =
                            newResourceResponse(event.get(FIELD_CONTENT_ID).asString(), null, event);
                    if (!handler.handleResource(resourceResponse)) {
                        break;
                    }
                }
                line = reader.readLine();
            }
        } catch (Exception e) {
            return newResourceException(INTERNAL_ERROR, "Failed to read json file: " + jsonFilePath, e).asPromise();
        }
        return newQueryResponse(null, CountPolicy.EXACT, results).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final String topic,
            final ActionRequest request) {
        final Path jsonFilePath = jsonFileWriter.getTopicFilePath(topic);
        if (jsonFilePath == null) {
            return newResourceException(NOT_FOUND, "Topic not found: " + topic).asPromise();
        }
        try {
            switch (request.getAction()) {
            case ROTATE_FILE_ACTION_NAME:
                if (!jsonFileWriter.rotateFile(topic)) {
                    return newResourceException(BAD_REQUEST, "Rotation not enabled").asPromise();
                }
                break;
            case FLUSH_FILE_ACTION_NAME:
                jsonFileWriter.flushFileBuffer(topic);
                break;
            default:
                return newResourceException(BAD_REQUEST, "Unsupported action: " + request.getAction()).asPromise();
            }
            return newActionResponse(json(object(field("status", "OK")))).asPromise();
        } catch (Exception e) {
            return newResourceException(INTERNAL_ERROR, "Failed to invoke action", e).asPromise();
        }
    }

    /**
     * Reverses all ElasticSearch JSON normalization, if {@link #elasticsearchCompatible} is enabled.
     *
     * @param event Audit event
     * @return Audit event
     * @throws IOException Failure while processing JSON
     * @see JsonFileWriter#put(String, JsonValue)
     */
    private JsonValue denormalizeJsonEvent(JsonValue event) throws IOException {
        if (elasticsearchCompatible) {
            // reverse all ElasticSearch JSON normalization
            event = ElasticsearchUtil.denormalizeJson(event);
            ElasticsearchUtil.renameField(event, EVENT_ID_FIELD, FIELD_CONTENT_ID);
        }
        return event;
    }
}
