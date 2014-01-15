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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

/**
 * A completion handler for consuming the results of a query request.
 * <p>
 * A query result completion handler may be specified when performing query
 * requests using a {@link Connection} object. The {@link #handleResource}
 * method is invoked for each resource which matches the query criteria,
 * followed by {@link #handleResult} or {@link #handleError} indicating that no
 * more JSON resources will be returned.
 * <p>
 * Implementations of these methods should complete in a timely manner so as to
 * avoid keeping the invoking thread from dispatching to other completion
 * handlers.
 * <p>
 * <b>Synchronization note:</b> each invocation of
 * {@link #handleResource(Resource) handleResource} for a resource <i><a href=
 * "http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility"
 * >happens-before</a></i> the invocation of {@code handleResource} for the next
 * resource. Invocation of {@code handleResource} for the final resource
 * <i>happens-before</i> either {@link #handleResult(QueryResult) handleResult}
 * or {@link #handleError(ResourceException) handleError} are invoked with the
 * final query status. In other words, query result handler method invocations
 * will occur sequentially and one at a time.
 */
public interface QueryResultHandler extends ResultHandler<QueryResult> {

    /**
     * Invoked when the query request has failed and no more matching resources
     * can been {@link #handleResource(Resource) returned}.
     *
     * @param error
     *            {@inheritDoc}
     */
    @Override
    void handleError(ResourceException error);

    /**
     * Invoked each time a matching JSON resource is returned from a query
     * request. More specifically, if a query request matches 10 resources, then
     * this method will be invoked 10 times, once for each matching resource.
     * Once all matching resources have been returned, either
     * {@link QueryResultHandler#handleResult(QueryResult)} will be invoked if
     * the query has completed successfully, or
     * {@link QueryResultHandler#handleError(ResourceException)} will be invoked
     * if the query did not complete successfully (even if some matching
     * resources were returned).
     *
     * @param resource
     *            The matching JSON resource.
     * @return {@code true} if this handler should continue to be notified of
     *         any remaining matching JSON resources, or {@code false} if the
     *         remaining JSON resources should be skipped for some reason (e.g.
     *         a client side size limit has been reached).
     */
    boolean handleResource(Resource resource);

    /**
     * Invoked when the query request has completed successfully and all
     * matching resources have been {@link #handleResource(Resource) returned}.
     *
     * @param result
     *            The query result indicating that no more resources are to be
     *            returned and, if applicable, including information which
     *            should be used for subsequent paged results query requests.
     */
    @Override
    void handleResult(QueryResult result);
}
