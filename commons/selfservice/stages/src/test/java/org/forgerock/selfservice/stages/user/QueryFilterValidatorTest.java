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
package org.forgerock.selfservice.stages.user;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for {@link QueryFilterValidator}.
 *
 * @since 0.5.0
 *
 */
public final class QueryFilterValidatorTest {

    private static final QueryFilterVisitor<Boolean, Set<JsonPointer>,
            JsonPointer> MAP_FILTER_VISITOR = new QueryFilterValidator();

    private static final Set<JsonPointer> WHITE_LIST = new HashSet<>(
            Arrays.asList(
                    new JsonPointer("/name"),
                    new JsonPointer("/admin/0"),
                    new JsonPointer("age"),
                    new JsonPointer("balance"),
                    new JsonPointer("role")));

    @DataProvider
    public Object[][] getTestData() {
        return new Object[][] {
                // @Checkstyle:off
                { "true", false },
                { "false", false },
                { "/name eq 'alice'", true },
                { "name eq 'alice'", true },
                { "/x/0/name eq 'alice'", false },
                { "name_Invalid eq 'alice'", false },
                { "admin/0 eq 'alice'", true },
                { "/age eq 1234", true },
                { "/balance eq 3.14159", true },
                { "/age lt 20", false },
                { "/age le 20", false },
                { "/name co 'al'", false },
                { "/name sw 'al'", false },
                { "/name_Invalid sw 'al'", false },
                { "/name pr", false },
                { "(/age lt 18 or /age gt 30)", false },
                { "(/age lt 18 or /age_Invalid gt 30)", false },
                { "(/age lt 18 and /age gt 30)", false },
                { "(/age lt 18 and /age_Invalid gt 30)", false },
                { "(/role eq 'a' and (/role eq 'b' or /role eq 'c'))", true },
                { "(/role_Invalid eq 'a' and (/role eq 'b' or /role eq 'c'))", false },
                { "! (/age eq 1234)", true },
                { "! (age eq 1234)", true },
                { "! (/age_Invalid eq 1234)", false },
                { "/name regex 'al.*'", false },
                { "/name_Invalid regex 'al.*'", false }
                // @Checkstyle:on
        };
    }

    @Test(dataProvider = "getTestData")
    public void testToMap(String queryFilter, Boolean expectedResult) throws Exception {
        // Given
        QueryFilter<JsonPointer> filter = QueryFilters.parse(queryFilter);

        // When
        Boolean actualResult = filter.accept(MAP_FILTER_VISITOR, WHITE_LIST);

        // Then
        assertEquals(actualResult, expectedResult);
    }

}
