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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.elasticsearch;

import static org.forgerock.audit.events.AuditEventHelper.jsonPointerToDotNotation;
import static org.forgerock.audit.util.ElasticsearchUtil.normalizeJsonPointer;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements a {@link QueryFilterVisitor} for <a href="https://www.elastic.co/">Elasticsearch</a> that returns a
 * JsonValue mapping for each visitor operation to the corresponding Elasticsearch query operation in the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl.html">Elasticsearch Query DSL.</a>
 */
class ElasticsearchQueryFilterVisitor implements QueryFilterVisitor<JsonValue, Void, JsonPointer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchQueryFilterVisitor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-all-query.html">
     *     match_all</a>
     * query for literal true. Literal false will return an {@link UnsupportedOperationException}.
     * @param aVoid A unused {@link Void} parameter.
     * @param value The booolean true or false literal.
     * @return A {@link JsonValue} match_all query.
     * @throws UnsupportedOperationException If literal false is used.
     */
    @Override
    public JsonValue visitBooleanLiteralFilter(Void aVoid, boolean value) {
        // "match_all": { }
        if (!value) {
            throw new UnsupportedOperationException("Boolean literal filter (false) not supported on this endpoint");
        }
        return json(object(field("match_all", object())));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-wildcard-query.html">wildcard</a>
     * query to support contains. The field for this operation must be declared as not_analyzed.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field that contains a value.
     * @param valueAssertion The value to contain.
     * @return A {@link JsonValue} wildcard query.
     */
    @Override
    public JsonValue visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "wildcard" : { "field" : "*contains*" }
        try {
            return json(
                    object(field(
                            "wildcard",
                            object(field(
                                    jsonPointerToDotNotation(normalizeJsonPointer(field).toString()),
                                    "*" + MAPPER.writeValueAsString(valueAssertion) + "*"
                            ))
                    ))
            );
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return json(object());
        }
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-term-query.html">term</a>
     * query to test equality. The field for this operation must be declared as not_analyzed.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field being tested for equality.
     * @param valueAssertion The equality value.
     * @return A {@link JsonValue} representing a term query.
     */
    @Override
    public JsonValue visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "term" : { "field" : "value" }
        return json(object(field("term",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()), valueAssertion)))));
    }

    /**
     * Not a supported operation.
     *
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException Always throws this exception.
     */
    @Override
    public JsonValue visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException("Extended match filter not supported on this endpoint");
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html">range</a> query.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field being range tested.
     * @param valueAssertion The value used in the range.
     * @return A {@link JsonValue} representing a range query.
     */
    @Override
    public JsonValue visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"gt" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()),
                        object(field("gt", valueAssertion))
                ))
        )));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html">range</a> query.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field being range tested.
     * @param valueAssertion The value used in the range.
     * @return A {@link JsonValue} representing a range query.
     */
    @Override
    public JsonValue visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"gte" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()),
                        object(field("gte", valueAssertion))
                ))
        )));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html">range</a> query.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field being range tested.
     * @param valueAssertion The value used in the range.
     * @return A {@link JsonValue} representing a range query.
     */
    @Override
    public JsonValue visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"lt" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()),
                        object(field("lt", valueAssertion))
                ))
        )));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html">range</a> query.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field being range tested.
     * @param valueAssertion The value used in the range.
     * @return A {@link JsonValue} representing a range query.
     */
    @Override
    public JsonValue visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"lte" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()),
                        object(field("lte", valueAssertion))
                ))
        )));
    }

    /**
     * Creates a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-bool-query.html">
     *     bool</a>
     * query that populates the "must_not" field.
     * @param aVoid A unused {@link Void} parameter.
     * @param subFilter The subFilters for the not operation.
     * @return A {@link JsonValue} representing a bool query that populates the "must_not" field.
     */
    @Override
    public JsonValue visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
        // "bool" : { "must_not" : { subFilter... } }
        return json(object(field("bool",
                object(field("must_not", subFilter.accept(this, aVoid).getObject()))
        )));
    }

    /**
     * Creates a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-bool-query.html">
     *     bool</a>
     * query that populates the "should" field.
     * @param aVoid A unused {@link Void} parameter.
     * @param subFilters The subFilters for the or operation.
     * @return A {@link JsonValue} representing a bool query that populates the "should" field.
     */
    @Override
    public JsonValue visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
        // "bool" : { "should" : { subFilters... } }
        final List<Object> filterResults = new ArrayList<>();
        for (QueryFilter<JsonPointer> filter : subFilters) {
            filterResults.add(filter.accept(this, aVoid).asMap());
        }
        return json(object(field("bool",
                object(field("should", filterResults))
        )));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-bool-query.html">bool</a>
     * query that populates the "must" field.
     * @param aVoid A unused {@link Void} parameter.
     * @param subFilters The subFilters for the and operation.
     * @return A {@link JsonValue} representing a bool query that populates the "must" field.
     */
    @Override
    public JsonValue visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
        // "bool" : { "must" : { subFilters... } }
        final List<Object> filterResults = new ArrayList<>();
        for (QueryFilter<JsonPointer> filter : subFilters) {
            filterResults.add(filter.accept(this, aVoid).asMap());
        }
        return json(object(field("bool",
                object(field("must", filterResults))
        )));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-exists-query.html">exists</a>
     * query.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field to test for existence.
     * @return A {@link JsonValue} representing a exists query.
     */
    @Override
    public JsonValue visitPresentFilter(Void aVoid, JsonPointer field) {
        // "exists" : { "field" : "fieldValue" }
        return json(object(field("exists",
                object(field("field", jsonPointerToDotNotation(normalizeJsonPointer(field).toString()))))));
    }

    /**
     * Creates a
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-prefix-query.html">prefix</a>
     * query. The field for this operation must be declared as not_analyzed.
     * @param aVoid A unused {@link Void} parameter.
     * @param field The field to test for a prefix.
     * @param valueAssertion The prefix value.
     * @return A {@link JsonValue} representing a prefix query.
     */
    @Override
    public JsonValue visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "prefix" : { "field" : "prefixValue" }
        return json(object(field("prefix",
                object(field(jsonPointerToDotNotation(normalizeJsonPointer(field).toString()), valueAssertion)))));
    }
}
