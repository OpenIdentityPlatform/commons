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

package org.forgerock.audit.events.handlers;

import java.util.List;
import java.util.Map;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

/**
 * The interface for an AuditEventHandler.
 *
 * @param <CFG> type of the configuration
 */
public interface AuditEventHandler<CFG extends EventHandlerConfiguration> {

    /**
     * Configures the Audit Event Handler with the provided configuration.
     *
     * @param config the configuration of the Audit Event Handler
     * @throws ResourceException if configuration fails
     */
    void configure(final CFG config) throws ResourceException;

    /**
     * Instruct this object to that it is safe to initialize file handles and network connections.
     *
     * @throws ResourceException if starting the AuditEventHandler fails
     */
    void startup() throws ResourceException;

    /**
     * Instruct this object to flush any buffers and close any open file handles or network connections.
     *
     * @throws ResourceException if closing the AuditEventHandler fails
     */
    void shutdown() throws ResourceException;

    /**
     * Set the audit events that this EventHandler may have to handle. This method is supposed to be called by the
     * AuditService when registering this AuditEventHandler.
     *
     * @param auditEvents
     *            List of AuditEvents to handle.
     */
    void setAuditEventsMetaData(Map<String, JsonValue> auditEvents);

    /**
     * Set the dependency provider to satisfy dependencies of this AuditEventHandler.
     *
     * @param dependencyProvider
     *            Dependency lookup abstraction for obtaining resources or objects from the
     *            product which integrates this AuditEventHandler.
     */
    void setDependencyProvider(DependencyProvider dependencyProvider);

    /**
     * Gets the configuration class for the audit event handler.
     * @return the configuration class for the audit event handler
     */
    Class<CFG> getConfigurationClass();

    /**
     * Publishes an event to the provided topic.
     *
     * @param context
     *          The context chain that initiated the event.
     * @param topic
     *          The topic where to publish the event.
     * @param event
     *          The event to publish.
     * @return a promise with either a response or an exception
     */
    Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event);

    /**
     * Publishes a list of events.
     *
     * @param events
     *          The list of (topic, event) pairs to publish.
     */
    void publishEvents(List<AuditEventTopicState> events);

    /**
     * Reads an event with the provided resource id from the provided topic.
     *
     * @param context
     *          The context chain that initiated the event.
     * @param topic
     *          The topic where event is read.
     * @param resourceId
     *          The identifier of the event.
     * @return a promise with either a response or an exception
     */
    Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId);

    /**
     * Query some events from the provided topic.
     *
     * @param context
     *          The context chain that initiated the event.
     * @param topic
     *          The topic on which query is performed.
     * @param query
     *          The request with the query.
     * @param handler
     *          The handler to process responses for the query.
     * @return a promise with either a response or an exception
     */
    Promise<QueryResponse, ResourceException> queryEvents(Context context, String topic, QueryRequest query,
            QueryResourceHandler handler);
}
