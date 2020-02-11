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

package org.forgerock.api.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.forgerock.api.ApiValidationException;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.QueryType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class QueryTest {

    @DataProvider(name = "buildValidationData")
    public Object[][] buildValidationData() {
        return new Object[][]{
                // invalid
                {null, null, null, ApiValidationException.class},
                {QueryType.ID, null, null, ApiValidationException.class},

                // valid
                {QueryType.ID, "id", null, null},
                {QueryType.FILTER, null, new String[]{"field"}, null},
                {QueryType.EXPRESSION, null, null, null},
                {QueryType.FILTER, null, null, null},
        };
    }

    @Test(dataProvider = "buildValidationData")
    public void testBuildValidation(final QueryType type, final String queryId, final String[] queryableFields,
            final Class<? extends Throwable> expectedException) {
        final Query.Builder builder = Query.query();
        final Query query;
        try {
            if (type != null) {
                builder.type(type);
            }
            if (queryId != null) {
                builder.queryId(queryId);
            }
            if (queryableFields != null) {
                builder.queryableFields(queryableFields);
            }
            query = builder.build();
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
            return;
        }

        if (expectedException != null) {
            failBecauseExceptionWasNotThrown(expectedException);
        }

        assertThat(query.getType()).isEqualTo(type);
    }

    @Test
    public void testQueryBuilder() {
        final Query query = Query.query()
                .type(QueryType.EXPRESSION)
                .pagingModes(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicies(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .queryableFields("field1", "field2")
                .supportedSortKeys("key1", "key2")
                .build();

        assertThat(query.getType()).isEqualTo(QueryType.EXPRESSION);
        assertThat(query.getPagingModes()).contains(PagingMode.COOKIE, PagingMode.OFFSET);
        assertThat(query.getCountPolicies()).contains(CountPolicy.EXACT, CountPolicy.ESTIMATE);
        assertThat(query.getQueryableFields()).contains("field1", "field2");
        assertThat(query.getSupportedSortKeys()).contains("key1", "key2");
    }

}
