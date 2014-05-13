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
 * Copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;

/**
 * A request to search for all JSON resources matching a user specified set of
 * criteria.
 * <p>
 * There are four types of query request:
 * <ul>
 * <li>default query: when neither a filter, expression or query ID are
 * specified all resources will be returned
 * <li>query by filter: returns all resources which match the
 * {@link QueryFilter} specified using {@link #setQueryFilter(QueryFilter)}
 * <li>query by ID: returns all resources which match the named prepared query
 * specified using {@link #setQueryId(String)}
 * <li>query by expression: returns all resources which match a native
 * expression specified using {@link #setQueryExpression(String)}. Note that
 * this type of query should only be used in very rare cases since it introduces
 * a tight coupling between the application and the underlying JSON resource. In
 * addition, applications should take care to prevent users from directly
 * accessing this form of query for security reasons.
 * </ul>
 */
public interface QueryRequest extends Request {
    /**
     * The name of the field which contains the paged results cookie in the JSON
     * representation.
     */
    public static final String FIELD_PAGED_RESULTS_COOKIE = "pagedResultsCookie";

    /**
     * The name of the field which contains the paged results offset in the JSON
     * representation.
     */
    public static final String FIELD_PAGED_RESULTS_OFFSET = "pagedResultsOffset";

    /**
     * The name of the field which contains the page size in the JSON
     * representation.
     */
    public static final String FIELD_PAGE_SIZE = "pageSize";

    /**
     * The name of the field which contains the query expression in the JSON
     * representation.
     */
    public static final String FIELD_QUERY_EXPRESSION = "queryExpression";

    /**
     * The name of the field which contains the query filter in the JSON
     * representation.
     */
    public static final String FIELD_QUERY_FILTER = "queryFilter";

    /**
     * The name of the field which contains the query ID in the JSON
     * representation.
     */
    public static final String FIELD_QUERY_ID = "queryId";

    /**
     * The name of the field which contains the sort keys in the JSON
     * representation.
     */
    public static final String FIELD_SORT_KEYS = "sortKeys";

    /**
     * {@inheritDoc}
     */
    <R, P> R accept(RequestVisitor<R, P> v, P p);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest addField(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest addField(String... fields);

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
     * Adds one or more sort keys which will be used for ordering the JSON
     * resources returned by this query request.
     *
     * @param keys
     *            The sort keys which will be used for ordering the JSON
     *            resources returned by this query request.
     * @return This query request.
     * @throws IllegalArgumentException
     *             If one or more of the provided sort keys could not be parsed.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the sort
     *             keys.
     */
    QueryRequest addSortKey(String... keys);

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFields();

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
     * @see #getPagedResultsOffset()
     */
    String getPagedResultsCookie();

    /**
     * Returns the index within the result set of the first result which should
     * be returned. Paged results will be enabled if and only if the page size
     * is non-zero. If the parameter is not present or a value less than 1 is
     * specified then then the page following the previous page returned will be
     * returned. A value equal to or greater than 1 indicates that a specific
     * page should be returned starting from the position specified.
     *
     * @return The index within the result set of the first result which should
     *         be returned.
     * @see #getPageSize()
     * @see #getPagedResultsCookie()
     */
    int getPagedResultsOffset();

    /**
     * Returns the requested page results page size or {@code 0} if paged
     * results are not required. For all paged result requests other than the
     * initial request, a cookie should be provided with the query request. See
     * {@link #getPagedResultsCookie()} for more information.
     *
     * @return The requested page results page size or {@code 0} if paged
     *         results are not required.
     * @see #getPagedResultsCookie()
     * @see #getPagedResultsOffset()
     */
    int getPageSize();

    /**
     * Returns the native query expression which will be used for processing the
     * query request. An example of a native query expression is a SQL
     * statement.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @return The native query expression which will be used for processing the
     *         query request, or {@code null} if another type of query is to be
     *         performed.
     * @see QueryRequest#getQueryFilter()
     * @see QueryRequest#getQueryId()
     */
    String getQueryExpression();

    /**
     * Returns the query filter which will be used for selecting which JSON
     * resources will be returned.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @return The query filter which will be used for selecting which JSON
     *         resources will be returned, or {@code null} if another type of
     *         query is to be performed.
     * @see QueryRequest#getQueryExpression()
     * @see QueryRequest#getQueryId()
     */
    QueryFilter getQueryFilter();

    /**
     * Returns the query identifier for pre-defined queries.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @return The query identifier for pre-defined queries, or {@code null} if
     *         a pre-defined query is not to be used, or {@code null} if another
     *         type of query is to be performed.
     * @see QueryRequest#getQueryExpression()
     * @see QueryRequest#getQueryFilter()
     */
    String getQueryId();

    /**
     * {@inheritDoc}
     */
    @Override
    RequestType getRequestType();

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceName();

    /**
     * {@inheritDoc}
     */
    @Override
    ResourceName getResourceNameObject();

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
     * @see #setPagedResultsOffset(int)
     */
    QueryRequest setPagedResultsCookie(String cookie);

    /**
     * Sets the index within the result set of the first result which should be
     * returned. Paged results will be enabled if and only if the page size is
     * non-zero. If the parameter is not present or a value less than 1 is
     * specified then then the page following the previous page returned will be
     * returned. A value equal to or greater than 1 indicates that a specific
     * page should be returned starting from the position specified.
     *
     * @param offset
     *            The index within the result set of the first result which
     *            should be returned.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the paged
     *             results offset.
     * @see #setPageSize(int)
     * @see #setPagedResultsCookie(String)
     */
    QueryRequest setPagedResultsOffset(int offset);

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
     * @see #setPagedResultsOffset(int)
     */
    QueryRequest setPageSize(int size);

    /**
     * Sets the native query expression which will be used for processing the
     * query request. An example of a native query expression is a SQL
     * statement.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @param expression
     *            The native query expression which will be used for processing
     *            the query request, or {@code null} if another type of query is
     *            to be performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the query
     *             identifier.
     * @see QueryRequest#setQueryFilter(QueryFilter)
     * @see QueryRequest#setQueryId(String)
     */
    QueryRequest setQueryExpression(String expression);

    /**
     * Sets the query filter which will be used for selecting which JSON
     * resources will be returned.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @param filter
     *            The query filter which will be used for selecting which JSON
     *            resources will be returned, or {@code null} if another type of
     *            query is to be performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the query
     *             filter.
     * @see QueryRequest#setQueryExpression(String)
     * @see QueryRequest#setQueryId(String)
     */
    QueryRequest setQueryFilter(QueryFilter filter);

    /**
     * Sets the query identifier for pre-defined queries.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID
     * parameters are mutually exclusive and only one of them may be specified.
     *
     * @param id
     *            The query identifier for pre-defined queries, or {@code null}
     *            if another type of query is to be performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *             If this query request does not permit changes to the query
     *             identifier.
     * @see QueryRequest#setQueryExpression(String)
     * @see QueryRequest#setQueryFilter(QueryFilter)
     */
    QueryRequest setQueryId(String id);

    /**
     * {@inheritDoc}
     */
    @Override
    Request setResourceName(ResourceName name);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest setResourceName(String name);

    /**
     * {@inheritDoc}
     */
    @Override
    QueryRequest setAdditionalParameter(String name, String value) throws BadRequestException;
}
