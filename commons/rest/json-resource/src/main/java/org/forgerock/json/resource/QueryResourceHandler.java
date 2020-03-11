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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * A completion handler for consuming the results of a query request.
 * <p>
 * A query result completion handler may be specified when performing query
 * requests using a {@link Connection} object. The {@link #handleResource}
 * method is invoked for each resource which matches the query criteria,
 * followed by returning a {@link QueryResponse} or a {@link ResourceException}
 * indicating that no more JSON resources will be returned.
 * <p>
 * Implementations of these methods should complete in a timely manner so as to
 * avoid keeping the invoking thread from dispatching to other completion
 * handlers.
 * <p>
 * <b>Synchronization note:</b> each invocation of
 * {@link #handleResource(ResourceResponse) handleResource} for a resource <i><a href=
 * "http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility"
 * >happens-before</a></i> the invocation of {@code handleResource} for the next
 * resource. Invocation of {@code handleResource} for the final resource
 * <i>happens-before</i> returning a {@link QueryResponse} or a
 * {@link ResourceException} are invoked with the final query status. In other
 * words, query resource handler method invocations will occur sequentially and
 * one at a time.
 */
public interface QueryResourceHandler {

    /**
     * Invoked each time a matching JSON resource is returned from a query
     * request. More specifically, if a query request matches 10 resources, then
     * this method will be invoked 10 times, once for each matching resource.
     *
     * <p>Refer to
     * {@link RequestHandler#handleQuery(org.forgerock.services.context.Context, QueryRequest, QueryResourceHandler)}
     * for information regarding the concurrency and the order in which events
     * are processed.</p>
     *
     * @param resource
     *            The matching JSON resource.
     * @return {@code true} if this handler should continue to be notified of
     *         any remaining matching JSON resources, or {@code false} if the
     *         remaining JSON resources should be skipped for some reason (e.g.
     *         a client side size limit has been reached).
     */
    boolean handleResource(ResourceResponse resource);
}
