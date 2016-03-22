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

import static org.forgerock.util.query.QueryFilter.*;
import static org.testng.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.Map;

import org.testng.annotations.DataProvider;

public class MapFilterVisitorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        // set ObjectMapper to use single quotes for readability of the json strings below
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @SuppressWarnings("rawtypes")
    private static final QueryFilterVisitor MAP_FILTER_VISITOR = new MapFilterVisitor();

    @DataProvider
    public Object[][] toMapData() {
        // Use json-strings as map representations to test using Jackson ObjectMapper.
        // Use ints for integer values because ObjectMapper deserializes integers as ints and
        // equals() is sensitive to the type.
        return new Object[][] {
            // @formatter:off
            { alwaysTrue(),
                "{ 'operator' : true }" },
            { alwaysFalse(),
                "{ 'operator' : false }" },
            { equalTo("/name", "alice"),
                "{ 'operator' : 'eq', 'field' : '/name', 'value' : 'alice' }"},
            { equalTo("/age", 1234),
                "{ 'operator' : 'eq', 'field' : '/age', 'value' : 1234 }" },
            { equalTo("/balance", 3.14159),
                "{ 'operator' : 'eq', 'field' : '/balance', 'value' : 3.14159 }" },
            { equalTo("/isAdmin", false),
                "{ 'operator' : 'eq', 'field' : '/isAdmin', 'value' : false }" },
            { lessThan("/age", 1234),
                "{ 'operator' : 'lt', 'field' : '/age', 'value' : 1234 }" },
            { lessThanOrEqualTo("/age", 1234),
                "{ 'operator' : 'le', 'field' : '/age', 'value': 1234 }" },
            { greaterThan("/age", 1234),
                "{ 'operator' : 'gt', 'field' : '/age', 'value' : 1234 }" },
            { greaterThanOrEqualTo("/age", 1234),
                "{ 'operator' : 'ge', 'field' : '/age', 'value' : 1234 }" },
            { contains("/name", "al"),
                "{ 'operator' : 'co', 'field' : '/name', 'value' : 'al' }" },
            { startsWith("/name", "al"),
                "{ 'operator' : 'sw', 'field' : '/name', 'value' : 'al' }" },
            { present("/name"),
                "{ 'operator' : 'pr', 'field' : '/name' }" },
            { or(),
                "{ 'operator' : false }" }, // zero operand or is always false
            { and(),
                "{ 'operator' : true }" }, // zero operand and is always true
            { or(equalTo("/age", 1234)),
                "{ 'operator' : 'eq', 'field' : '/age', 'value' : 1234 }'" }, // single operand or is no-op
            { and(equalTo("/age", 1234)),
                "{ 'operator' : 'eq', 'field' : '/age', 'value' : 1234 }'" }, // single operand and is no-op
            { or(lessThan("/age", 18), greaterThan("/age", 30)),
                "{ 'operator' : 'or', 'subfilters' : ["
                        + "{ 'operator' : 'lt', 'field' : '/age', 'value' : 18 },"
                        + "{ 'operator' : 'gt', 'field' : '/age', 'value' : 30 } "
                    + "] "
                    + "}" },
            { and(lessThan("/age", 18), greaterThan("/age", 30)),
                "{ 'operator' : 'and', 'subfilters' : ["
                        + "{ 'operator' : 'lt', 'field' : '/age', 'value' : 18 },"
                        + "{ 'operator' : 'gt', 'field' : '/age', 'value' : 30 } "
                    + "] "
                    + "}" },
            { and(lessThan("/age", 18), greaterThan("/age", 30), startsWith("/name", "bill")), // 3-element AND
                "{ 'operator' : 'and', 'subfilters' : ["
                        + "{ 'operator' : 'lt', 'field' : '/age', 'value' : 18 },"
                        + "{ 'operator' : 'gt', 'field' : '/age', 'value' : 30 },"
                        + "{ 'operator' : 'sw', 'field' : '/name', 'value' : 'bill' } "
                    + "] "
                    + "}" },
            { or(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "{ 'operator' : 'or', 'subfilters' : ["
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'a' },"
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'b' },"
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'c' } "
                    + "] "
                    + "}" },
            { and(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "{ 'operator' : 'and', 'subfilters' : ["
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'a' },"
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'b' },"
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'c' } "
                    + "] "
                    + "}" },
            { or(equalTo("/role", "a"), and(equalTo("/role", "b"), equalTo("/role", "c"))),
                "{ 'operator' : 'or', 'subfilters' : ["
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'a' },"
                        + "{ 'operator' : 'and', 'subfilters' : ["
                                + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'b' },"
                                + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'c' } "
                            + "] "
                        + "} "
                    + "] "
                    + "}" },
            { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                "{ 'operator' : 'and', 'subfilters' : ["
                        + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'a' },"
                        + "{ 'operator' : 'or', 'subfilters' : ["
                                + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'b' },"
                                + "{ 'operator' : 'eq', 'field' : '/role', 'value' : 'c' } "
                            + "] "
                        + "} "
                    + "] "
                    + "}" },
            { not(equalTo("/age", 1234)),
                "{ 'operator' : '!', 'subfilter' : "
                        + "{ 'operator' : 'eq', 'field' : '/age', 'value' : 1234 } "
                    + "}" },
            { not(not(equalTo("/age", 1234))),
                "{ 'operator' : '!', 'subfilter' : "
                    + "{ 'operator' : '!', 'subfilter' : "
                        + "{ 'operator' : 'eq', 'field' : '/age', 'value' : 1234 } "
                    + "} "
                    + "}" },
            { extendedMatch("/name", "regex", "al.*"),
                "{ 'operator' : 'regex', 'field' : '/name', 'value' : 'al.*' }" },
            { extendedMatch("/name", "eq", "alice"),
                "{ 'operator' : 'eq', 'field' : '/name', 'value' : 'alice' }" },

            // test value assertion types
            { equalTo("/enabled", true),
                "{ 'operator' : 'eq', 'field' : '/enabled', 'value' : true }"},
            { equalTo("/optional", null),
                "{ 'operator' : 'eq', 'field' : '/optional', 'value' : null }"}
            // @formatter:on
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test(dataProvider = "toMapData")
    public void testToMap(QueryFilter filter, String jsonString) throws Exception {
        assertEquals(filter.accept(MAP_FILTER_VISITOR, null), MAPPER.readValue(jsonString, Map.class));
    }

}
