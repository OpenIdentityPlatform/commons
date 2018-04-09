/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.javascript;

import org.forgerock.json.resource.QueryRequest;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

/**
 * Provides a {@code Scriptable} wrapper for a {@code QueryRequest} object.
 */
class ScriptableQueryRequest extends AbstractScriptableRequest implements Wrapper {

    private static final long serialVersionUID = 1L;


    /** The map being wrapped. */
    private final QueryRequest request;

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptableQueryRequest(final Parameter parameter, final QueryRequest request) {
        super(parameter, request);
        this.request = request;
    }

    @Override
    public String getClassName() {
        return "ScriptableQueryRequest";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {

        if (QueryRequest.FIELD_PAGED_RESULTS_COOKIE.equals(name)) {
            return Converter.wrap(parameter, request.getPagedResultsCookie(), start, false);
        } else if (QueryRequest.FIELD_PAGED_RESULTS_OFFSET.equals(name)) {
            return Converter.wrap(parameter, request.getPagedResultsOffset(), start, false);
        } else if (QueryRequest.FIELD_PAGE_SIZE.equals(name)) {
            return Converter.wrap(parameter, request.getPageSize(), start, false);
        } else if (QueryRequest.FIELD_QUERY_EXPRESSION.equals(name)) {
            return Converter.wrap(parameter, request.getQueryExpression(), start, false);
        } else if (QueryRequest.FIELD_QUERY_FILTER.equals(name)) {
            return Converter.wrap(parameter, request.getQueryFilter(), start, false);
        } else if (QueryRequest.FIELD_QUERY_ID.equals(name)) {
            return Converter.wrap(parameter, request.getQueryId(), start, false);
        } else if (QueryRequest.FIELD_SORT_KEYS.equals(name)) {
            return Converter.wrap(parameter, request.getSortKeys(), start, false);
        } else {
            return super.get(name, start);
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return QueryRequest.FIELD_PAGED_RESULTS_COOKIE.equals(name)
                || QueryRequest.FIELD_PAGED_RESULTS_OFFSET.equals(name)
                || QueryRequest.FIELD_PAGE_SIZE.equals(name)
                || QueryRequest.FIELD_QUERY_EXPRESSION.equals(name)
                || QueryRequest.FIELD_QUERY_FILTER.equals(name)
                || QueryRequest.FIELD_QUERY_ID.equals(name)
                || QueryRequest.FIELD_SORT_KEYS.equals(name)
                || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO allow updating fields
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
    }

    /** concatenate the QueryRequest properties with the generic request properties */
    private static Object[] PROPERTIES = concatIds(
            QueryRequest.FIELD_PAGED_RESULTS_COOKIE,
            QueryRequest.FIELD_PAGED_RESULTS_OFFSET,
            QueryRequest.FIELD_PAGE_SIZE,
            QueryRequest.FIELD_QUERY_EXPRESSION,
            QueryRequest.FIELD_QUERY_FILTER,
            QueryRequest.FIELD_QUERY_ID,
            QueryRequest.FIELD_SORT_KEYS);

    @Override
    public Object[] getIds() {
        return PROPERTIES;
    }

}
