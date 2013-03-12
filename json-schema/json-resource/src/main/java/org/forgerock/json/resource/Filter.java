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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;

/**
 * An interface for implementing request handler filters. Filters are linked
 * together using a {@link FilterChain}.
 *
 * @see Filters
 */
public interface Filter {

    /**
     * Filters an action request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The action request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler, RequestHandler next);

    /**
     * Filters a create request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The create request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler, RequestHandler next);

    /**
     * Filters a delete request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The delete request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler, RequestHandler next);

    /**
     * Filters a patch request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The patch request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterPatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler,
            RequestHandler next);

    /**
     * Filters a query request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The query request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterQuery(ServerContext context, QueryRequest request, QueryResultHandler handler,
            RequestHandler next);

    /**
     * Filters a read request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The read request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler,
            RequestHandler next);

    /**
     * Filters an update request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The update request.
     * @param handler
     *            The result handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     */
    void filterUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler, RequestHandler next);

}
