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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
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
 *  // register the handlers
 *  service.register(handler1, handler1Name, events1);
 *  service.register(handler2, handler2Name, events2);
 *  ...
 * </pre>
 *
 */
public class AuditService implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private static final ObjectMapper mapper;

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        mapper = new ObjectMapper(jsonFactory);
    }

    /** Existing active configuration. */
    private AuditServiceConfiguration config;

    /** All the AuditEventHandlers configured. */
    private Map<String, AuditEventHandler<?>> allAuditEventHandlers;
    /** All the AuditEventHandlers configured for each event type. */
    private Map<String, List<AuditEventHandler<?>>> eventTypeAuditEventHandlers;
    /** All the audit event types configured. */
    private Map<String, JsonValue> auditEvents;

    /** The name of the AuditEventHandler to use for queries. */
    private String queryHandlerName;

    /**
     * Constructs an AuditService with no extension for event types.
     */
    public AuditService() {
        this(new JsonValue(null));
    }

    /**
     * Constructs an AuditService with extension of event types.
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
     */
    public AuditService(JsonValue extendedEventTypes) {
        eventTypeAuditEventHandlers = new HashMap<>();
        allAuditEventHandlers = new HashMap<>();
        try(final InputStream configStream = getClass().getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue jsonConfig = new JsonValue(mapper.readValue(configStream, Map.class));
            auditEvents = getEventTypes(jsonConfig, extendedEventTypes);
        } catch (IOException ioe) {
            logger.error("Error while parsing the events definition.", ioe);
            throw new RuntimeException(ioe);
        }
        for (String event : auditEvents.keySet()) {
            eventTypeAuditEventHandlers.put(event, new ArrayList<AuditEventHandler<?>>());
        }
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
        queryHandlerName = configuration.getQueryHandlerName();
        config = new AuditServiceConfiguration(configuration);
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
        logger.info("Registered {}", eventTypeAuditEventHandlers.toString());
    }

    private void cleanupPreviousConfig() throws ResourceException {
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
    public void handleRead(final ServerContext context, final ReadRequest request,
                           final ResultHandler<Resource> handler) {
        try {

            logger.debug("Audit read called for {}", request.getResourceName());
            if (queryHandlerName != null && allAuditEventHandlers.containsKey(queryHandlerName)) {
                final String id = request.getResourceNameObject().size() > 1
                        ? request.getResourceNameObject().tail(1).toString() : null;
                allAuditEventHandlers.get(queryHandlerName).readInstance(context, id, request, handler);
                return;
            }
            handler.handleError(ResourceExceptionsUtil.adapt(new AuditException(String.format(
                    "The handler defined for queries, '%s', has not been registered to the audit service.",
                    queryHandlerName))));
        } catch (Throwable t) {
            handler.handleError(ResourceExceptionsUtil.adapt(t));
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
    public void handleCreate(final ServerContext context,
                             final CreateRequest request,
                             final ResultHandler<Resource> handler) {
        try {
            if (request.getResourceName() == null) {
                throw new BadRequestException(
                        "Audit service called without specifying which audit log in the identifier");
            }
            // Audit create called for /access with {timestamp=2013-07-30T18:10:03.773Z, principal=openidm-admin,
            // status=SUCCESS, roles=[openidm-admin, openidm-authorized], action=authenticate, userid=openidm-admin,
            // ip=127.0.0.1}
            logger.debug(
                    "Audit create called for {} with {}",
                    request.getResourceName(),
                    request.getContent().asMap());

            // Generate an ID for the object
            final String localId = (request.getNewResourceId() == null || request.getNewResourceId().isEmpty())
                    ? UUID.randomUUID().toString()
                    : request.getNewResourceId();
            request.getContent().put(Resource.FIELD_CONTENT_ID, localId);
            logger.debug("Audit create id {}",
                         request.getContent().get(Resource.FIELD_CONTENT_ID));

            if (!request.getContent().isDefined("transactionId")
                    || !request.getContent().isDefined("timestamp")) {
                String message = "The request requires a transactionId and a timestamp";
                logger.error(message);
                throw new BadRequestException(message);
            }

            // Don't audit the audit log
            if (context.containsContext(AuditContext.class)) {
                handler.handleResult(new Resource(null, null, request.getContent().copy()));
                return;
            }

            final String auditEventType = request.getResourceNameObject().head(1).toString();
            if (!auditEvents.containsKey(auditEventType)) {
                logger.warn("The AuditService is not aware about events of type {}", auditEventType);
                return;
            } else {
                Collection<AuditEventHandler<?>> auditEventHandlersForEvent;
                auditEventHandlersForEvent = getAuditEventHandlersForEvent(auditEventType);
                logger.info("Will cascade the event of type {} to the handlers : {}",
                            auditEventType,
                            auditEventHandlersForEvent);
                for (AuditEventHandler<?> auditEventHandler : auditEventHandlersForEvent) {
                    auditEventHandler.createInstance(context, request, handler);
                }
            }
        } catch (Throwable t) {
            // TODO Throwable might be a little bit too large ? (that also catches Error)
            // What about Exception | RuntimeException ?
            handler.handleError(ResourceExceptionsUtil.adapt(t));
        }
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Audit service currently does not support deleting audit entries.
     *
     * Deletes the specified object from the object set.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
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
    public void handleQuery(final ServerContext context, final QueryRequest request, final QueryResultHandler handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }
    /**
     * Audit service does not support actions on audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }


    private Collection<AuditEventHandler<?>> getAuditEventHandlersForEvent(final String auditEvent)
            throws InternalServerErrorException {
        if (eventTypeAuditEventHandlers.containsKey(auditEvent)) {
            return eventTypeAuditEventHandlers.get(auditEvent);
        } else {
            return Collections.<AuditEventHandler<?>>emptyList();
        }
    }

    private Map<String, JsonValue> getEventTypes(JsonValue coreEventTypes, JsonValue extendedEventTypes) {
        final JsonPointer schemaPropertiesPointer = new JsonPointer("/schema/properties");
        Map<String, JsonValue> listOfEventTypes = new HashMap<>(coreEventTypes.keys().size());

        for (String eventTypeKey : coreEventTypes.keys()) {
            JsonValue coreEventType = coreEventTypes.get(eventTypeKey);

            // Is there any extension provided ?
            if (extendedEventTypes.isDefined(eventTypeKey)) {
                JsonValue coreProperties = coreEventType.get(schemaPropertiesPointer);
                JsonValue extendedProperties = extendedEventTypes.get(eventTypeKey).get(schemaPropertiesPointer);

                for (String key : extendedProperties.keys()) {
                    if (coreEventType.isDefined(key)) {
                        logger.warn("It is not allowed to override an existing property : {}", key);
                    } else {
                        coreProperties.add(key, extendedProperties.get(key));
                    }
                }
            }

            listOfEventTypes.put(eventTypeKey, coreEventType);
        }

        return Collections.unmodifiableMap(listOfEventTypes);
    }

    /**
     * Gets the AuditService configuration.
     *
     * @return the audit service config
     */
    public AuditServiceConfiguration getConfig() {
        return new AuditServiceConfiguration(config);
    }
}
