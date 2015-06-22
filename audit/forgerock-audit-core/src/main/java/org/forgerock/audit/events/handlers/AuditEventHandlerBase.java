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

import java.util.Map;

import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Abstract AuditEventHandler class.
 *
 * @param <T> type of the configuration
 */
public abstract class AuditEventHandlerBase<T extends AuditEventHandlerConfiguration> implements AuditEventHandler<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuditEventsMetaData(final Map<String, JsonValue> auditEvents) {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(
            final ServerContext context,
            final String resourceId,
            final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(
            final ServerContext context,
            final String resourceId,
            final PatchRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(
            final ServerContext context,
            final String resourceId,
            final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

}
