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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.util.query.QueryFilter.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class QueryFilterParserTest {

    private QueryFilterParser<String> parser = new QueryFilterParser<String>() {
        @Override
        protected String parseField(String fieldDescription) {
            return fieldDescription;
        }
    };

    @DataProvider
    public Object[][] toIllegalStringData() {
        return new Object[][] {
            // @formatter:off
            { "" },
            { "(" },
            { ")" },
            { "()" },
            { "xxx" },
            { "x and y" },
            { "x or y" },
            { "true and" },
            { "true or" },
            { "true and true and" },
            { "true or false or" },
            { "nt" },
            { "foo eq bar" },   // missing quotes
            { "foo eq \"bar" }, // unmatched quotes
            { "foo eq bar\"" }, // unmatched quotes
            { "foo eq" },       // missing value
            { "foo pr 123" },   // trailing token
            { "true foo" },     // trailing token
            { "name op! 123" }, // bad operator
            { "name op! 123\\" }, // ending with an escape character
            // @formatter:on
        };
    }

    @Test(dataProvider = "toIllegalStringData", expectedExceptions = IllegalArgumentException.class)
    public void testValueOfIllegalStrings(String filterString) throws Exception {
        parser.valueOf(filterString);
    }

    @DataProvider
    public Object[][] toStringData() {
        // Use longs for integer values because valueOf parses integers as Longs and
        // equals() is sensitive to the type.
        return new Object[][] {
            // @formatter:off
            { alwaysTrue(), "true" },
            { alwaysFalse(), "false" },
            { equalTo("/name", "alice"), "/name eq \"alice\""},
            { equalTo("/name", "alice"), "/name eq 'alice'"},
            { equalTo("/name", "al\"ice"), "/name eq \"al\\\"ice\""},
            { equalTo("/name", "al'ice"), "/name eq \"al\'ice\""},
            { equalTo("/name", "al\"ice"), "/name eq 'al\"ice'"},
            { equalTo("/name", "al'ice"), "/name eq 'al\\\'ice'"},
            { equalTo("/name", "\\alice"), "/name eq \"\\\\alice\""},
            { equalTo("/name", "al\nice"), "/name eq \"al\\\nice\""},
            { equalTo("/age", 1234L), "/age eq 1234" },
            { equalTo("/balance", 3.14159), "/balance eq 3.14159" },
            { equalTo("/isAdmin", false), "/isAdmin eq false" },
            { lessThan("/age", 1234L), "/age lt 1234" },
            { lessThanOrEqualTo("/age", 1234L), "/age le 1234" },
            { greaterThan("/age", 1234L), "/age gt 1234" },
            { greaterThanOrEqualTo("/age", 1234L), "/age ge 1234" },
            { contains("/name", "al"), "/name co \"al\"" },
            { contains("/name", "al"), "/name co 'al'" },
            { startsWith("/name", "al"), "/name sw \"al\"" },
            { startsWith("/name", "al"), "/name sw 'al'" },
            { present("/name"), "/name pr" },
            { or(), "false" }, // zero operand or is always false
            { and(), "true" }, // zero operand and is always true
            { or(equalTo("/age", 1234L)), "/age eq 1234" }, // single operand or is no-op
            { and(equalTo("/age", 1234L)), "/age eq 1234" }, // single operand and is no-op
            { or(lessThan("/age", 18L), greaterThan("/age", 30L)), "(/age lt 18 or /age gt 30)" },
            { and(lessThan("/age", 18L), greaterThan("/age", 30L)), "(/age lt 18 and /age gt 30)" },
            { or(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "(/role eq \"a\" or /role eq \"b\" or /role eq \"c\")" },
            { and(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "(/role eq \"a\" and /role eq \"b\" and /role eq \"c\")" },
            { or(equalTo("/role", "a"), and(equalTo("/role", "b"), equalTo("/role", "c"))),
                "(/role eq \"a\" or (/role eq \"b\" and /role eq \"c\"))" },
            { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                "(/role eq \"a\" and (/role eq \"b\" or /role eq \"c\"))" },
            { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                "(/role eq 'a' and (/role eq 'b' or /role eq 'c'))" },
            { not(equalTo("/age", 1234L)), "! (/age eq 1234)" },
            { not(not(equalTo("/age", 1234L))), "! (! (/age eq 1234))" },
            { extendedMatch("/name", "regex", "al.*"), "/name regex \"al.*\"" },
            { extendedMatch("/name", "regex", "al.*"), "/name regex 'al.*'" },
            { equalTo("/name", "alice"), "/name eq \"alice\"" },
            // @formatter:on
        };
    }

    @Test(dataProvider = "toStringData")
    public void testValueOf(QueryFilter<String> filter, String filterString) {
        assertThat(parser.valueOf(filterString)).isEqualTo(filter);
    }

}