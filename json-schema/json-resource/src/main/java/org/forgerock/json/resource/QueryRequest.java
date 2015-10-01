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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.query.QueryFilter;

/**
 * A request to search for all JSON resources matching a user specified set of criteria.
 * <p>
 * There are four types of query request: <ul> <li>default query: when neither a filter, expression or query ID are
 * specified all resources will be returned <li>query by filter: returns all resources which match the {@link
 * QueryFilters} specified using {@link #setQueryFilter(QueryFilter)} <li>query by ID: returns all resources which match
 * the named prepared query specified using {@link #setQueryId(String)} <li>query by expression: returns all resources
 * which match a native expression specified using {@link #setQueryExpression(String)}. Note that this type of query
 * should only be used in very rare cases since it introduces a tight coupling between the application and the
 * underlying JSON resource. In addition, applications should take care to prevent users from directly accessing this
 * form of query for security reasons. </ul>
 * <p>
 * In addition to the above mentioned query types queries may also be paged when a page size is found via {@link
 * #getPageSize()}. Paged requests should be used in most cases when an unknown number of query results will be
 * returned.
 */
public interface QueryRequest extends Request {
    /**
     * The name of the field which contains the paged results cookie in the JSON representation.
     */
    String FIELD_PAGED_RESULTS_COOKIE = "pagedResultsCookie";
    /**
     * The name of the field which contains the paged results offset in the JSON representation.
     */
    String FIELD_PAGED_RESULTS_OFFSET = "pagedResultsOffset";
    /**
     * The name of the field which contains the page size in the JSON representation.
     */
    String FIELD_PAGE_SIZE = "pageSize";
    /**
     * The name of the field which contains the query expression in the JSON representation.
     */
    String FIELD_QUERY_EXPRESSION = "queryExpression";
    /**
     * The name of the field which contains the query filter in the JSON representation.
     */
    String FIELD_QUERY_FILTER = "queryFilter";
    /**
     * The name of the field which contains the query ID in the JSON representation.
     */
    String FIELD_QUERY_ID = "queryId";
    /**
     * The name of the field which contains the sort keys in the JSON representation.
     */
    String FIELD_SORT_KEYS = "sortKeys";
    /**
     * The name of the field which contains the policy used for calculating the total number of paged results.
     */
    String FIELD_TOTAL_PAGED_RESULTS_POLICY = "totalPagedResultsPolicy";


    @Override
    <R, P> R accept(final RequestVisitor<R, P> v, final P p);


    @Override
    QueryRequest addField(JsonPointer... fields);


    @Override
    QueryRequest addField(String... fields);

    /**
     * Adds one or more sort keys which will be used for ordering the JSON resources returned by this query request.
     *
     * @param keys
     *         The sort keys which will be used for ordering the JSON resources returned by this query request.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the sort keys.
     */
    QueryRequest addSortKey(SortKey... keys);

    /**
     * Adds one or more sort keys which will be used for ordering the JSON resources returned by this query request.
     *
     * @param keys
     *         The sort keys which will be used for ordering the JSON resources returned by this query request.
     * @return This query request.
     * @throws IllegalArgumentException
     *         If one or more of the provided sort keys could not be parsed.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the sort keys.
     */
    QueryRequest addSortKey(String... keys);


    @Override
    String getAdditionalParameter(String name);


    @Override
    Map<String, String> getAdditionalParameters();


    @Override
    List<JsonPointer> getFields();

    /**
     * Returns the requested page results page size or {@code 0} if paged results are not required. For all paged result
     * requests other than the initial request, a cookie should be provided with the query request. See {@link
     * #getPagedResultsCookie()} for more information.
     *
     * @return The requested page results page size or {@code 0} if paged results are not required.
     * @see #getPagedResultsCookie()
     * @see #getPagedResultsOffset()
     */
    int getPageSize();

