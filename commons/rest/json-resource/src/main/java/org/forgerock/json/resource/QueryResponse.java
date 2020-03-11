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

import org.forgerock.util.promise.Promise;

/**
 * The final result of a query request returned after all resources matching the
 * request have been returned. In addition to indicating that no more resources
 * are to be returned by the query, the query result will contain page results
 * state information if result paging has been enabled for the query.
 */
public interface QueryResponse extends Response {

    /**
     * The name of the field which contains the error in the JSON
     * representation.
     */
    String FIELD_ERROR = "error";

    /**
     * The name of the field which contains the paged results cookie in the JSON
     * representation.
     */
    String FIELD_PAGED_RESULTS_COOKIE = QueryRequest.FIELD_PAGED_RESULTS_COOKIE;

    /**
     * The name of the field which contains the policy used for calculating
     * the total number of paged results in the JSON representation.
     */
    String FIELD_TOTAL_PAGED_RESULTS_POLICY = QueryRequest.FIELD_TOTAL_PAGED_RESULTS_POLICY;

    /**
     * The name of the field which contains the total paged results in the JSON
     * representation.
     */
    String FIELD_TOTAL_PAGED_RESULTS = "totalPagedResults";

    /**
     * The name of the field which contains the remaining paged results in the
     * JSON representation.
     */
    String FIELD_REMAINING_PAGED_RESULTS = "remainingPagedResults";

    /**
     * The name of the field which contains the result count in the JSON
     * representation.
     */
    String FIELD_RESULT_COUNT = "resultCount";

    /**
     * The name of the field which contains the array of matching resources in
     * the JSON representation.
     */
    String FIELD_RESULT = "result";

    /**
     * The value provided when no count is known or can reasonably be supplied.
     */
    int NO_COUNT = -1;

    /**
     * Returns the policy that was used to calculate the {@literal totalPagedResults}.
     *
     * @return The count policy.
     * @see #getTotalPagedResults()
     */
    CountPolicy getTotalPagedResultsPolicy();

    /**
     * Returns the opaque cookie which can be used for the next cookie-based
     * paged request. A cookie will only be returned if paged results have
     * been requested via a non-zero {@code pageSize}. Cookies are only
     * guaranteed for {@link org.forgerock.util.query.QueryFilter}-based
     * queries. Implicit sorting may be supported by the resource provider
     * but it is not required. Given the arbitrary nature of query expressions
     * (and expression-backed queryIds) there can be no guarantee made of
     * cookie support for these queries.
     *
     * <p>
     *     <em>Note:</em>Cookies have a limited lifespan. They should
     *     not be stored long-term. Cookies should only be used on immediate
     *     subsequent requests or behavior is undefined.
     * </p>
     *
     * @return The opaque cookie which should be used with the next cookie-based paged
     *         results query request, or {@code null} if paged results were not
     *         requested, there are no more pages to be returned, or cookies are not
     *         supported for this query.
     *
     * @see QueryRequest#getPagedResultsCookie()
     * @see QueryRequest#setPagedResultsCookie(String)
     * @see QueryRequest#addSortKey(SortKey...)
     * @see QueryRequest#addSortKey(String...)
     */
    String getPagedResultsCookie();

    /**
     * Returns the total number of paged results in adherence with
     * the {@link QueryRequest#getTotalPagedResultsPolicy()} in the request
     * or {@link #NO_COUNT} if paged results were not requested, the count
     * policy is {@code NONE}, or the total number of paged
     * results is unknown.
     *
     * @return A count of the total number of paged results to be
     *         returned in subsequent paged results query requests, or
     *         {@link #NO_COUNT} if paged results were not requested, or if the total
     *         number of paged results is unknown.
     */
    int getTotalPagedResults();

    /**
     * Returns an estimate of the total number of remaining results to be
     * returned in subsequent paged results query requests.
     *
     * @return An estimate of the total number of remaining results to be
     *         returned in subsequent paged results query requests, or
     *         {@code -1} if paged results were not requested, or if the total
     *         number of remaining results is unknown.
     */
    int getRemainingPagedResults();

    /**
     * Return this response as a result Promise.
     *
     * @return A Promise whose result is this QueryResponse object.
     */
    Promise<QueryResponse, ResourceException> asPromise();
}
