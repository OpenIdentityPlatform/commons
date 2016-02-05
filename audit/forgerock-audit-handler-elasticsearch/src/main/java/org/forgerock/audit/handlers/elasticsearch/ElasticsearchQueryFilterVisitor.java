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

public class ElasticsearchQueryFilterVisitor implements QueryFilterVisitor<JsonValue, Void, JsonPointer> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchQueryFilterVisitor.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public JsonValue visitBooleanLiteralFilter(Void aVoid, boolean value) {
        // "match_all": { }
        if (value == false) {
            throw new UnsupportedOperationException("Boolean literal filter (false) not supported on this endpoint");
        }
        return json(object(field("match_all", object())));
    }

    @Override
    public JsonValue visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "term" : { "wildcard" : { "field" : "*contains*" } }
        try {
            return json(
                    object(field(
                            "wildcard",
                            object(field(
                                    jsonPointerToDotNotation(field.toString()),
                                    "*" + mapper.writeValueAsString(valueAssertion) + "*"
                            ))
                    ))
            );
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            return json(object());
        }
    }

    @Override
    public JsonValue visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "term" : { "field" : "value" }
        return json(object(field("term", object(field(jsonPointerToDotNotation(field.toString()), valueAssertion)))));
    }

    @Override
    public JsonValue visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException("Extended match filter not supported on this endpoint");
    }

    @Override
    public JsonValue visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"gt" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(field.toString()),
                        object(field("gt", valueAssertion))
                ))
        )));
    }

    @Override
    public JsonValue visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"gte" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(field.toString()),
                        object(field("gte", valueAssertion))
                ))
        )));
    }

    @Override
    public JsonValue visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"lt" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(field.toString()),
                        object(field("lt", valueAssertion))
                ))
        )));
    }

    @Override
    public JsonValue visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "range" : { "field" : {"lte" : 5 } }
        return json(object(field("range",
                object(field(jsonPointerToDotNotation(field.toString()),
                        object(field("lte", valueAssertion))
                ))
        )));
    }

    @Override
    public JsonValue visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
        // "bool" : { "must_not" : { subFilter... } }
        return json(object(field("bool",
                object(field("must_not", subFilter.accept(this, aVoid).getObject()))
        )));
    }

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

    @Override
    public JsonValue visitPresentFilter(Void aVoid, JsonPointer field) {
        // "exists" : { "field" : "fieldValue" }
        return json(object(field("exists", object(field("field", jsonPointerToDotNotation(field.toString()))))));
    }

    @Override
    public JsonValue visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        // "prefix" : { "field" : "prefixValue" }
        return json(object(field("prefix", object(field(jsonPointerToDotNotation(field.toString()), valueAssertion)))));
    }
}
