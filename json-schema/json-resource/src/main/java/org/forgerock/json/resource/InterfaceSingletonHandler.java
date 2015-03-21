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

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;

class InterfaceSingletonHandler implements RequestHandler {
    private final SingletonResourceProvider provider;

    InterfaceSingletonHandler(final SingletonResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        provider.actionInstance(context, request, handler);
    }

    @Override
    public final void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(Resources.newBadRequestException(
                "The singleton resource %s cannot be created", request.getResourceName()));
    }

    @Override
    public final void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(Resources.newBadRequestException(
                "The singleton resource %s cannot be deleted", request.getResourceName()));
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        provider.patchInstance(context, request, handler);
    }

    @Override
    public final void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        // TODO: i18n
        handler.handleError(Resources.newBadRequestException(
                "The singleton resource %s cannot be queried", request.getResourceName()));
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        provider.readInstance(context, request, handler);
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        provider.updateInstance(context, request, handler);
    }
}
