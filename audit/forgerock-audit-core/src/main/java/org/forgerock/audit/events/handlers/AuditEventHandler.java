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

import org.forgerock.audit.util.ResourceUtil;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Abstract AuditEventHandler class.
 */
public abstract class AuditEventHandler implements CollectionResourceProvider {

    private boolean useForQueries = false;

    /**
     * Configures the Audit Event Handler with a config.
     * @param config the configuration of the Audit Event Handler
     */
    public abstract void configure(final JsonValue config);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void actionCollection(final ServerContext context, final ActionRequest request,
                                          final ResultHandler<JsonValue> handler);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void actionInstance(final ServerContext context, final String resourceId,
                                        final ActionRequest request, final ResultHandler<JsonValue> handler);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void createInstance(final ServerContext context, final CreateRequest request,
                                        final ResultHandler<Resource> handler);

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
                               final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
                              final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void queryCollection(final ServerContext context, final QueryRequest request,
                                         final QueryResultHandler handler);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void readInstance(final ServerContext context, final String resourceId, final ReadRequest request,
                                      final ResultHandler<Resource> handler);

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
                               final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Returns whether or not this event handler is used when querying an audit log entry.
     * @return true if this audit logger should be used; false otherwise.
     */
    public boolean isUsedForQueries() {
        return useForQueries;
    }

    /**
     * Sets this audit event handler to be used for queries.
     * @param value true if this audit logger should be used; false otherwise.
     */
    public void useForQueries(final boolean value) {
        this.useForQueries = value;
    }

}
