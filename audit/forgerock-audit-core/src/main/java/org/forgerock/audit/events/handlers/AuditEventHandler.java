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

package org.forgerock.audit.events.handlers;

import java.util.Set;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

/**
 * The interface for an AuditEventHandler.
 * <p/>
 * Implementations may make use of {@link org.forgerock.audit.DependencyProvider} to obtain dependencies from
 * the product within which the audit service is deployed. Where DependencyProvider is used, the dependencies
 * that it is expected to provide should be clearly documented.
 */
public interface AuditEventHandler {

    /**
     * Instruct this object that it is safe to initialize file handles and network connections.
     * <p/>
     * Reconfiguration of the {@link org.forgerock.audit.AuditService} and its handlers is achieved by replacing
     * rather than modifying the existing objects. Therefore, it's essential that the replacements do not perform
     * any I/O that would interfere with the operation of the objects they are replacing until the old objects are
     * shutdown. For example, when shutting down an old instance of a file-based AuditEventHandler, the old instance
     * may need to flush buffers, apply file rotation or retention policies, or even add line or block signatures
     * as part of tamper evident logging. Any of these operations could be broken if two handler instances are
     * operating on the same set of files simultaneously.
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
     * Gets the name of this audit event handler.
     * @return this handler's name.
     */
    String getName();

    /**
     * Gets the names of all audit event topics this handler is registered against.
     * @return the names of all topics handled by this object.
     */
    Set<String> getHandledTopics();

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

    /**
     * Checks if the audit event handler is enabled.
     * @return whether or not the audit event handler is enabled
     */
    boolean isEnabled();

    /**
     * Performs an action.
     *
     * @param context
     *          The context chain that initiated the event.
     * @param topic
     *          The topic on which action is performed.
     * @param request
     *          The request with the action.
     * @return a promise with either a response or an exception
     */
    Promise<ActionResponse, ResourceException> handleAction(Context context, String topic, ActionRequest request);
}
