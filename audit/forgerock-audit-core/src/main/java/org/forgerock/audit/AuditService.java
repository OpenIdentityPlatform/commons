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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.audit.events.AuditEventHelper;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.events.handlers.AuditEventHandlerFactory;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
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
import org.forgerock.audit.util.DateUtil;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This audit service is the entry point for audit logging on the router.
 */
public class AuditService implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    /** The connection factory. */
    private ConnectionFactory connectionFactory;

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
    private static final String EVENT_TYPES = "eventTypes";
    private static final String USE_FOR_QUERIES = "useForQueries";

    /**
     * Constructs an AuditService.
     */
    public AuditService() {
        this(null);
    }

    /**
     * Constructs an AuditService with a cConnectionFactory.
     * @param connectionFactory  the ConnectionFactory to add to the AuditService
     */
    public AuditService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Configure the AuditService.
     * @param jsonConfig the config of the audit service.
     * @throws ResourceException if unable to configure audit service.
     */
    public void configure(final JsonValue jsonConfig) throws ResourceException {
        cleanupPreviousConfig();

        auditEvents = getEventTypes(jsonConfig);
        defaultAuditEventHandlers = getAuditEventHandlers(jsonConfig);
        eventTypeAuditEventHandlers = getEventTypeAuditEventHandlers(jsonConfig);
        queryAuditEventHandler = getQueryAuditEventHandler(jsonConfig.get(USE_FOR_QUERIES).asString());

        //set current config
        config = jsonConfig;
    }

    private void cleanupPreviousConfig() throws ResourceException {
        if (auditEvents != null) {
            auditEvents.clear();
            auditEvents = null;
        }
        // drop audit event handlers
        if (defaultAuditEventHandlers != null) {
            // notify the audit event handlers to close
            for (AuditEventHandler auditEventHandler : defaultAuditEventHandlers.values()) {
                auditEventHandler.close();
            }
            defaultAuditEventHandlers.clear();
            defaultAuditEventHandlers = null;
        }
        // event type audit event handlers are made up of the default event handlers,
        // so they are already notified of close. Drop event type audi event handlers
        if (eventTypeAuditEventHandlers != null) {
            eventTypeAuditEventHandlers.clear();
            eventTypeAuditEventHandlers = null;
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

            if (!request.getContent().isDefined("timestamp")) {
                logger.error("The request requires a timestamp");
                throw new BadRequestException("The request requires a timestamp");
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
     * @param auditEventName the name of the audit event to use for queries.
     * @return an AuditEventHandler to use for queries.
     * @throws ResourceException on failure to find an appropriate logger.
     */
    private AuditEventHandler getQueryAuditEventHandler(final String auditEventName) throws ResourceException {
        //return configured audit event handler
        if (auditEventName != null) {
            final AuditEventHandler auditEventHandler = defaultAuditEventHandlers.get(auditEventName);
            if (auditEventHandler != null) {
                return auditEventHandler;
            } else {
                logger.warn("The audit event doesn't exist with name: {}", auditEventName);
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

    private Map<String, JsonValue> getEventTypes(JsonValue config) {
        Map<String, JsonValue> listOfEventTypes = new HashMap<String, JsonValue>();

        JsonValue eventTypes = config.get(EVENT_TYPES);
        if (!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types
            for (String eventTypeKey : eventTypesKeys) {
                listOfEventTypes.put(eventTypeKey, eventTypes.get(eventTypeKey));
            }
        }

        return listOfEventTypes;
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
                auditEventHandlers.put(
                        eventHandlerKey,
                        AuditEventHandlerFactory.createAuditEventHandler(
                                eventHandlerKey, eventHandlers.get(eventHandlerKey), auditEvents, connectionFactory)
                );

            }
        }
        return auditEventHandlers;
    }


    private Map<String, List<AuditEventHandler>> getEventTypeAuditEventHandlers(JsonValue config) {
        Map<String, List<AuditEventHandler>> configuredAuditEventHandlers =
                new HashMap<String, List<AuditEventHandler>>();

        JsonValue eventTypes = config.get(EVENT_TYPES);
        if (!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types
            for (String eventTypeKey : eventTypesKeys) {
                final List<String> auditEventHandlersToLogTo =
                        AuditEventHelper.getConfiguredAuditEventHandlers(eventTypes.get(eventTypeKey));
                if (auditEventHandlersToLogTo == null || auditEventHandlersToLogTo.isEmpty()) {
                    continue;
                }
                final List<AuditEventHandler> auditEventHandlers = new ArrayList<AuditEventHandler>();
                for (String auditEventHandlerName : auditEventHandlersToLogTo) {
                    auditEventHandlers.add(this.defaultAuditEventHandlers.get(auditEventHandlerName));
                }
                configuredAuditEventHandlers.put(eventTypeKey, auditEventHandlers);
            }
        }

        return configuredAuditEventHandlers;
    }

    /**
     * Gets the AuditService config.
     * @return the audit service config as a JsonValue
     */
    public JsonValue getConfig() {
        return config.copy();
    }
}
