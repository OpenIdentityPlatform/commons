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

/**
 * An interface for implementing generic cross-cutting filters. Cross cutting
 * filters can be converted as normal {@link Filter} by calling
 * {@link Filters#asFilter}.
 *
 * @param <C>
 *            The type of filter context to be maintained between request
 *            notification and response notification. Use {@code Void} if the
 *            filter is stateless and no filtering context information is
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
     * {@link CrossCutFilterResultHandler#handleResult
     * handler.handleResult(Object)} MUST take care to ensure that the result
     * has the correct type, i.e. {@code JsonValue} for actions,
     * {@code QueryResult} for queries, and {@code Resource} for all other
     * requests.
     *
     * @param context
     *            The filter chain context.
     * @param filterContext
     *            The filter context which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue}.
     * @param error
     *            The error to be filtered.
     * @param handler
     *            The result handler which must be invoked once the error has
     *            been filtered.
     */
    void handleGenericError(ServerContext context, C filterContext, ResourceException error,
            ResultHandler<Object> handler);

    /**
     * Filters the provided request. Implementations may modify the provided
     * request and may invoke sub-requests using the provided request handler.
     * Once filtering has completed implementations must indicate whether or not
     * processing should continue by invoking one of the
     * {@code handler.handleXXX()} methods.
     * <p>
     * <b>NOTE:</b> implementations which stop processing and immediately return
     * a non-error result using {@link CrossCutFilterResultHandler#handleResult
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
    void handleGenericRequest(ServerContext context, Request request, RequestHandler next,
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
     * @param filterContext
     *            The filter context which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue}.
     * @param result
     *            The result to be filtered.
     * @param handler
     *            The result handler which must be invoked once the result has
     *            been filtered.
     */
    <R> void handleGenericResult(ServerContext context, C filterContext, R result,
            ResultHandler<R> handler);

    /**
     * Filters the provided query resource response (see
     * {@link QueryResultHandler#handleResource}). Implementations may modify
     * the provided resource. Once filtering has completed implementations must
     * invoke one of the {@code handler.handleXXX()} methods.
     *
     * @param context
     *            The filter chain context.
     * @param filterContext
     *            The filter context which was passed to
     *            {@link CrossCutFilterResultHandler#handleContinue}.
     * @param resource
     *            The resource to be filtered.
     * @param handler
     *            The result handler which must be invoked once the resource has
     *            been filtered.
     */
    void handleQueryResource(ServerContext context, C filterContext, Resource resource,
            ResultHandler<Resource> handler);

}
