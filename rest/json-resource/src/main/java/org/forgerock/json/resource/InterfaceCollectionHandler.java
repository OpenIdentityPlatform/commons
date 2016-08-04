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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.promise.Promises.*;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

class InterfaceCollectionHandler implements RequestHandler {
    private final CollectionResourceProvider provider;

    InterfaceCollectionHandler(final CollectionResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
            final ActionRequest request) {
        return provider.actionCollection(Resources.parentOf(context), request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        return provider.createInstance(Resources.parentOf(context), request);
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource collection %s cannot be deleted", request.getResourcePath()));
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
            final PatchRequest request) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource collection %s cannot be patched", request.getResourcePath()));
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return provider.queryCollection(Resources.parentOf(context), request, handler);
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handleRead(final Context context,
            final ReadRequest request) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource collection %s cannot be read", request.getResourcePath()));
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        // TODO: i18n
        return newExceptionPromise(Resources.newBadRequestException(
                "The resource collection %s cannot be updated", request.getResourcePath()));
    }

}
