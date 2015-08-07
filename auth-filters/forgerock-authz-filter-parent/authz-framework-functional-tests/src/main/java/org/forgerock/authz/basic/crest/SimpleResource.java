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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.basic.crest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * Simple {@link CollectionResourceProvider} that just echos back the operation performed.
 *
 * @since 1.5.0
 */
public class SimpleResource implements CollectionResourceProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(ServerContext context, ActionRequest request) {
        return newResultPromise(newActionResponse(json(object(field("operation", "actionCollection")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(ServerContext context, String resourceId,
            ActionRequest request) {
        return newResultPromise(newActionResponse(json(object(field("operation", "action")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(ServerContext context, CreateRequest request) {
        return newResultPromise(newResourceResponse("0", "0", json(object(field("operation", "create")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(ServerContext context, String resourceId,
            DeleteRequest request) {
        return newResultPromise(newResourceResponse("0", "0", json(object(field("operation", "delete")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(ServerContext context, String resourceId,
            PatchRequest request) {
        return newResultPromise(newResourceResponse("0", "0", json(object(field("operation", "patch")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(ServerContext context, QueryRequest request,
            QueryResourceHandler handler) {
        handler.handleResource(newResourceResponse("0", "0", json(object(field("operation", "queryCollection")))));
        return newResultPromise(newQueryResponse());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(ServerContext context, String resourceId,
            ReadRequest request) {
        return newResultPromise(newResourceResponse("0", "0", json(object(field("operation", "read")))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(ServerContext context, String resourceId,
            UpdateRequest request) {
        return newResultPromise(newResourceResponse("0", "0", json(object(field("operation", "update")))));
    }
}
