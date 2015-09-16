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

import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

import java.util.Set;

/**
 * CREST {@link RequestHandler} responsible for storing and retrieving audit events.
 * <p/>
 * After construction, the AuditService will be in the 'STARTING' state until {@link #startup()} is called.
 * When in the 'STARTING' state, a call to any method other than {@link #startup()} will lead to
 * {@link ServiceUnavailableException}.
 * <p/>
 * After {@link #startup()} is called, assuming startup succeeds, the AuditService will then be in the
 * 'RUNNING' state and further calls to {@link #startup()} will be ignored.
 * <p/>
 * Calling {@link #shutdown()} will put the AuditService into the 'SHUTDOWN' state; once shutdown, the
 * AuditService will remain in this state and cannot be restarted. Further calls to {@link #shutdown()}
 * will be ignored. When in the 'SHUTDOWN' state, a call to any method other than {@link #shutdown()} will
 * lead to {@link ServiceUnavailableException}.
 */
public interface AuditService extends RequestHandler {

    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     * <p/>
     * If this {@code AuditService} has been closed, the returned promise will resolve to a
     * {@link ServiceUnavailableException}.
     *
     * {@inheritDoc}
     */
    @Override
    Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request);

    /**
     * Propagates the audit event to the {@link AuditEventHandler} objects that have been registered
     * for the audit event topic.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency).
     * <p/>
     * If this {@code AuditService} has been closed, the returned promise will resolve to a
     * {@link ServiceUnavailableException}.
     *
     * {@inheritDoc}
     */
    @Override
    Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request);

    /**
     * Audit service does not support changing audit entries.
     * <p/>
     * The returned promise will resolve to a {@link NotSupportedException}.
     */
    @Override
    Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request);

    /**
     * Audit service does not support changing audit entries.
     * <p/>
     * The returned promise will resolve to a {@link NotSupportedException}.
     */
    @Override
    Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request);

    /**
     * Audit service does not support changing audit entries.
     * <p/>
     * The returned promise will resolve to a {@link NotSupportedException}.
     */
    @Override
    Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request);

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
     * <p/>
     * If this {@code AuditService} has been closed, the returned promise will resolve to a
     * {@link ServiceUnavailableException}.
     *
     * {@inheritDoc}
     */
    @Override
    Promise<QueryResponse, ResourceException> handleQuery(
            Context context, QueryRequest request, QueryResourceHandler handler);

    /**
     * Audit service does not support actions on audit entries.
     * <p/>
     * The returned promise will resolve to a {@link NotSupportedException}.
     */
    @Override
    Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request);

    /**
     * Gets the AuditService configuration.
     *
     * @return the audit service config
     * @throws ServiceUnavailableException if the AuditService has been closed.
     */
    AuditServiceConfiguration getConfig() throws ServiceUnavailableException;

    /**
     * Returns the registered handler corresponding to provided name.
     *
     * @param handlerName
     *            Name of the registered handler to retrieve.
     * @return the handler, or {@code null} if no handler with the provided name
     *         was registered to the service.
     * @throws ServiceUnavailableException if the AuditService has been closed.
     */
    AuditEventHandler<?> getRegisteredHandler(String handlerName) throws ServiceUnavailableException;

    /**
     * Returns whether or not events of the specified topic will be handled.
     *
     * @param topic Identifies a category of events to which handlers may or may not be registered.
     * @return whether handling of the specified topic is enabled.
     * @throws ServiceUnavailableException if the AuditService has been closed.
     */
    boolean isAuditing(String topic) throws ServiceUnavailableException;

    /**
     * Returns the set of event topics (schemas) that the <code>AuditService</code> understands.
     *
     * @return The set of event topics.
     * @throws ServiceUnavailableException if the AuditService has been closed.
     */
    Set<String> getKnownTopics() throws ServiceUnavailableException;

    /**
     * Allows this {@code AuditService} and all its {@link AuditEventHandler}s to perform any initialization that
     * would be unsafe to do if any other instance of the {@code AuditService} were still running.
     */
    void startup() throws ServiceUnavailableException;

    /**
     * Closes this {@code AuditService} and all its {@link AuditEventHandler}s.
     * <p/>
     * This ensures that any buffered are flushed and all file handles / network connections are closed.
     * <p/>
     * Once {@code closed}, any further calls to this {@code AuditService} will throw, or return a promise
     * that will resolve to, {@link ServiceUnavailableException}.
     */
    void shutdown();
}
