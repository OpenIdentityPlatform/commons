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

import static org.forgerock.audit.events.handlers.AuditEventHandlerFactory.createAuditEventHandler;

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
 */
public class AuditService implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private static final ObjectMapper mapper;

    static {
        final JsonFactory jsonFactory = new JsonFactory();
        mapper = new ObjectMapper(jsonFactory);
    }

    private JsonValue config; // Existing active configuration

    /** All the AuditEventHandlers configured. */
    private Map<String, AuditEventHandler> defaultAuditEventHandlers;
    /** All the AuditEventHandlers configured for each event type. */
    private Map<String, List<AuditEventHandler>> eventTypeAuditEventHandlers;
    /** All the audit event types configured. */
    private Map<String, JsonValue> auditEvents;
    /** The name of the AuditEventHandler to use for queries. */
    private AuditEventHandler queryAuditEventHandler;

    private static final String EVENT_HANDLERS = "eventHandlers";
    private static final String USE_FOR_QUERIES = "useForQueries";

    /**
     * Constructs an AuditService.
     */
    public AuditService() {
        this(new JsonValue(null));
    }

    /**
     * Constructs an AuditService.
     *
     * @param extendedEventTypes the extension of the core event types.
     */
    public AuditService(JsonValue extendedEventTypes) {
        eventTypeAuditEventHandlers = new HashMap<>();
        try(final InputStream configStream = getClass().getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue jsonConfig = new JsonValue(mapper.readValue(configStream, Map.class));
            auditEvents = getEventTypes(jsonConfig, extendedEventTypes);
        } catch (IOException ioe) {
            logger.error("Error while parsing the events definition.", ioe);
            throw new RuntimeException(ioe);
        }
        for (String event : auditEvents.keySet()) {
            eventTypeAuditEventHandlers.put(event, new ArrayList<AuditEventHandler>());
        }
    }

    /**
     * Configure the AuditService.
     * @param jsonConfig the config of the audit service.
     * @throws ResourceException if unable to configure audit service.
     */
    public void configure(final JsonValue jsonConfig) throws ResourceException {
        cleanupPreviousConfig();

        defaultAuditEventHandlers = getAuditEventHandlers(jsonConfig);
        queryAuditEventHandler = getQueryAuditEventHandler(jsonConfig.get(USE_FOR_QUERIES).asString());

        //set current config
        config = jsonConfig;
    }

    private void register(AuditEventHandler handler, List<String> events) {
        for (String event : events) {
            if (!auditEvents.containsKey(event)) {
                logger.error("unknown event : {}", event);
                continue;
            }
            List<AuditEventHandler> handlers = eventTypeAuditEventHandlers.get(event);
            // TODO Use a Set as we do not want duplicates.
            if (!handlers.contains(handler)) {
                handlers.add(handler);
            }
        }
    }

    private void cleanupPreviousConfig() throws ResourceException {
        // drop audit event handlers
        if (defaultAuditEventHandlers != null) {
            // notify the audit event handlers to close
            for (AuditEventHandler auditEventHandler : defaultAuditEventHandlers.values()) {
                auditEventHandler.close();
            }
            defaultAuditEventHandlers.clear();
            defaultAuditEventHandlers = null;
        }

        for(Map.Entry<String, List<AuditEventHandler>> entry : eventTypeAuditEventHandlers.entrySet()) {
            entry.getValue().clear();
        }
        queryAuditEventHandler = null;
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
            final String id = request.getResourceNameObject().size() > 1
                    ? request.getResourceNameObject().tail(1).toString()
                    : null;

            logger.debug("Audit read called for {}", request.getResourceName());
            queryAuditEventHandler.readInstance(context, id, request, handler);
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
    public void handleCreate(final ServerContext context, final CreateRequest request,
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
            for (AuditEventHandler auditEventHandler : getAuditEventHandlersForEvent(auditEventType)) {
                auditEventHandler.createInstance(context, request, handler);
            }
        } catch (Throwable t) {
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
        try {
            logger.debug(
                    "Audit query called for {} with {}",
                    request.getResourceName(),
                    request.getAdditionalParameters());
            queryAuditEventHandler.queryCollection(context, request, handler);
        } catch (Throwable t) {
            handler.handleError(ResourceExceptionsUtil.adapt(t));
        }
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


    private Collection<AuditEventHandler> getAuditEventHandlersForEvent(final String auditEvent)
            throws InternalServerErrorException {
        if (eventTypeAuditEventHandlers == null && defaultAuditEventHandlers == null) {
            throw new InternalServerErrorException("No audit event type handlers were configured");
        }

        if (eventTypeAuditEventHandlers == null || eventTypeAuditEventHandlers.get(auditEvent) == null) {
            return defaultAuditEventHandlers.values();
        } else {
            return eventTypeAuditEventHandlers.get(auditEvent);
        }
    }

    /**
     * Returns the AuditEventHandler to use for reads/queries.
     *
     * @param auditEventHandlerKey the name of the audit event handler to use for queries.
     * @return an AuditEventHandler to use for queries.
     * @throws ResourceException on failure to find an appropriate logger.
     */
    private AuditEventHandler getQueryAuditEventHandler(final String auditEventHandlerKey) throws ResourceException {
        //return configured audit event handler
        if (auditEventHandlerKey != null) {
            final AuditEventHandler auditEventHandler = defaultAuditEventHandlers.get(auditEventHandlerKey);
            if (auditEventHandler != null) {
                return auditEventHandler;
            } else {
                logger.warn("The audit event handler doesn't exist with name: {}", auditEventHandlerKey);
            }
        }

        if (defaultAuditEventHandlers != null
                && !defaultAuditEventHandlers.isEmpty()) {
            //return first global audit event handler
            return defaultAuditEventHandlers.values().iterator().next();
        } else {
            throw new InternalServerErrorException("No audit event handlers configured to be queried.");
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

    private Map<String, AuditEventHandler> getAuditEventHandlers(JsonValue config) throws ResourceException {
        Map<String, AuditEventHandler> auditEventHandlers = new HashMap<String, AuditEventHandler>();
        JsonValue eventHandlers = config.get(EVENT_HANDLERS);
        if (!eventHandlers.isNull()) {
            Set<String> eventHandlersKeys = eventHandlers.keys();
            if (eventHandlersKeys.isEmpty()) {
                logger.error("No audit event handlers configured");
                throw new InternalServerErrorException("No audit event handlers configured");
            }
            // Loop through event handlers
            for (String eventHandlerKey : eventHandlersKeys) {
                JsonValue handlerDefinition = eventHandlers.get(eventHandlerKey);
                AuditEventHandler handler = createAuditEventHandler(eventHandlerKey,
                                                                    handlerDefinition.get("config"),
                                                                    auditEvents);
                auditEventHandlers.put(eventHandlerKey, handler);

                register(handler, handlerDefinition.get("events").asList(String.class));
            }
        }
        return auditEventHandlers;
    }

    /**
     * Gets the AuditService config.
     * @return the audit service config as a JsonValue
     */
    public JsonValue getConfig() {
        return config.copy();
    }
}
