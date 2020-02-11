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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * An interface for implementing request handler filters. Filters are linked
 * together using a {@link FilterChain}.
 * <p>
 * On receipt of a request a filter implementation may either:
 * <ul>
 * <li><i>stop processing</i> the request and return a result or error
 * immediately. This is achieved by returning a completed {@code Promise} with
 * a {@link QueryResponse} or a {@link ResourceException} methods and returning
 * <li><i>continue processing</i> the request using the next filter in the
 * filter chain. This is achieved by invoking the appropriate {@code handlerXXX}
 * method on the passed in request handler. Implementations are permitted to
 * modify the context or request before forwarding. They may also chain the
 * promise, returned from the downstream handler, in order to be notified when
 * a response is returned, allowing a filter to interact with responses before
 * they are sent to the client.
 * </ul>
 * <p>
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
 *     public Promise&lt;Resource, ResourceException&gt; filterRead(final Context context,
 *             final ReadRequest request, final RequestHandler next) {
 *         /*
 *          * Only forward the request if the request is allowed.
 *          &#42;/
 *         if (isAuthorized(context, request)) {
 *             /*
 *              * Continue processing the request since it is allowed. Chain the
 *              * promise so that we can filter the returned resource.
 *              &#42;/
 *             return next.handleRead(context, request)
 *                     .thenAsync(new AsyncFunction&lt;Resource, Resource, ResourceException&gt;() {
 *                         &#064;Override
 *                         public Promise&lt;Resource, ResourceException&gt; apply(Resource result) {
 *                             /*
 *                              * Filter the resource and its attributes.
 *                              &#42;/
 *                             if (isAuthorized(context, result)) {
 *                                 return Promises.newResultPromise(filterResource(context, result));
 *                             } else {
 *                                 return newExceptionPromise(ResourceException.newNotFoundException());
 *                             }
 *                         }
 *                     }, new AsyncFunction&lt;ResourceException, Resource, ResourceException&gt;() {
 *                         &#064;Override
 *                         public Promise&lt;Resource, ResourceException&gt; apply(ResourceException error) {
 *                             // Forward - assumes no authorization is required.
 *                             return newExceptionPromise(error);
 *                         }
 *                     });
 *         } else {
 *             /*
 *              * Stop processing the request since it is not allowed.
 *              &#42;/
 *             ResourceException exception = new ForbiddenException();
 *             return newExceptionPromise(exception);
 *         }
 *     }
 *
 *     // Remaining filterXXX methods...
 * }
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
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next);

    /**
     * Filters a create request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The create request.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next);

    /**
     * Filters a delete request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The delete request.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next);

    /**
     * Filters a patch request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The patch request.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next);

    /**
     * Filters a query request.
     * <p>
     * Implementations which return results directly rather than forwarding the
     * request should invoke {@link QueryResourceHandler#handleResource(ResourceResponse)}
     * for each resource which matches the query criteria. Once all matching
     * resources have been returned implementations are required to return
     * either a {@link QueryResponse} if the query has completed successfully, or
     * {@link ResourceException} if the query did not complete successfully
     * (even if some matching resources were returned).
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The query request.
     * @param handler
     *            The resource handler.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next);

    /**
     * Filters a read request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The read request.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next);

    /**
     * Filters an update request.
     *
     * @param context
     *            The filter chain context.
     * @param request
     *            The update request.
     * @param next
     *            A request handler representing the remainder of the filter
     *            chain.
     * @return A {@code Promise} containing the result of the operation.
     */
    Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next);
}
