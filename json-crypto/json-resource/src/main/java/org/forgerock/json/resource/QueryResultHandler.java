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
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;


/**
 * A completion handler for consuming the results of a query request.
 * <p>
 * A query result completion handler may be specified when performing query
 * requests using a {@link Connection} object. The {@link #handleResource}
 * method is invoked each time a matching JSON resource is returned, followed by
 * {@link #handleResult} or {@link #handleError} indicating that no more JSON
 * resources will be returned.
 * <p>
 * Implementations of these methods should complete in a timely manner so as to
 * avoid keeping the invoking thread from dispatching to other completion
 * handlers.
 */
public interface QueryResultHandler extends ResultHandler<QueryResult> {

    /**
     * {@inheritDoc}
     */
    @Override
    void handleError(ResourceException error);

    /**
     * Invoked each time a matching JSON resource is returned from a query
     * request.
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
     * {@inheritDoc}
     *
     * @param result
     *            The query result indicating that no more resources are to be
     *            returned and, if applicable, including information which
     *            should be used for subsequent paged results query requests.
     */
    @Override
    void handleResult(QueryResult result);
}
