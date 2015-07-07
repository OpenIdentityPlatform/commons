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

import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
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
    public Promise<JsonValue, ResourceException> handleAction(final ServerContext context,
            final ActionRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<Resource, ResourceException> handleCreate(final ServerContext context, final CreateRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<Resource, ResourceException> handleDelete(final ServerContext context, final DeleteRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<Resource, ResourceException> handlePatch(final ServerContext context, final PatchRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<QueryResult, ResourceException> handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResourceHandler handler) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<Resource, ResourceException> handleRead(final ServerContext context, final ReadRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation is to return a {@link NotSupportedException}.
     */
    @Override
    public Promise<Resource, ResourceException> handleUpdate(final ServerContext context, final UpdateRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }
}
