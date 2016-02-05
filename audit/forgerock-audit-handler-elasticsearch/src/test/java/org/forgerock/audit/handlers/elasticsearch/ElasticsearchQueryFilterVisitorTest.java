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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.events.AuditEventHelper.jsonPointerToDotNotation;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.contains;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.extendedMatch;
import static org.forgerock.util.query.QueryFilter.greaterThan;
import static org.forgerock.util.query.QueryFilter.greaterThanOrEqualTo;
import static org.forgerock.util.query.QueryFilter.lessThan;
import static org.forgerock.util.query.QueryFilter.lessThanOrEqualTo;
import static org.forgerock.util.query.QueryFilter.not;
import static org.forgerock.util.query.QueryFilter.or;
import static org.forgerock.util.query.QueryFilter.present;
import static org.forgerock.util.query.QueryFilter.startsWith;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticsearchQueryFilterVisitorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final JsonPointer FIELD_1 = new JsonPointer("field1");
    private static final JsonPointer FIELD_2 = new JsonPointer("field2");

    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";
    private static final String EM_OPERATOR = "em";

    @Test
    public void testAndFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter =
                and(equalTo(FIELD_1, VALUE_1), equalTo(FIELD_2, VALUE_2));
        final JsonValue expectedQueryStatement =
                json(object(field("bool",
                        object(field("must",
                                array(
                                        object(field("term", object(field(jsonPointerToDotNotation(FIELD_1.toString()), VALUE_1)))),
                                        object(field("term", object(field(jsonPointerToDotNotation(FIELD_2.toString()), VALUE_2))))
                                )
                        ))
                )));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void testOrFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter =
                or(equalTo(FIELD_1, VALUE_1), equalTo(FIELD_2, VALUE_2));
        final JsonValue expectedQueryStatement =
                json(object(field("bool",
                        object(field("should",
                                array(
                                        object(field("term", object(field(jsonPointerToDotNotation(FIELD_1.toString()), VALUE_1)))),
                                        object(field("term", object(field(jsonPointerToDotNotation(FIELD_2.toString()), VALUE_2))))
                                )
                        ))
                )));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void testNotFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = not(equalTo(FIELD_1, VALUE_1));
        final JsonValue expectedQueryStatement =
                json(object(field("bool",
                        object(field("must_not",
                                object(field("term", object(field(jsonPointerToDotNotation(FIELD_1.toString()), VALUE_1))))
                        ))
                )));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void testPresentFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = present(FIELD_1);
        final JsonValue expectedQueryStatement =
                json(object(field("exists", object(field("field", jsonPointerToDotNotation(FIELD_1.toString()))))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void testStartsWithFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = startsWith(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("prefix", object(field(jsonPointerToDotNotation(FIELD_1.toString()), VALUE_1)))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void TestLessThanOrEqualToFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = lessThanOrEqualTo(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("range",
                        object(field(jsonPointerToDotNotation(FIELD_1.toString()),
                                object(field("lte", VALUE_1)))))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void TestLessThanFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = lessThan(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("range",
                        object(field(jsonPointerToDotNotation(FIELD_1.toString()),
                                object(field("lt", VALUE_1)))))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void TestGreaterThanOrEqualToFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = greaterThanOrEqualTo(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("range",
                                object(field(jsonPointerToDotNotation(FIELD_1.toString()),
                                        object(field("gte", VALUE_1)))))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void TestGreaterThanFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = greaterThan(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("range",
                        object(field(jsonPointerToDotNotation(FIELD_1.toString()),
                                object(field("gt", VALUE_1)))))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void TestExtendedMatchFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = extendedMatch(FIELD_1, EM_OPERATOR, VALUE_1);

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);
    }

    @Test
    public void TestEqualsFilter() {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = equalTo(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field("term", object(field(jsonPointerToDotNotation(FIELD_1.toString()), VALUE_1)))));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
    public void TestContainsFilter() throws Exception {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = contains(FIELD_1, VALUE_1);
        final JsonValue expectedQueryStatement =
                json(object(field(
                                        "wildcard",
                                        object(field(
                                                jsonPointerToDotNotation(FIELD_1.toString()),
                                                "*" + mapper.writeValueAsString(VALUE_1) + "*"
                                        ))
                )));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test
         public void TestBooleanLiteralFilterTrue() throws Exception {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = QueryFilter.alwaysTrue();
        final JsonValue expectedQueryStatement =
                json(object(field("match_all", object())));

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);

        // then
        assertThat(query.asMap()).isEqualTo(expectedQueryStatement.asMap());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void TestBooleanLiteralFilterFalse() throws Exception {
        // given
        final ElasticsearchQueryFilterVisitor elasticsearchQueryFilterVisitor = new ElasticsearchQueryFilterVisitor();
        final QueryFilter<JsonPointer> queryFilter = QueryFilter.alwaysFalse();

        // when
        final JsonValue query = queryFilter.accept(elasticsearchQueryFilterVisitor, null);
    }
}
