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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.Collections;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * An event handler that does nothing.
 * <p>
 * The purpose of this handler is mainly to be able to assess performance of the Audit Service alone, without
 * the cost implied by the actual handlers.
 */
public class NoOpAuditEventHandler extends AuditEventHandlerBase {

    /**
     * Default constructor.
     */
    public NoOpAuditEventHandler() {
        super("NullAuditEventHandler",
                new EventTopicsMetaData(Collections.<String, JsonValue>emptyMap()), Collections.<String>emptySet(),
                        true);
    }

    @Override
    public void startup() throws ResourceException {
        // nothing to do
    }

    @Override
    public void shutdown() throws ResourceException {
        // nothing to do
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(Context context, String topic, JsonValue event) {
        return newResourceResponse(event.get(ResourceResponse.FIELD_CONTENT_ID).asString(), null, event).asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(Context context, String topic, QueryRequest query,
            QueryResourceHandler handler) {
        try {
            return newQueryResponse().asPromise();
        } catch (Exception e) {
            return new BadRequestException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(Context context, String topic, String resourceId) {
        return newResourceResponse(resourceId, null, json(object())).asPromise();
    }

}
