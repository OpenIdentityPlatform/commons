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

import static org.forgerock.util.promise.Promises.*;

import java.util.Map;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Abstract AuditEventHandler class.
 *
 * @param <CFG> type of the configuration
 */
public abstract class AuditEventHandlerBase<CFG> implements AuditEventHandler<CFG> {

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
    public void setDependencyProvider(DependencyProvider dependencyProvider) {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(
            final Context context,
            final String resourceId,
            final DeleteRequest request) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(
            final Context context,
            final String resourceId,
            final PatchRequest request) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(
            final Context context,
            final String resourceId,
            final UpdateRequest request) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Class<CFG> getConfigurationClass();

}
