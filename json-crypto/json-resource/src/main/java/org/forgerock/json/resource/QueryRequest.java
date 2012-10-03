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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;

/**
 * A request to search for all JSON resources matching a user specified set of
 * criteria.
 */
public interface QueryRequest extends Request {

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest addFieldFilter(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest addFieldFilter(String... fields);

    /**
     * Adds one or more sort keys which will be used for ordering the JSON
     * resources returned by this query request.
     *
     * @param keys
     *            The sort keys which will be used for ordering the JSON
     *            resources returned by this query request.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the sort
     *             keys.
     */
    QueryRequest addSortKey(SortKey... keys);

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceName();

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFieldFilters();

    /**
     * Returns the opaque cookie which is used by the resource provider to track
     * its position in the set of query results. Paged results will be enabled
     * if and only if the page size is non-zero.
     * <p>
     * The cookie must be {@code null} in the initial query request sent by the
     * client. For subsequent query requests the client must include the cookie
     * returned with the previous query result, until the resource provider
     * returns a {@code null} cookie indicating that the final page of results
     * has been returned.
     *
     * @return The opaque cookie which is used by the resource provider to track
     *         its position in the set of query results, or {@code null} if
     *         paged results are not requested (when the page size is 0), or if
     *         the first page of results is being requested (when the page size
     *         is non-zero).
     * @see #getPageSize()
     */
    String getPagedResultsCookie();

    /**
     * Returns the requested page results page size or {@code 0} if paged
     * results are not required. For all paged result requests other than the
     * initial request, a cookie should be provided with the query request. See
     * {@link #getPagedResultsCookie()} for more information.
     *
     * @return The requested page results page size or {@code 0} if paged
     *         results are not required.
     * @see #getPagedResultsCookie()
     */
    int getPageSize();

    /**
     * Returns the additional parameters which should be used to control the
     * behavior of this query request. The returned map may be modified if
     * permitted by this query request.
     *
     * @return The additional parameters which should be used to control the
     *         behavior of this query request (never {@code null}).
     */
    Map<String, String> getAdditionalQueryParameters();

    /**
     * Returns the query filter which will be used for selecting which JSON
     * resources will be returned.
     *
     * @return The query filter which will be used for selecting which JSON
     *         resources will be returned, or {@code null} if all JSON resources
     *         should be returned.
     */
    QueryFilter getQueryFilter();

    /**
     * Returns the query identifier for pre-defined queries.
     *
     * @return The query identifier for pre-defined queries, or {@code null} if
     *         a pre-defined query is not to be used.
     */
    String getQueryId();

    /**
     * Returns the sort keys which should be used for ordering the JSON
     * resources returned by this query request. The returned list may be
     * modified if permitted by this query request.
     *
     * @return The sort keys which should be used for ordering the JSON
     *         resources returned by this query request (never {@code null}).
     */
    List<SortKey> getSortKeys();

    /**
     * Sets an additional parameter which should be used to control the behavior
     * of this query request.
     *
     * @param name
     *            The name of the additional parameter.
     * @param value
     *            The additional parameter's value.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the
     *             additional parameters.
     */
    QueryRequest setAdditionalQueryParameter(String name, String value);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest setResourceName(String name);

    /**
     * Sets the opaque cookie which is used by the resource provider to track
     * its position in the set of query results. Paged results will be enabled
     * if and only if the page size is non-zero.
     * <p>
     * The cookie must be {@code null} in the initial query request sent by the
     * client. For subsequent query requests the client must include the cookie
     * returned with the previous query result, until the resource provider
     * returns a {@code null} cookie indicating that the final page of results
     * has been returned.
     *
     * @param cookie
     *            The opaque cookie which is used by the resource provider to
     *            track its position in the set of query results, or
     *            {@code null} if paged results are not requested (when the page
     *            size is 0), or if the first page of results is being requested
     *            (when the page size is non-zero).
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the paged
     *             results cookie.
     * @see #setPageSize(int)
     */
    QueryRequest setPagedResultsCookie(String cookie);

    /**
     * Sets the requested page results page size or {@code 0} if paged results
     * are not required. For all paged result requests other than the initial
     * request, a cookie should be provided with the query request. See
     * {@link #setPagedResultsCookie(String)} for more information.
     *
     * @param size
     *            The requested page results page size or {@code 0} if paged
     *            results are not required.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the page
     *             size.
     * @see #getPagedResultsCookie()
     */
    QueryRequest setPageSize(int size);

    /**
     * Sets the query filter which will be used for selecting which JSON
     * resources will be returned.
     *
     * @param filter
     *            The query filter which will be used for selecting which JSON
     *            resources will be returned, or {@code null} if all JSON
     *            resources should be returned.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the query
     *             filter.
     */
    QueryRequest setQueryFilter(QueryFilter filter);

    /**
     * Sets the query identifier for pre-defined queries.
     *
     * @param id
     *            The query identifier for pre-defined queries, or {@code null}
     *            if a pre-defined query is not to be used.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the query
     *             identifier.
     */
    QueryRequest setQueryId(String id);
}
