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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.resource.QueryFilter.alwaysFalse;
import static org.forgerock.json.resource.QueryFilter.alwaysTrue;
import static org.forgerock.json.resource.QueryFilter.and;
import static org.forgerock.json.resource.QueryFilter.equalTo;
import static org.forgerock.json.resource.QueryFilter.extendedMatch;
import static org.forgerock.json.resource.QueryFilter.greaterThan;
import static org.forgerock.json.resource.QueryFilter.greaterThanOrEqualTo;
import static org.forgerock.json.resource.QueryFilter.lessThan;
import static org.forgerock.json.resource.QueryFilter.lessThanOrEqualTo;
import static org.forgerock.json.resource.QueryFilter.not;
import static org.forgerock.json.resource.QueryFilter.or;
import static org.forgerock.json.resource.QueryFilter.present;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the QueryFilter class.
 */
@SuppressWarnings("javadoc")
public final class QueryFilterTest {
    @DataProvider
    public Object[][] toStringData() {
        return new Object[][] {
            // @formatter:off
            { alwaysTrue(), "true" },
            { alwaysFalse(), "false" },
            { equalTo("/name", "alice"), "/name eq \"alice\""},
            { equalTo("/age", 1234), "/age eq 1234" },
            { equalTo("/isAdmin", false), "/isAdmin eq false" },
            { lessThan("/age", 1234), "/age lt 1234" },
            { lessThanOrEqualTo("/age", 1234), "/age le 1234" },
            { greaterThan("/age", 1234), "/age gt 1234" },
            { greaterThanOrEqualTo("/age", 1234), "/age ge 1234" },
            { extendedMatch("/name", "regex", "al.*"), "/name regex \"al.*\"" },
            { present("/name"), "/name pr" },
            { or(), "false" }, // zero operand or is always false
            { and(), "true" }, // zero operand and is always true
            { or(equalTo("/age", 1234)), "/age eq 1234" }, // single operand or is no-op
            { and(equalTo("/age", 1234)), "/age eq 1234" }, // single operand and is no-op
            { or(lessThan("/age", 18), greaterThan("/age", 30)), "(/age lt 18 or /age gt 30)" },
            { and(lessThan("/age", 18), greaterThan("/age", 30)), "(/age lt 18 and /age gt 30)" },
            { or(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "(/role eq \"a\" or /role eq \"b\" or /role eq \"c\")" },
            { and(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                "(/role eq \"a\" and /role eq \"b\" and /role eq \"c\")" },
            { or(equalTo("/role", "a"), and(equalTo("/role", "b"), equalTo("/role", "c"))),
                "(/role eq \"a\" or (/role eq \"b\" and /role eq \"c\"))" },
            { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                "(/role eq \"a\" and (/role eq \"b\" or /role eq \"c\"))" },
            { not(equalTo("/age", 1234)), "nt /age eq 1234" },
            { not(not(equalTo("/age", 1234))), "nt nt /age eq 1234" }, // Yuk!
            // @formatter:on
        };
    }

    @Test(dataProvider = "toStringData")
    public void testToString(QueryFilter filter, String expected) {
        assertThat(filter.toString()).isEqualTo(expected);
    }
}