    /**
     * Returns the opaque cookie which is used by the resource provider to track its position in the set of query
     * results. Paged results will be enabled if and only if the page size is non-zero.
     * <p>
     * The cookie must be {@code null} in the initial query request sent by the client. For subsequent query requests
     * the client must include the cookie returned with the previous query result, until the resource provider returns a
     * {@code null} cookie indicating that the final page of results has been returned.
     * <p>
     * <em>Note:</em> Cookies and offsets are mutually exclusive.
     *
     * @return The opaque cookie which is used by the resource provider to track its position in the set of query
     * results, or {@code null} if paged results are not requested (when the page size is 0) or if the first page of
     * results is being requested (when the page size is non-zero).
     * @see #getPageSize()
     * @see #getPagedResultsOffset()
     */
    String getPagedResultsCookie();

    /**
     * Returns the zero-based index of the first resource which should be included in the query results. An offset of 0
     * (default) will return the first resource in the collection. An offset of {@code 1} will return the second, and so
     * on ...
     * <p>
     * <em>Note:</em> Offsets and cookies are mutually exclusive. When a cookie is supplied only the default {@code 0}
     * offset is supported.
     * <p>
     * Offset must be a zero-based integer denoting the number of records to skip. This is very similar to the
     * <code>LIMIT</code> and <code>SKIP</code> clauses in SQL databases.
     *
     * @return The zero-based index within the result set of the first result which should be returned.
     * @see #getPageSize()
     * @see #getPagedResultsCookie()
     */
    int getPagedResultsOffset();


    @Override
    PreferredLocales getPreferredLocales();

    /**
     * Returns the native query expression which will be used for processing the query request. An example of a native
     * query expression is a SQL statement.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @return The native query expression which will be used for processing the query request, or {@code null} if
     * another type of query is to be performed.
     * @see QueryRequest#getQueryFilter()
     * @see QueryRequest#getQueryId()
     */
    String getQueryExpression();

    /**
     * Returns the query filter which will be used for selecting which JSON resources will be returned.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @return The query filter which will be used for selecting which JSON resources will be returned, or {@code null}
     * if another type of query is to be performed.
     * @see QueryRequest#getQueryExpression()
     * @see QueryRequest#getQueryId()
     */
    QueryFilter<JsonPointer> getQueryFilter();

    /**
     * Returns the query identifier for pre-defined queries.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @return The query identifier for pre-defined queries, or {@code null} if a pre-defined query is not to be used,
     * or {@code null} if another type of query is to be performed.
     * @see QueryRequest#getQueryExpression()
     * @see QueryRequest#getQueryFilter()
     */
    String getQueryId();


    @Override
    RequestType getRequestType();

    @Override
    String getResourcePath();

    @Override
    ResourcePath getResourcePathObject();

    @Override
    Version getResourceVersion();

    /**
     * Returns the sort keys which should be used for ordering the JSON resources returned by this query request. The
     * returned list may be modified if permitted by this query request.
     *
     * @return The sort keys which should be used for ordering the JSON resources returned by this query request (never
     * {@code null}).
     */
    List<SortKey> getSortKeys();

    /**
     * Returns the {@link CountPolicy} used to calculate {@link QueryResponse#getTotalPagedResults()}.
     *
     * @return The count policy.
     * @see QueryResponse#getTotalPagedResults()
     */
    CountPolicy getTotalPagedResultsPolicy();

    @Override
    QueryRequest setAdditionalParameter(String name, String value) throws BadRequestException;

    /**
     * Sets the requested page results page size or {@code 0} if paged results are not required. For all paged result
     * requests other than the initial request, a cookie should be provided with the query request. See {@link
     * #setPagedResultsCookie(String)} for more information.
     *
     * @param size
     *         The requested page results page size or {@code 0} if paged results are not required.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the page size.
     * @see #getPagedResultsCookie()
     * @see #setPagedResultsOffset(int)
     */
    QueryRequest setPageSize(int size);

