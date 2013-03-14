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
 * <p>
 * On receipt of a request a filter implementation may either:
 * <ul>
 * <li><i>stop processing</i> the request and return a result or error
 * immediately. This is achieved by invoking the passed in result handler's
 * {@link ResultHandler#handleResult handleResult} or
 * {@link ResultHandler#handleError handleError} methods and returning
 * <li><i>continue processing</i> the request using the next filter in the
 * filter chain. This is achieved by invoking the appropriate {@code handlerXXX}
 * method on the passed in request handler. Implementations are permitted to
 * modify the context or request before forwarding. They may also wrap the
 * provided result handler in order to be notified when a response is returned,
 * allowing a filter to interact with responses before they are sent to the
 * client.
 * </ul>
 * Implementations are allowed to invoke arbitrary {@code handleXXX} methods on
 * the request handler if needed before deciding to stop or continue processing.
 * However, implementations should take care to ensure that the passed in result
 * handler is invoked at most once per request. This is useful in the case where
 * a filter implements some functionality as a composite of other operations
 * (e.g. theoretically, a password modify action could be intercepted within a
 * filter and converted into a read + update).
 * <p>
 * Filter chains are fully asynchronous: filters and request handlers may
 * delegate work to separate threads either directly (i.e. new Thread() ...) or
 * indirectly (e.g. via NIO completion handlers).
 * <p>
 * The following example illustrates how an authorization filter could be
 * implemented:
 *
 * <pre>
 * public class AuthzFilter implements Filter {
 *
 *     public void filterRead(final ServerContext context, final ReadRequest request,
 *                            final ResultHandler&lt;Resource&gt; handler, final RequestHandler next) {
 *         /*
 *          * Only forward the request if the request is allowed.
 *          &#42;/
 *         if (isAuthorized(context, request) {
 *             /*
 *              * Continue processing the request since it is allowed. Wrap the result handler
 *              * so that we can filter the returned resource.
 *              &#42;/
 *             next.handleRead(context, request, new ResultHandler&lt;Resource&gt;() {
 *                 public void handleResult(final Resource result) {
 *                     /*
 *                      * Filter the resource and its attributes.
 *                      &#42;/
 *                     if (isAuthorized(context, result)) {
 *                         handler.handleResult(filterResource(context, result));
 *                     } else {
 *                         handler.handleError(new NotFoundException(..));
 *                     }
 *                 }
 *
 *                 public void handleError(final ResourceException error) {
 *                     // Forward - assumes no authorization is required.
 *                     handler.handleError(error);
 *                 }
 *             });
 *         } else {
 *             /*
 *              * Stop processing the request since it is not allowed.
 *              &#42;/
 *             handler.handleError(new ForbiddenException(..));
 *         }
 *     }
 *
 *     // Remaining filterXXX methods...
 * }
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * </pre>
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
