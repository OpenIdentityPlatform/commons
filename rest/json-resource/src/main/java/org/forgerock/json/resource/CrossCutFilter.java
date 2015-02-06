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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;

/**
 * A strongly typed version of {@link UntypedCrossCutFilter}. Cross cutting
 * filters can be converted to {@link Filter}s by calling
 * {@link Filters#asFilter(CrossCutFilter)}.
 *
 * @param <C>
 *            The type of filter state to be maintained between request
 *            notification and response notification. Use {@code Void} if the
 *            filter is stateless and no filtering state information is
 *            required.
 * @see UntypedCrossCutFilter
 * @see Filters#asFilter(CrossCutFilter)
 */
public interface CrossCutFilter<C> {

    /**
     * Filters the provided action request error. Implementations may modify the
     * provided error and may choose to convert the error into a result. Once
     * filtering has completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param error
     *            The error to be filtered.
     * @param handler
     *            The result handler which must be invoked once the error has
     *            been filtered.
     */
    void filterActionError(ServerContext context, C state, ResourceException error,
            ResultHandler<JsonValue> handler);

    /**
     * Filters the provided action request. Implementations may modify the
     * provided request and may invoke sub-requests using the provided request
     * handler. Once filtering has completed implementations must indicate
     * whether or not processing should continue by invoking one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The request to be filtered.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @param handler
     *            The result handler which must be invoked once the request has
     *            been filtered.
     */
    void filterActionRequest(ServerContext context, ActionRequest request, RequestHandler next,
            CrossCutFilterResultHandler<C, JsonValue> handler);

    /**
     * Filters the provided action request result. Implementations may modify
     * the provided result and may choose to convert the result into an error.
     * Once filtering has completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param result
     *            The result to be filtered.
     * @param handler
     *            The result handler which must be invoked once the result has
     *            been filtered.
     */
    void filterActionResult(ServerContext context, C state, JsonValue result,
            ResultHandler<JsonValue> handler);

    /**
     * Filters the provided generic request error (create, delete, patch, read,
     * and update). Implementations may modify the provided error and may choose
     * to convert the error into a result. Once filtering has completed
     * implementations must invoke one of the {@code handler.handleXXX()}
     * methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param error
     *            The error to be filtered.
     * @param handler
     *            The result handler which must be invoked once the error has
     *            been filtered.
     */
    void filterGenericError(ServerContext context, C state, ResourceException error,
            ResultHandler<Resource> handler);

    /**
     * Filters the provided generic request (create, delete, patch, read, and
     * update). Implementations may modify the provided request and may invoke
     * sub-requests using the provided request handler. Once filtering has
     * completed implementations must indicate whether or not processing should
     * continue by invoking one of the {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The request to be filtered.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @param handler
     *            The result handler which must be invoked once the request has
     *            been filtered.
     */
    void filterGenericRequest(ServerContext context, Request request, RequestHandler next,
            CrossCutFilterResultHandler<C, Resource> handler);

    /**
     * Filters the provided generic request result (create, delete, patch, read,
     * and update). Implementations may modify the provided result and may
     * choose to convert the result into an error. Once filtering has completed
     * implementations must invoke one of the {@code handler.handleXXX()}
     * methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param result
     *            The result to be filtered.
     * @param handler
     *            The result handler which must be invoked once the result has
     *            been filtered.
     */
    void filterGenericResult(ServerContext context, C state, Resource result,
            ResultHandler<Resource> handler);

    /**
     * Filters the provided query request error. Implementations may modify the
     * provided error and may choose to convert the error into a result. Once
     * filtering has completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param error
     *            The error to be filtered.
     * @param handler
     *            The result handler which must be invoked once the error has
     *            been filtered.
     */
    void filterQueryError(ServerContext context, C state, ResourceException error,
            QueryResultHandler handler);

    /**
     * Filters the provided query request. Implementations may modify the
     * provided request and may invoke sub-requests using the provided request
     * handler. Once filtering has completed implementations must indicate
     * whether or not processing should continue by invoking one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The request to be filtered.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @param handler
     *            The result handler which must be invoked once the request has
     *            been filtered.
     */
    void filterQueryRequest(ServerContext context, QueryRequest request, RequestHandler next,
            CrossCutFilterResultHandler<C, QueryResult> handler);

    /**
     * Filters the provided query resource response (see
     * {@link QueryResultHandler#handleResource(Resource)}). Once filtering has
     * completed implementations may do any of the following:
     * <ul>
     * <li>forward zero or more resources to the client by invoking
     * {@link QueryResultHandler#handleResource handler.handleResource}.
     * Implementations will typically invoke this once per resource, but may
     * choose not to invoke it at all if the resource is to be excluded from the
     * query results, or multiple times if, for example, the resource is to be
     * decomposed into multiple related resources,
     * <li>signal that no more resources will be returned to the client and that
     * an error should be sent instead by invoking
     * {@link QueryResultHandler#handleError handler.handleError},
     * <li>signal that no more resources will be returned to the client and that
     * a query result should be sent instead by invoking
     * {@link QueryResultHandler#handleResult handler.handleResult}.
     * </ul>
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param resource
     *            The resource to be filtered.
     * @param handler
     *            The result handler which must be invoked once the resource has
     *            been filtered.
     */
    void filterQueryResource(ServerContext context, C state, Resource resource,
            QueryResultHandler handler);

    /**
     * Filters the provided query request result. Implementations may modify the
     * provided result and may choose to convert the result into an error. Once
     * filtering has completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param state
     *            The filter state which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue(ServerContext, Object)}
     *            .
     * @param result
     *            The result to be filtered.
     * @param handler
     *            The result handler which must be invoked once the result has
     *            been filtered.
     */
    void filterQueryResult(ServerContext context, C state, QueryResult result,
            QueryResultHandler handler);

}
