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
package org.forgerock.audit.handlers.jdbc;

import static org.forgerock.util.Utils.joinAsString;

import java.util.List;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.util.Utils;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * An abstract {@link QueryFilterVisitor} to produce SQL using an {@link StringSQLRenderer}.
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
 * The implementer is responsible for implementing {@link #visitValueAssertion(Object, String, org.forgerock.json.JsonPointer, Object)}
 * which handles the value assertions - x operand y for the standard operands.  The implementer is also responsible for
 * implementing {@link #visitPresentFilter(Object, org.forgerock.json.JsonPointer)} as "field present" can vary
 * by database implementation (though typically "field IS NOT NULL" is chosen).
 */
public class StringSQLQueryFilterVisitor
        extends AbstractSQLQueryFilterVisitor<StringSQLRenderer, TableMappingAndParameters> {

    // key/value number for each key/value placeholder
    int objectNumber = 0;

    public StringSQLRenderer visitCompositeFilter(final TableMappingAndParameters parameters,
            List<QueryFilter<JsonPointer>> subFilters, String operand) {
        final String operandDelimiter = new StringBuilder(" ").append(operand).append(" ").toString();
        return new StringSQLRenderer("(")
                .append(joinAsString(
                        operandDelimiter,
                        FluentIterable.from(subFilters)
                                .transform(new Function<QueryFilter<JsonPointer>, String>() {
                                    @Override
                                    public String apply(QueryFilter<JsonPointer> filter) {
                                        return filter.accept(StringSQLQueryFilterVisitor.this, parameters).toSQL();
                                    }
                                })))
                .append(")");
    }

    @Override
    public StringSQLRenderer visitAndFilter(TableMappingAndParameters parameters,
            List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "AND");
    }

    @Override
    public StringSQLRenderer visitOrFilter(TableMappingAndParameters parameters,
            List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "OR");
    }
    @Override
    public StringSQLRenderer visitBooleanLiteralFilter(TableMappingAndParameters parameters, boolean value) {
        return new StringSQLRenderer(value ? "1 = 1" : "1 <> 1");
    }

    @Override
    public StringSQLRenderer visitNotFilter(TableMappingAndParameters parameters, QueryFilter<JsonPointer> subFilter) {
        return new StringSQLRenderer("NOT ")
                .append(subFilter.accept(this, parameters).toSQL());
    }

    @Override
    public StringSQLRenderer visitValueAssertion(TableMappingAndParameters parameters, String operand, JsonPointer field,
            Object valueAssertion) {
        ++objectNumber;
        String value = "${v"+objectNumber + "}";
        parameters.getParameters().put(value, new TableMappingAndParameters.FieldValuePair(field, valueAssertion));
        return new StringSQLRenderer(parameters.getColumnName(field) + " " + operand + " " + value);
    }

    @Override
    public StringSQLRenderer visitPresentFilter(TableMappingAndParameters parameters, JsonPointer field) {
        return new StringSQLRenderer(parameters.getColumnName(field) + " IS NOT NULL");
    }
}
