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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.Collection;

import org.forgerock.http.Context;
import org.forgerock.json.fluent.JsonValue;

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
    public JsonValue action(final Context context, final ActionRequest request)
            throws ResourceException {
        return actionAsync(context, request).getOrThrowUninterruptibly();
    }

    @Override
    public Resource create(final Context context, final CreateRequest request)
            throws ResourceException {
        return createAsync(context, request).getOrThrowUninterruptibly();
    }

    @Override
    public Resource delete(final Context context, final DeleteRequest request)
            throws ResourceException {
        return deleteAsync(context, request).getOrThrowUninterruptibly();
    }

    @Override
    public Resource patch(final Context context, final PatchRequest request)
            throws ResourceException {
        return patchAsync(context, request).getOrThrowUninterruptibly();
    }

    @Override
    public QueryResult query(final Context context, final QueryRequest request,
            final QueryResultHandler handler) throws ResourceException {
        return queryAsync(context, request, handler).getOrThrowUninterruptibly();
    }

    @Override
    public QueryResult query(final Context context, final QueryRequest request,
            final Collection<? super Resource> results) throws ResourceException {
        return query(context, request, new QueryResultHandler() {

            @Override
            public void handleError(final ResourceException error) {
                // Ignore - handled by future.
            }

            @Override
            public boolean handleResource(final Resource resource) {
                results.add(resource);
                return true;
            }

            @Override
            public void handleResult(final QueryResult result) {
                // Ignore - handled by future.
            }
        });
    }

    @Override
    public Resource read(final Context context, final ReadRequest request) throws ResourceException {
        return readAsync(context, request).getOrThrowUninterruptibly();
    }

    @Override
    public Resource update(final Context context, final UpdateRequest request)
            throws ResourceException {
        return updateAsync(context, request).getOrThrowUninterruptibly();
    }
}
