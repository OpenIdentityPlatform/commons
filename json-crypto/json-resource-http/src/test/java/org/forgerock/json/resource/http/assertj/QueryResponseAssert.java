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

package org.forgerock.json.resource.http.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.resource.QueryResponse;

/**
 * Assertion methods for {@link QueryResponse}s.
 *
 * <p>To create a new instance of this class, invoke
 * <code>{@link org.forgerock.json.resource.http.Assertions#assertThat(QueryResponse) assertThat}(actual)</code>.
 */
@SuppressWarnings("javadoc")
public class QueryResponseAssert extends ResponseAssert<QueryResponseAssert, QueryResponse> {

    public QueryResponseAssert(final QueryResponse actual) {
        super(actual, QueryResponseAssert.class);
    }

    public QueryResponseAssert isEqualTo(final QueryResponse expected) {
        super.isEqualTo(expected);
        assertThat(actual.getPagedResultsCookie()).isEqualTo(expected.getPagedResultsCookie());
        assertThat(actual.getRemainingPagedResults()).isEqualTo(expected.getRemainingPagedResults());
        assertThat(actual.getTotalPagedResultsPolicy()).isEqualTo(expected.getTotalPagedResultsPolicy());
        assertThat(actual.getTotalPagedResults()).isEqualTo(expected.getTotalPagedResults());
        return myself;
    }
}
