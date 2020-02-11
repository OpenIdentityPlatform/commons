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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.util.query;

import static org.forgerock.util.query.QueryFilterOperators.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A {@link QueryFilterVisitor} that produces a Map representation of the filter tree.
 * <p>
 * The produced map is constructed according to the following representation:
 *
 * <dl>
 *     <dt>an and expression</dt>
 *     <dd><pre>{ "operator" : "and", "subfilters" : [ the subfilters ] }</pre></dd>
 *     <dt>an or expression</dt>
 *     <dd><pre>{ "operator" : "or", "subfilters" : [ the subfilters ] }</pre></dd>
 *     <dt>a not expression</dt>
 *     <dd><pre>{ "operator" : "not", "subfilter" : the subfilter }</pre></dd>
 *     <dt>a presence expression</dt>
 *     <dd><pre>{ "operator" : "pr", "field" : "/afield"}</pre></dd>
 *     <dt>an equals expression</dt>
 *     <dd><pre>{ "operator" : "eq", "field" : "/afield", "value" : "something"}</pre></dd>
 *     <dt>a contains expression</dt>
 *     <dd><pre>{ "operator" : "co", "field" : "/afield", "value" : "something"}</pre></dd>
 *     <dt>a starts-with expression</dt>
 *     <dd><pre>{ "operator" : "sw", "field" : "/afield", "value" : "some"}</pre></dd>
 *     <dt>a less-than expression</dt>
 *     <dd><pre>{ "operator" : "lt", "field" : "/afield", "value" : "something"}</pre></dd>
 *     <dt>a less-than-or-equal-to expression</dt>
 *     <dd><pre>{ "operator" : "le", "field" : "/afield", "value" : "something"}</pre></dd>
 *     <dt>a greater-than expression</dt>
 *     <dd><pre>{ "operator" : "gt", "field" : "/afield", "value" : "something"}</pre></dd>
 *     <dt>a greater-than-or-equal-to expression</dt>
 *     <dd><pre>{ "operator" : "ge", "field" : "/afield", "value" : "something"}</pre></dd>
 * </dl>
 * <em><strong>Notes:</strong></em>
 * <ol>
 *     <li>
 *         JSON notation used for convenience to illustrate Map-structure.
 *         <ul>
 *             <li>To produce JSON, use
 *                  {@code new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);}
 *             </li>
 *             <li>Or wrap with JsonValue: <pre>new org.forgerock.json.JsonValue(map);</pre></li>
 *         </ul>
 *     </li>
 *     <li>
 *         Field values are shown in {@code org.forgerock.json.JsonPointer} syntax; actual field representation
 * depends on field type of QueryFilter.
 *     </li>
 * </ol>
 *
 * @param <F> The type of field description used in parsed {@link QueryFilter} objects.
 */
public class MapFilterVisitor<F> implements QueryFilterVisitor<Map<String, Object>, Void, F> {
    static final String OPERATOR = "operator";
    static final String FIELD = "field";
    static final String VALUE = "value";
    static final String SUBFILTERS = "subfilters";
    static final String SUBFILTER = "subfilter";

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitAndFilter(final Void parameters, List<QueryFilter<F>> subFilters) {
        final List<Object> filters = new ArrayList<>();
        for (QueryFilter<F> filter : subFilters) {
            filters.add(filter.accept(this, parameters));
        }
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, AND);
        object.put(SUBFILTERS, filters);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitBooleanLiteralFilter(Void parameters, boolean value) {
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, value);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitContainsFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, CONTAINS, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitEqualsFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, EQUALS, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitExtendedMatchFilter(Void parameters, F field, String operator,
            Object valueAssertion) {
        return object(field, operator, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitGreaterThanFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, GREATER_THAN, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitGreaterThanOrEqualToFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, GREATER_EQUAL, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitLessThanFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, LESS_THAN, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitLessThanOrEqualToFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, LESS_EQUAL, valueAssertion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitNotFilter(Void parameters, QueryFilter<F> subFilter) {
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, NOT);
        object.put(SUBFILTER, subFilter.accept(this, parameters));
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitOrFilter(final Void parameters, List<QueryFilter<F>> subFilters) {
        final List<Object> filters = new ArrayList<>();
        for (QueryFilter<F> filter : subFilters) {
            filters.add(filter.accept(this, parameters));
        }
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, OR);
        object.put(SUBFILTERS, filters);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitPresentFilter(Void parameters, F field) {
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, PRESENT);
        object.put(FIELD, field.toString());
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitStartsWithFilter(Void parameters, F field, Object valueAssertion) {
        return object(field, STARTS_WITH, valueAssertion);
    }

    private Map<String, Object> object(F field, String operator, Object valueAssertion) {
        final Map<String, Object> object = new HashMap<>();
        object.put(OPERATOR, operator);
        object.put(FIELD, field.toString());
        if (valueAssertion == null || valueAssertion instanceof Number || valueAssertion instanceof Boolean) {
            object.put(VALUE, valueAssertion);
        } else {
            object.put(VALUE, String.valueOf(valueAssertion));
        }
        return object;
    }

}
