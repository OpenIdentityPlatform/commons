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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.audit;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builder for AuditService.
 */
public final class AuditServiceBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceBuilder.class);
    private static final String SCHEMA = "schema";
    private static final String PROPERTIES = "properties";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuditServiceFactory auditServiceFactory;
    private AuditServiceConfiguration auditServiceConfiguration = new AuditServiceConfiguration();
    private JsonValue coreTopicSchemaExtensions = json(object());
    private JsonValue additionalTopicSchemas = json(object());
    private DependencyProvider dependencyProvider = new DependencyProviderBase();
    private Map<String, HandlerRegistration> handlerRegistrations = new LinkedHashMap<>();

    @VisibleForTesting
    AuditServiceBuilder(AuditServiceFactory auditServiceFactory) {
        this.auditServiceFactory = auditServiceFactory;
    }

    /**
     * Factory method for new instances of this builder.
     *
     * @return A new instance of the AuditServiceBuilder.
     */
    public static AuditServiceBuilder newAuditService() {
        return new AuditServiceBuilder(new AuditServiceFactory());
    }

    /**
     * Sets the AuditServiceConfiguration that is to be passed to the AuditService.
     * <p/>
     * AuditServiceConfiguration embodies the configuration state that can be set by system administrators.
     *
     * @param auditServiceConfiguration
     *          user-facing configuration that is to be applied to the AuditService.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withConfiguration(AuditServiceConfiguration auditServiceConfiguration) {
        Reject.ifNull(auditServiceConfiguration, "Audit service configuration cannot be null");
        this.auditServiceConfiguration = auditServiceConfiguration;
        return this;
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
    public AuditServiceBuilder withCoreTopicSchemaExtensions(JsonValue coreTopicSchemaExtensions) {
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
    public AuditServiceBuilder withAdditionalTopicSchemas(JsonValue additionalTopicSchemas) {
        this.additionalTopicSchemas = additionalTopicSchemas == null ? json(object()) : additionalTopicSchemas;;
        return this;
    }

    /**
     * Register the DependencyProvider, after which, an AuditEventHandler can be registered and
     * receive this provider.  The dependency provider allows the handler to obtain resources or
     * objects from the product which integrates the Audit Service.
     *
     * @param dependencyProvider
     *            the DependencyProvider to register.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withDependencyProvider(DependencyProvider dependencyProvider) {
        Reject.ifNull(dependencyProvider, "Audit event handler DependencyProvider cannot be null");
        this.dependencyProvider = dependencyProvider;
        return this;
    }

    /**
     * Register an AuditEventHandler. After that registration, that AuditEventHandler can be referred with the given
     * name. This AuditEventHandler will only be notified about the events specified in the parameter events.
     *
     * @param handler
     *            the AuditEventHandler to register.
     * @param name
     *            the name of the handler we want to register.
     * @param topics
     *            the event topics to which the handler should subscribe.
     * @throws AuditException
     *             if already asked to register a handler with the same name.
     * @return this builder for method-chaining.
     */
    public AuditServiceBuilder withAuditEventHandler(AuditEventHandler<?> handler, String name, Set<String> topics)
            throws AuditException {

        Reject.ifNull(handler, "Audit event handler cannot be null");
        Reject.ifNull(name, "Audit event handler name cannot be null");

        if (handlerRegistrations.containsKey(name)) {
            throw new AuditException("There is already a handler registered for " + name);
        }
        handlerRegistrations.put(name, new HandlerRegistration(handler, name, topics));
        return this;
    }

    /**
     * Creates a new AuditService instance.
     * <p/>
     * Instances receive their configuration when constructed and cannot be reconfigured. Where "hot-swappable"
     * reconfiguration is required, an instance of {@link AuditServiceProxy} should be used as a proxy. The old
     * AuditService should fully shutdown before the new instance is started. Care must be taken to ensure that
     * no other threads can interact with this object while {@link AuditService#startup()} and
     * {@link AuditService#shutdown()} methods are running.
     * <p/>
     * After construction, the AuditService will be in the 'STARTING' state until {@link AuditService#startup()}
     * is called. When in the 'STARTING' state, a call to any method other than {@link AuditService#startup()}
     * will lead to {@link ServiceUnavailableException}.
     * <p/>
     * After {@link AuditService#startup()} is called, assuming startup succeeds, the AuditService will then be in
     * the 'RUNNING' state and further calls to {@link AuditService#startup()} will be ignored.
     * <p/>
     * Calling {@link AuditService#shutdown()} will put the AuditService into the 'SHUTDOWN' state; once shutdown, the
     * AuditService will remain in this state and cannot be restarted. Further calls to {@link AuditService#shutdown()}
     * will be ignored. When in the 'SHUTDOWN' state, a call to any method other than {@link AuditService#shutdown()}
     * will lead to {@link ServiceUnavailableException}.
     * <p/>
     * When instances are no longer needed, {@link AuditService#shutdown()} should be called to ensure that any
     * buffered audit events are flushed and that all open file handles or connections are closed.
     *
     * @return a new AuditService instance.
     */
    public AuditService build() {
        Map<String, JsonValue> auditEventTopicSchemas = getAuditEventTopicSchemas();
        return auditServiceFactory.newAuditService(
                auditServiceConfiguration,
                auditEventTopicSchemas,
                getAuditEventHandlersByName(),
                getAuditEventHandlersByTopic(auditEventTopicSchemas));
    }

    private Map<String, JsonValue> getAuditEventTopicSchemas() {
        Map<String, JsonValue> auditEventTopicSchemas = readCoreEventTopicSchemas();
        extendCoreEventTopicsSchemas(auditEventTopicSchemas);
        addCustomEventTopicSchemas(auditEventTopicSchemas);
        return auditEventTopicSchemas;
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

    /**
     * Generate lists of AuditEventHandlers that should receive events for each topic.
     * <p/>
     * NB. As a side-effect of calling this method, handlers get topic schemas and dependency provider.
     */
    private Map<String, Set<AuditEventHandler<?>>> getAuditEventHandlersByTopic(
            Map<String, JsonValue> auditEventTopicSchemas) {

        Map<String, Set<AuditEventHandler<?>>> handlersByTopic = new HashMap<>();

        for (String topic : auditEventTopicSchemas.keySet()) {
            // Use a LinkedHashSet so that iteration order follows order in which handlers were defined
            handlersByTopic.put(topic, new LinkedHashSet<AuditEventHandler<?>>());
        }

        for (HandlerRegistration handlerRegistration : handlerRegistrations.values()) {
            final AuditEventHandler<?> handler = handlerRegistration.handler;

            logger.info("Registering {} handler '{}' for {} topics", handlerRegistration.handler.getClass().getName(),
                    handlerRegistration.name, handlerRegistration.topics.toString());

            Map<String, JsonValue> filteredAuditEventTopicSchemas = new HashMap<>();
            for (String topic : handlerRegistration.topics) {
                if (!auditEventTopicSchemas.containsKey(topic)) {
                    logger.error("unknown audit event topic : {}", topic);
                    continue;
                }
                filteredAuditEventTopicSchemas.put(topic, auditEventTopicSchemas.get(topic));
                handlersByTopic.get(topic).add(handler);
            }
            handler.setAuditEventsMetaData(filteredAuditEventTopicSchemas);
            handler.setDependencyProvider(dependencyProvider);
        }

        logger.info("Registered {}", handlersByTopic.toString());

        return handlersByTopic;
    }

    private Map<String, AuditEventHandler<?>> getAuditEventHandlersByName() {
        Map<String, AuditEventHandler<?>> handlersByName = new HashMap<>();
        for (HandlerRegistration handlerRegistration : handlerRegistrations.values()) {
            handlersByName.put(handlerRegistration.name, handlerRegistration.handler);
        }
        return handlersByName;
    }

    /**
     * Captures details of a handler registration request.
     * <p/>
     * Calls to {@link AuditServiceBuilder#withAuditEventHandler} are lazily-processed when
     * {@link AuditServiceBuilder#build()} is called so that all event topic schema meta-data
     * is available for validation of the mapping from topics to handlers without constraining
     * the order in which the builder's methods should be called.
     */
    private static class HandlerRegistration {

        private final AuditEventHandler<?> handler;
        private final String name;
        private final Set<String> topics;

        private HandlerRegistration(AuditEventHandler<?> handler, String name, Set<String> topics) {
            this.handler = handler;
            this.name = name;
            this.topics = topics == null
                    ? Collections.<String>emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(topics));
        }
    }

    /**
     * This class exists solely to provide a 'seam' that can be mocked during unit testing.
     */
    @VisibleForTesting
    static class AuditServiceFactory {

        AuditService newAuditService(
                final AuditServiceConfiguration configuration,
                final Map<String, JsonValue> auditEventTopicSchemas,
                final Map<String, AuditEventHandler<?>> auditEventHandlersByName,
                final Map<String, Set<AuditEventHandler<?>>> auditEventHandlersByTopic) {

            return new AuditServiceImpl(
                    configuration, auditEventTopicSchemas, auditEventHandlersByName, auditEventHandlersByTopic);
        }
    }
}
