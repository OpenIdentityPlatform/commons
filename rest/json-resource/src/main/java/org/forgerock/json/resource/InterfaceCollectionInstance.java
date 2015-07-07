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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Promise;

class InterfaceCollectionInstance implements RequestHandler {
    private final CollectionResourceProvider provider;

    InterfaceCollectionInstance(final CollectionResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(final ServerContext context, final ActionRequest request) {
        return provider.actionInstance(Resources.parentOf(context), Resources.idOf(context), request);
    }

    @Override
    public final Promise<Resource, ResourceException> handleCreate(final ServerContext context,
            final CreateRequest request) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource instance %s cannot be created", request.getResourcePath()));
    }

    @Override
    public Promise<Resource, ResourceException> handleDelete(final ServerContext context, final DeleteRequest request) {
        return provider.deleteInstance(Resources.parentOf(context), Resources.idOf(context), request);
    }

    @Override
    public Promise<Resource, ResourceException> handlePatch(final ServerContext context, final PatchRequest request) {
        return provider.patchInstance(Resources.parentOf(context), Resources.idOf(context), request);
    }

    @Override
    public final Promise<QueryResult, ResourceException> handleQuery(final ServerContext context,
            final QueryRequest request, QueryResourceHandler handler) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource instance %s cannot be queried", request.getResourcePath()));
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(final ServerContext context, final ReadRequest request) {
        return provider.readInstance(Resources.parentOf(context), Resources.idOf(context), request);
    }

    @Override
    public Promise<Resource, ResourceException> handleUpdate(final ServerContext context, final UpdateRequest request) {
        return provider.updateInstance(Resources.parentOf(context), Resources.idOf(context), request);
    }
}
