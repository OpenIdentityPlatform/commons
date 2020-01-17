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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.jdbc;

import static org.forgerock.util.Utils.joinAsString;

import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * An abstract {@link QueryFilterVisitor} to produce SQL using an {@link StringSqlRenderer}.
 * Includes patterns for the standard
 *
 * <ul>
 *     <li>AND</li>
 *     <li>OR</li>
 *     <li>NOT</li>
 *     <li>&gt;=</li>
 *     <li>&gt;</li>
 *     <li>=</li>
 *     <li>&lt;</li>
 *     <li>&lt;=</li>
 * </ul>
 * operators, along with the following implementations for {@link QueryFilter}'s
 * <ul>
 *     <li>contains : field LIKE '%value%'</li>
 *     <li>startsWith : field LIKE 'value%'</li>
 *     <li>literal true : 1 = 1</li>
 *     <li>literal false : 1 &lt;&gt; 1</li>
 * </ul>
 * <p>
 * This implementation does not support extended-match.
 * <p>
 * The implementer is responsible for implementing
 * {@link #visitValueAssertion(Object, String, org.forgerock.json.JsonPointer, Object)}
 * which handles the value assertions - x operand y for the standard operands.  The implementer is also responsible for
 * implementing {@link #visitPresentFilter(Object, org.forgerock.json.JsonPointer)} as "field present" can vary
 * by database implementation (though typically "field IS NOT NULL" is chosen).
 */
class StringSqlQueryFilterVisitor
        extends AbstractSqlQueryFilterVisitor<StringSqlRenderer, TableMappingParametersPair> {

    // key/value number for each key/value placeholder
    int objectNumber = 0;

    private StringSqlRenderer visitCompositeFilter(final TableMappingParametersPair parameters,
            List<QueryFilter<JsonPointer>> subFilters, String operand) {
        final String operandDelimiter = new StringBuilder(" ").append(operand).append(" ").toString();
        final List<String> subFilterValues = new LinkedList<>();
        for (QueryFilter<JsonPointer> subFilter : subFilters) {
            subFilterValues.add(subFilter.accept(StringSqlQueryFilterVisitor.this, parameters).toSql());
        }
        return new StringSqlRenderer("(").append(joinAsString(operandDelimiter, subFilterValues)).append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitAndFilter(TableMappingParametersPair parameters,
            List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "AND");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitOrFilter(TableMappingParametersPair parameters,
            List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "OR");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitBooleanLiteralFilter(TableMappingParametersPair parameters, boolean value) {
        return new StringSqlRenderer(value ? "1 = 1" : "1 <> 1");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitNotFilter(TableMappingParametersPair parameters, QueryFilter<JsonPointer> subFilter) {
        return new StringSqlRenderer("NOT ")
                .append(subFilter.accept(this, parameters).toSql());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitValueAssertion(TableMappingParametersPair parameters, String operand,
            JsonPointer field, Object valueAssertion) {
        String value = "${" + field.toString() + "}";
        parameters.getParameters().put(field.toString(), valueAssertion);
        return new StringSqlRenderer(parameters.getColumnName(field) + " " + operand + " " + value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringSqlRenderer visitPresentFilter(TableMappingParametersPair parameters, JsonPointer field) {
        return new StringSqlRenderer(parameters.getColumnName(field) + " IS NOT NULL");
    }
}
