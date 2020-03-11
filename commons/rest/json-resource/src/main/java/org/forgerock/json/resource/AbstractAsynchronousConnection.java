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

import java.util.Collection;

import org.forgerock.services.context.Context;

/**
 * An abstract connection whose synchronous methods are implemented in terms of
 * asynchronous methods.
 */
public abstract class AbstractAsynchronousConnection implements Connection {

    /**
     * Creates a new abstract asynchronous connection.
     */
    protected AbstractAsynchronousConnection() {
        // No implementation required.
    }

    @Override
    public ActionResponse action(final Context context, final ActionRequest request)
            throws ResourceException {
        try {
            return actionAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public ResourceResponse create(final Context context, final CreateRequest request)
            throws ResourceException {
        try {
            return createAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public ResourceResponse delete(final Context context, final DeleteRequest request)
            throws ResourceException {
        try {
            return deleteAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public ResourceResponse patch(final Context context, final PatchRequest request)
            throws ResourceException {
        try {
            return patchAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public QueryResponse query(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) throws ResourceException {
        try {
            return queryAsync(context, request, handler).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public QueryResponse query(final Context context, final QueryRequest request,
            final Collection<? super ResourceResponse> results) throws ResourceException {
        return query(context, request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse resource) {
                results.add(resource);
                return true;
            }
        });
    }

    @Override
    public ResourceResponse read(final Context context, final ReadRequest request) throws ResourceException {
        try {
            return readAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    @Override
    public ResourceResponse update(final Context context, final UpdateRequest request)
            throws ResourceException {
        try {
            return updateAsync(context, request).getOrThrow();
        } catch (InterruptedException e) {
            throw newTimeoutException(e);
        }
    }

    private ResourceException newTimeoutException(Exception cause) {
        return ResourceException.newResourceException(503, "Request was interrupted", cause);
    }
}
