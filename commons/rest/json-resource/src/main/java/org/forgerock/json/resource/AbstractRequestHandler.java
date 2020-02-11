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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * An abstract base class from which request handlersmay be easily implemented.
 * The default implementation of each method is to return a
 * {@link NotSupportedException}.
 */
public abstract class AbstractRequestHandler implements RequestHandler {
    /**
     * Creates a new abstract request handler.
     */
    protected AbstractRequestHandler() {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context,
            final ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
            final PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
            final ReadRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }
}