    /**
     * Sets the opaque cookie which is used by the resource provider to track its position in the set of query results.
     * Paged results will be enabled if and only if the page size is non-zero.
     * <p>
     * The cookie must be {@code null} in the initial query request sent by the client. For subsequent query requests
     * the client must include the cookie returned with the previous query result, until the resource provider returns a
     * {@code null} cookie indicating that the final page of results has been returned.
     * <p>
     * When subsequent paged requests are being made no query parameters may be altered; doing so will result in
     * undefined behavior. The only parameter that may be changed during paged requests is the page size.
     *
     * @param cookie
     *         The opaque cookie which is used by the resource provider to track its position in the set of query
     *         results.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the paged results cookie.
     * @see #setPageSize(int)
     * @see #addSortKey(SortKey...)
     * @see #addSortKey(String...)
     */
    QueryRequest setPagedResultsCookie(String cookie);

    /**
     * Sets the zero-based index of the first resource which should be included in the query results. An offset of 0
     * (default) will return the first resource in the collection. An offset of {@code 1} will return the second, and so
     * on ...
     * <p>
     * <em>Note:</em> Offsets and cookies are mutually exclusive. When a cookie is supplied only the default {@code 0}
     * offset is supported.
     * <p>
     * Offset must be a zero-based integer denoting the number of records to skip. This is very similar to the
     * <code>LIMIT</code> and <code>SKIP</code> clauses in SQL databases.
     *
     * @param offset
     *         The index within the result set of the first result which should be returned.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the paged results offset.
     * @see #setPageSize(int)
     * @see #setPagedResultsCookie(String)
     */
    QueryRequest setPagedResultsOffset(int offset);

    @Override
    QueryRequest setPreferredLocales(PreferredLocales preferredLocales);

    /**
     * Sets the native query expression which will be used for processing the query request. An example of a native
     * query expression is a SQL statement.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @param expression
     *         The native query expression which will be used for processing the query request, or {@code null} if
     *         another type of query is to be performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the query identifier.
     * @see QueryRequest#setQueryFilter(QueryFilter)
     * @see QueryRequest#setQueryId(String)
     */
    QueryRequest setQueryExpression(String expression);

    /**
     * Sets the query filter which will be used for selecting which JSON resources will be returned.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @param filter
     *         The query filter which will be used for selecting which JSON resources will be returned, or {@code null}
     *         if another type of query is to be performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the query filter.
     * @see QueryRequest#setQueryExpression(String)
     * @see QueryRequest#setQueryId(String)
     */
    QueryRequest setQueryFilter(QueryFilter<JsonPointer> filter);

    /**
     * Sets the query identifier for pre-defined queries.
     * <p>
     * <b>NOTE:</b> the native query expression, query filter, and query ID parameters are mutually exclusive and only
     * one of them may be specified.
     *
     * @param id
     *         The query identifier for pre-defined queries, or {@code null} if another type of query is to be
     *         performed.
     * @return This query request.
     * @throws UnsupportedOperationException
     *         If this query request does not permit changes to the query identifier.
     * @see QueryRequest#setQueryExpression(String)
     * @see QueryRequest#setQueryFilter(QueryFilter)
     */
    QueryRequest setQueryId(String id);

    @Override
    QueryRequest setResourcePath(ResourcePath path);

    @Override
    QueryRequest setResourcePath(String path);

    @Override
    QueryRequest setResourceVersion(Version resourceVersion);

    /**
     * Sets the policy for calculating the total number of paged results. If no count policy is supplied or paged
     * results are not requested a default of {@link CountPolicy#NONE} will be used. This will result in no count being
     * performed and no overhead incurred.
     *
     * @param policy
     *         The policy used to calculate total paged results
     * @return This query request.
     * @see QueryResponse#getTotalPagedResultsPolicy()
     * @see QueryResponse#getTotalPagedResults()
     */
    QueryRequest setTotalPagedResultsPolicy(CountPolicy policy);

    @Override
    JsonValue toJsonValue();
}
