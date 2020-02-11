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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.events;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for {@link EventTopicsMetaData}.
 */
public final class EventTopicsMetaDataBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventTopicsMetaDataBuilder.class);
    private static final String SCHEMA = "schema";
    private static final String PROPERTIES = "properties";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonValue coreTopicSchemaExtensions = json(object());
    private JsonValue additionalTopicSchemas = json(object());

    private EventTopicsMetaDataBuilder() {
        // private to force use of static factory method
    }

    /**
     * Create a new instance of EventTopicsMetaDataBuilder that will populate {@link EventTopicsMetaData} objects its
     * creates with the schema meta-data for core topics.
     *
     * @return a new instance of EventTopicsMetaDataBuilder.
     */
    public static EventTopicsMetaDataBuilder coreTopicSchemas() {
        return new EventTopicsMetaDataBuilder();
    }

    /**
     * Specifies additional fields that should be added to the schemas for core event topics.
     * <p/>
     * The extension must not redefine a property already defined in the core event topics.
     * <p>
     * Example of a valid extension:
     * <pre>
     *  {
     *    "access": {
     *      "schema": {
     *        "$schema": "http://json-schema.org/draft-04/schema#",
     *        "id": "/",
     *        "type": "object",
     *        "properties": {
     *          "extraField": {
     *            "type": "string"
     *          }
     *        }
     *      }
     *    }
     *  }
     * </pre>
     *
     * @param coreTopicSchemaExtensions
     *          the extension of the core event topics.
     * @return this builder for method-chaining.
     */
    public EventTopicsMetaDataBuilder withCoreTopicSchemaExtensions(JsonValue coreTopicSchemaExtensions) {
        this.coreTopicSchemaExtensions = coreTopicSchemaExtensions == null ? json(object()) : coreTopicSchemaExtensions;
        return this;
    }

    /**
     * Specifies schemas for additional topics.
     * <p/>
     * Custom schema must always include _id, timestamp, transactionId and eventName fields.
     * <p/>
     * Example of a valid schema:
     * <pre>
     * "customTopic": {
     *   "schema": {
     *     "$schema": "http://json-schema.org/draft-04/schema#",
     *     "id": "/",
     *     "type": "object",
     *     "properties": {
     *       "_id": {
     *         "type": "string"
     *       },
     *       "timestamp": {
     *         "type": "string"
     *       },
     *       "transactionId": {
     *         "type": "string"
     *       },
     *       "eventName": {
     *         "type": "string"
     *       },
     *       "customField": {
     *         "type": "string"
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @param additionalTopicSchemas
     *          the schemas of the additional event topics.
     * @return this builder for method-chaining.
     */
    public EventTopicsMetaDataBuilder withAdditionalTopicSchemas(JsonValue additionalTopicSchemas) {
        this.additionalTopicSchemas = additionalTopicSchemas == null ? json(object()) : additionalTopicSchemas;
        return this;
    }

    /**
     * Create a new instance of {@link EventTopicsMetaData}.
     *
     * @return a new instance of {@link EventTopicsMetaData}.
     */
    public EventTopicsMetaData build() {
        Map<String, JsonValue> auditEventTopicSchemas = readCoreEventTopicSchemas();
        extendCoreEventTopicsSchemas(auditEventTopicSchemas);
        addCustomEventTopicSchemas(auditEventTopicSchemas);
        return new EventTopicsMetaData(auditEventTopicSchemas);
    }

    private Map<String, JsonValue> readCoreEventTopicSchemas() {
        Map<String, JsonValue> auditEvents = new HashMap<>();
        try (final InputStream configStream = getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(MAPPER.readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                auditEvents.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
            return auditEvents;
        } catch (IOException ioe) {
            logger.error("Error while parsing core event topic schema definitions", ioe);
            throw new RuntimeException(ioe);
        }
    }

    private InputStream getResourceAsStream(String resourcePath) {
        return new BufferedInputStream(getClass().getResourceAsStream(resourcePath));
    }

    private void extendCoreEventTopicsSchemas(Map<String, JsonValue> auditEventTopicSchemas) {
        for (String topic : coreTopicSchemaExtensions.keys()) {
            if (auditEventTopicSchemas.containsKey(topic)) {
                JsonValue coreEventType = auditEventTopicSchemas.get(topic);
                JsonValue coreProperties = coreEventType.get(SCHEMA).get(PROPERTIES);
                JsonValue extendedProperties = coreTopicSchemaExtensions.get(topic).get(SCHEMA).get(PROPERTIES);

                for (String property : extendedProperties.keys()) {
                    if (coreProperties.isDefined(property)) {
                        logger.warn("Cannot override {} property of {} topic", property, topic);
                    } else {
                        coreProperties.add(property, extendedProperties.get(property));
                    }
                }
            }
        }
    }

    private void addCustomEventTopicSchemas(Map<String, JsonValue> auditEventTopicSchemas) {
        for (String topic : additionalTopicSchemas.keys()) {
            if (!auditEventTopicSchemas.containsKey(topic)) {
                JsonValue additionalTopicSchema = additionalTopicSchemas.get(topic);
                if (!additionalTopicSchema.get(SCHEMA).isDefined(PROPERTIES)) {
                    logger.warn("{} topic schema definition is invalid", topic);
                } else {
                    auditEventTopicSchemas.put(topic, additionalTopicSchema);
                }
            } else {
                logger.warn("Cannot override pre-defined event topic {}", topic);
            }
        }
    }

}
