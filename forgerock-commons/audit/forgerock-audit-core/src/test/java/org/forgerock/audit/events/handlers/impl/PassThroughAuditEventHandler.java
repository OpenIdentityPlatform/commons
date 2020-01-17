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

package org.forgerock.audit.events.handlers.impl;

import static org.forgerock.json.resource.Responses.*;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

/**
 * Handles AuditEvents by just calling the result Handler.
 */
public class PassThroughAuditEventHandler extends AuditEventHandlerBase {

    /** A message logged when a new entry is added. */
    private String message;

    public PassThroughAuditEventHandler(
            final PassThroughAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.message = configuration.getMessage();
    }

    /** {@inheritDoc} */
    @Override
    public void startup() throws ResourceException {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() throws ResourceException {
        // nothing to do
    }

    /**
     * Create a audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {
        return newResourceResponse(
                        event.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                        null,
                        new JsonValue(event)).asPromise();
    }

    /**
     * Perform a query on the audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(
            Context context, String topic, QueryRequest query, QueryResourceHandler handler) {
        return newQueryResponse().asPromise();
    }

    /**
     * Read from the audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        return ResourceExceptionsUtil.adapt(
                new NotSupportedException("The " + RequestType.READ + " operation is not supported.")).asPromise();
    }

}
