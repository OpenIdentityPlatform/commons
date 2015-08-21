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

import static org.forgerock.audit.util.ResourceExceptionsUtil.adapt;
import static org.forgerock.audit.util.ResourceExceptionsUtil.notSupported;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This audit service is the entry point for audit logging on the router.
 * <p>
 * The service should be configured and registered like this:
 * <pre>
 *  // create the service
 *  AuditService service = new AuditService(extentedTypes);
 *  // configure it
 *  service.configure(configuration);
 *  // set dependency provider
 *  service.registerDependencyProvider(provider);
 *  // register the handlers
 *  service.register(handler1, handler1Name, events1);
 *  service.register(handler2, handler2Name, events2);
 *  ...
 * </pre>
 *
 */
public class AuditService implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private static final ObjectMapper MAPPER;

    private static final JsonPointer SCHEMA_PROPERTIES_POINTER = new JsonPointer("/schema/properties");

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        MAPPER = new ObjectMapper(jsonFactory);
    }

    /** Existing active configuration. */
    private AuditServiceConfiguration config;

    /** All the AuditEventHandlers configured. */
    private Map<String, AuditEventHandler<?>> allAuditEventHandlers;
    /** All the AuditEventHandlers configured for each event type. */
    private Map<String, List<AuditEventHandler<?>>> eventTypeAuditEventHandlers;
    /** All the audit event types configured. */
    private Map<String, JsonValue> auditEvents;

    /** The dependency provider used by event handlers to satisfy dependencies. */
    private DependencyProvider dependencyProvider = new DependencyProviderBase();

    /** The name of the AuditEventHandler to use for queries. */
    private String queryHandlerName;

    /**
     * Constructs an AuditService with no extension for event types and no additional event types.
     */
    public AuditService() {
        this(new JsonValue(null), new JsonValue(null));
    }

    /**
     * Constructs an AuditService with extension of event types and additional event types.
     * <p>
     * The extension of the core event types is provided as a
     * Json value which can define additional properties.
     * The extension must not redefine a property already
     * defined in the core event types.
     * <p>
     * Example of a valid extension:
     * <pre>
     *   {
     *     "newProperty" : "value"
     *   }
     * </pre>
     *
     * @param extendedEventTypes the extension of the core event types.
     * @param customEventTypes the custom event types.
     */
    public AuditService(JsonValue extendedEventTypes, JsonValue customEventTypes) {
        eventTypeAuditEventHandlers = new HashMap<>();
        allAuditEventHandlers = new HashMap<>();

        auditEvents = new HashMap<>();
        readPredefinedEventTypes();
        extendEventTypes(extendedEventTypes);
        addCustomEventTypes(customEventTypes);
        auditEvents = Collections.unmodifiableMap(auditEvents);

        for (String eventName : auditEvents.keySet()) {
            eventTypeAuditEventHandlers.put(eventName, new ArrayList<AuditEventHandler<?>>());
        }

        config = new AuditServiceConfiguration();
    }

    /**
     * Configure the AuditService.
     *
     * @param configuration
     *            the configuration of the audit service.
     * @throws ResourceException
     *             if unable to configure audit service.
     */
    public void configure(final AuditServiceConfiguration configuration) throws ResourceException {
        cleanupPreviousConfig();
        queryHandlerName = configuration.getHandlerForQueries();
        config = new AuditServiceConfiguration(configuration);
    }

    /**
     * Register the DependencyProvider, after which, an AuditEventHandler can be registered and
     * receive this provider.  The dependency provider allows the handler to obtain resources or
     * objects from the product which integrates the Audit Service.
     *
     * @param provider
     *            the DependencyProvider to register
     */
    public void registerDependencyProvider(DependencyProvider provider) {
        Reject.ifNull(provider, "DependencyProvider must not be null");
        dependencyProvider = provider;
    }

    /**
     * Register an AuditEventHandler. After that registration, that AuditEventHandler can be referred with the given
     * name. This AuditEventHandler will only be notified about the events specified in the parameter events.
     *
     * @param handler
     *            the AuditEventHandler to register
     * @param name
     *            the name of the AuditEventHandler we want to register
     * @param events
     *            the events that this AuditEventHandler is interested.
     * @throws AuditException
     *             if there is already an AuditEventHandler register with the same name.
     */
    public void register(AuditEventHandler<?> handler, String name, Set<String> events) throws AuditException {
        if (!allAuditEventHandlers.containsKey(name)) {
            allAuditEventHandlers.put(name, handler);
        } else {
            throw new AuditException("There is already a handler registered for " + name);
        }

        logger.info("Registering {} with {} for {}", handler.getClass().getName(), name, events.toString());

        Map<String, JsonValue> auditEventsMetaData = new HashMap<>();
        for (String event : events) {
            if (!auditEvents.containsKey(event)) {
                logger.error("unknown event : {}", event);
                continue;
            }

            auditEventsMetaData.put(event, auditEvents.get(event));

            List<AuditEventHandler<?>> handlers = eventTypeAuditEventHandlers.get(event);
            // TODO Use a Set as we do not want duplicates.
            if (!handlers.contains(handler)) {
                handlers.add(handler);
            }
        }

        handler.setAuditEventsMetaData(auditEventsMetaData);
        handler.setDependencyProvider(dependencyProvider);
        logger.info("Registered {}", eventTypeAuditEventHandlers.toString());
    }

    private void cleanupPreviousConfig() {
        queryHandlerName = null;
    }

    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        try {

            logger.debug("Audit read called for {}", request.getResourcePath());
            if (queryHandlerName != null && allAuditEventHandlers.containsKey(queryHandlerName)) {
                final String id = request.getResourcePathObject().size() > 1
                        ? request.getResourcePathObject().tail(1).toString() : null;
                final String topic = parseTopicFromPath(request.getResourcePathObject());
                return allAuditEventHandlers.get(queryHandlerName).readEvent(topic, id);
            }
            String error = String.format(
                    "The handler defined for queries, '%s', has not been registered to the audit service.",
                    queryHandlerName);
            return adapt(new AuditException(error)).asPromise();
        } catch (Exception e) {
            return adapt(e).asPromise();
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        try {
            if (request.getResourcePath() == null) {
                throw new BadRequestException(
                        "Audit service called without specifying audit type in the identifier");
            }
            // Audit create called for /access with {timestamp=2013-07-30T18:10:03.773Z, principal=openidm-admin,
            // status=SUCCESS, roles=[openidm-admin, openidm-authorized], action=authenticate, userid=openidm-admin,
            // ip=127.0.0.1}
            logger.debug(
                    "Audit create called for {} with {}",
                    request.getResourcePath(),
                    request.getContent().asMap());

            // Don't audit the audit log
            if (context.containsContext(AuditContext.class)) {
                return newResourceResponse(null, null, request.getContent().copy()).asPromise();
            }

            // Generate an ID for the object
            final String localId = (request.getNewResourceId() == null || request.getNewResourceId().isEmpty())
                    ? UUID.randomUUID().toString()
                    : request.getNewResourceId();
            request.getContent().put(ResourceResponse.FIELD_CONTENT_ID, localId);
            logger.debug("Audit create id {}",
                         request.getContent().get(ResourceResponse.FIELD_CONTENT_ID));

            if (!request.getContent().isDefined("transactionId")
                    || !request.getContent().isDefined("timestamp")) {
                throw new BadRequestException("The request requires a transactionId and a timestamp");
            }

            final String topic = parseTopicFromPath(request.getResourcePathObject());
            if (!auditEvents.containsKey(topic)) {
                throw new NotSupportedException("Audit service called with unknown event type " + topic);
            }

            Collection<AuditEventHandler<?>> auditEventHandlersForEvent = getAuditEventHandlersForEvent(topic);
            logger.debug("Will cascade the event of type {} to the handlers : {}",
                        topic,
                        auditEventHandlersForEvent);

            ResourceResponse result = newResourceResponse(localId, null, new JsonValue(request.getContent()));
            Promise<ResourceResponse, ResourceException> promise = result.asPromise();
            // if the event is known but not registered with a handler, it's ok to ignore it
            if (auditEventHandlersForEvent.isEmpty()) {
                logger.debug("No handler found for the event of type {}", topic);
                return promise;
            }

            // Otherwise, let the event handlers set the response
            for (AuditEventHandler<?> auditEventHandler : auditEventHandlersForEvent) {
                promise = auditEventHandler.publishEvent(topic, request.getContent());
            }
            // TODO CAUD-24 last one wins!
            return promise;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return adapt(e).asPromise();
        }
    }

    private String parseTopicFromPath(final ResourcePath path) {
        return path.head(1).toString();
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(
            final Context context, final UpdateRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * Audit service currently does not support deleting audit entries.
     *
     * Deletes the specified object from the object set.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context, DeleteRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * The returned map is structured as follow:
     * <ul>
     * <li>The top level map contains meta-data about the query, plus an entry with the actual result records.
     * <li>The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            logger.debug("Audit query called for {}", request.getResourcePath());
            if (queryHandlerName != null && allAuditEventHandlers.containsKey(queryHandlerName)) {
                final String topic = parseTopicFromPath(request.getResourcePathObject());
                return getRegisteredHandler(queryHandlerName).queryEvents(topic, request, handler);
            }
            String error = String.format(
                    "The handler defined for queries, '%s', has not been registered to the audit service.",
                    queryHandlerName);
            return adapt(new AuditException(error)).asPromise();
        } catch (Exception e) {
            return adapt(e).asPromise();
        }
    }

    /**
     * Audit service does not support actions on audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        final String error = String.format("Unable to handle action: %s", request.getAction());
        logger.error(error);
        return new BadRequestException(error).asPromise();
    }

    private Collection<AuditEventHandler<?>> getAuditEventHandlersForEvent(final String auditEvent) {
        if (eventTypeAuditEventHandlers.containsKey(auditEvent)) {
            return eventTypeAuditEventHandlers.get(auditEvent);
        } else {
            return Collections.emptyList();
        }
    }

    private void addCustomEventTypes(JsonValue customEventTypes) {
        for (String eventTypeName : customEventTypes.keys()) {
            if (!auditEvents.containsKey(eventTypeName)) {
                auditEvents.put(eventTypeName, customEventTypes.get(eventTypeName));
            } else {
                logger.warn("Attempting to override a pre-defined event type : " + eventTypeName);
            }
        }
    }

    private void readPredefinedEventTypes() {
        try (final InputStream configStream = getClass().getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(MAPPER.readValue(configStream, Map.class));

            for (String eventTypeName : predefinedEventTypes.keys()) {
                auditEvents.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        } catch (IOException ioe) {
            logger.error("Error while parsing the events definition.", ioe);
            throw new RuntimeException(ioe);
        }
    }

    private void extendEventTypes(JsonValue extendedEventTypes) {
        for (String eventTypeName : extendedEventTypes.keys()) {
            if (auditEvents.containsKey(eventTypeName)) {
                JsonValue coreEventType = auditEvents.get(eventTypeName);
                JsonValue coreProperties = coreEventType.get(SCHEMA_PROPERTIES_POINTER);
                JsonValue extendedProperties = extendedEventTypes.get(eventTypeName).get(SCHEMA_PROPERTIES_POINTER);

                for (String property : extendedProperties.keys()) {
                    if (coreProperties.isDefined(property)) {
                        logger.warn("It is not allowed to override an existing property : {}", property);
                    } else {
                        coreProperties.add(property, extendedProperties.get(property));
                    }
                }
            }
        }
    }

    /**
     * Gets the AuditService configuration.
     *
     * @return the audit service config
     */
    public AuditServiceConfiguration getConfig() {
        return new AuditServiceConfiguration(config);
    }

    /**
     * Returns the registered handler corresponding to provided name.
     *
     * @param handlerName
     *            Name of the registered handler to retrieve.
     * @return the handler, or {@code null} if no handler with the provided name
     *         was registered to the service.
     */
    public AuditEventHandler<?> getRegisteredHandler(String handlerName) {
        return allAuditEventHandlers.get(handlerName);
    }

    /**
     * Returns whether or not events of the specified topic will be handled.
     *
     * @param topic Identifies a category of events to which handlers may or may not be registered.
     * @return whether handling of the specified topic is enabled.
     */
    public boolean isAuditing(String topic) {
        return !getAuditEventHandlersForEvent(topic).isEmpty();
    }

    /**
     * Returns the set of event topics (schemas) that the <code>AuditService</code> understands.
     *
     * @return The set of event topics.
     */
    public Set<String> getKnownTopics() {
        return auditEvents.keySet();
    }
}
