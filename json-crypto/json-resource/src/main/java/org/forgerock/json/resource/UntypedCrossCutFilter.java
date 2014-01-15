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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * An interface for implementing generic cross-cutting filters. Cross cutting
 * filters can be converted to {@link Filter}s by calling
 * {@link Filters#asFilter(UntypedCrossCutFilter)}.
 * <p>
 * The following example illustrates how an authorization filter could be
 * implemented:
 *
 * <pre>
 * public static final class AuthzFilter implements UntypedCrossCutFilter&lt;Void&gt; {
 *
 *     public void filterGenericRequest(final ServerContext context, final Request request,
 *             final RequestHandler next, final CrossCutFilterResultHandler&lt;Void, Object&gt; handler) {
 *         /*
 *          * Only forward the request if the request is allowed.
 *          &#42;/
 *         if (isAuthorized(context, request)) {
 *             /*
 *              * Continue processing the request since it is allowed.
 *              &#42;/
 *             handler.handleContinue(context, null);
 *         } else {
 *             /*
 *              * Stop processing the request since it is not allowed.
 *              &#42;/
 *             handler.handleError(new ForbiddenException());
 *         }
 *     }
 *
 *     public void filterGenericError(ServerContext context, Void state, ResourceException error,
 *             ResultHandler&lt;Object&gt; handler) {
 *         /*
 *          * Forward - assumes no authorization is required.
 *          &#42;/
 *         handler.handleError(error);
 *     }
 *
 *     public &lt;R&gt; void filterGenericResult(ServerContext context, Void state, R result,
 *             ResultHandler&lt;R&gt; handler) {
 *         /*
 *          * Filter the result if it is a resource.
 *          &#42;/
 *         if (result instanceof Resource) {
 *             if (isAuthorized(context, (Resource) result)) {
 *                 handler.handleResult(filterResource(context, result));
 *             } else {
 *                 handler.handleError(new NotFoundException());
 *             }
 *         } else {
 *             /*
 *              * Forward - assumes no authorization is required.
 *              &#42;/
 *             handler.handleResult(result);
 *         }
 *     }
 *
 *     public void filterQueryResource(ServerContext context, Void state, Resource resource,
 *             QueryResultHandler handler) {
 *         /*
 *          * Filter the resource.
 *          &#42;/
 *         if (isAuthorized(context, resource)) {
 *             handler.handleResource(filterResource(context, resource));
 *         }
 *     }
 * }
 * </pre>
 *
 * @param <C>
 *            The type of filter state to be maintained between request
 *            notification and response notification. Use {@code Void} if the
 *            filter is stateless and no filtering state information is
 *            required.
 * @see CrossCutFilter
 * @see Filters#asFilter(UntypedCrossCutFilter)
 */
public interface UntypedCrossCutFilter<C> {

    /**
     * Filters the provided error. Implementations may modify the provided error
     * and may choose to convert the error into a result. Once filtering has
     * completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     * <p>
     * <b>NOTE:</b> implementations which return a non-error result using
     * {@link CrossCutFilterResultHandler#handleResult(Object)
     * handler.handleResult(Object)} MUST take care to ensure that the result
     * has the correct type, i.e. {@code JsonValue} for actions,
     * {@code QueryResult} for queries, and {@code Resource} for all other
     * requests.
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
            ResultHandler<Object> handler);

    /**
     * Filters the provided request. Implementations may modify the provided
     * request and may invoke sub-requests using the provided request handler.
     * Once filtering has completed implementations must indicate whether or not
     * processing should continue by invoking one of the
     * {@code handler.handleXXX()} methods.
     * <p>
     * <b>NOTE:</b> implementations which stop processing and immediately return
     * a non-error result using
     * {@link CrossCutFilterResultHandler#handleResult(Object)
     * handler.handleResult(Object)} MUST take care to ensure that the result
     * has the correct type, i.e. {@code JsonValue} for actions,
     * {@code QueryResult} for queries, and {@code Resource} for all other
     * requests.
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
            CrossCutFilterResultHandler<C, Object> handler);

    /**
     * Filters the provided result. Implementations may modify the provided
     * result and may choose to convert the result into an error. Once filtering
     * has completed implementations must invoke one of the
     * {@code handler.handleXXX()} methods.
     *
     * @param <R>
     *            The type of result to be filtered.
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
    <R> void filterGenericResult(ServerContext context, C state, R result, ResultHandler<R> handler);

    /**
     * Filters the provided query resource response (see
     * {@link QueryResultHandler#handleResource(Resource)}). Implementations may
     * modify the provided resource. Once filtering has completed
     * implementations may do any of the following:
     * <ul>
     * <li>forward zero or more resources to the client by invoking
     * {@link QueryResultHandler#handleResource(Resource)
     * handler.handleResource} for each matching resource. Implementations will
     * typically invoke this once per resource, but may choose not to invoke it
     * at all if the resource is to be excluded from the query results, or
     * multiple times if, for example, the resource is to be decomposed into
     * multiple related resources,
     * <li>signal that no more resources will be returned to the client and that
     * an error should be sent instead by invoking
     * {@link QueryResultHandler#handleError(ResourceException)
     * handler.handleError},
     * <li>signal that no more resources will be returned to the client and that
     * a query result should be sent instead by invoking
     * {@link QueryResultHandler#handleResult(QueryResult) handler.handleResult}.
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

}
