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
package org.forgerock.json.resource.test.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;

public class AssertJQueryResponseAssert extends AbstractAssert<AssertJQueryResponseAssert, QueryResponse> {

    /**
     * Assert the value of the provided {@link ResourceResponse} instance.
     * @param resource The instance to make assertions on.
     * @return An assert object.
     */
    public static AssertJQueryResponseAssert assertThat(QueryResponse resource) {
        return new AssertJQueryResponseAssert(resource);
    }

    /**
     * Assert the promise of a {@link ResourceResponse} instance.
     * @param promise The promise to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourcePromiseAssert assertThat(Promise<QueryResponse, ?> promise) {
        return new AssertJResourcePromiseAssert(promise);
    }

    public static class AssertJResourcePromiseAssert extends AbstractAssertJPromiseAssert<QueryResponse, AssertJResourcePromiseAssert, AssertJQueryResponseAssert> {

        protected AssertJResourcePromiseAssert(Promise<QueryResponse, ?> promise) {
            super(promise, AssertJResourcePromiseAssert.class);
        }

        @Override
        protected AssertJQueryResponseAssert createSucceededAssert(QueryResponse resource) {
            return new AssertJQueryResponseAssert(resource);
        }
    }

    /**
     * Creates a new {@link AssertJResourceResponseAssert}.
     *
     * @param actual   the actual value to verify.
     */
    private AssertJQueryResponseAssert(QueryResponse actual) {
        super(actual, AssertJQueryResponseAssert.class);
    }

    /**
     * Assert the value of the resource ID.
     * @return A {@code AbstractCharSequenceAssert} for this Resource's ID.
     */
    public AbstractObjectAssert<?, CountPolicy> withTotalPagedResultsPolicy() {
        return Assertions.assertThat(actual.getTotalPagedResultsPolicy());
    }

    /**
     * Assert the value of the resource revision.
     * @return A {@code AbstractCharSequenceAssert} for this Resource's revision.
     */
    public AbstractCharSequenceAssert<?, String> withPagedResultsCookie() {
        return Assertions.assertThat(actual.getPagedResultsCookie());
    }

    /**
     * Assert the value of the resource content.
     * @return A {@code AbstractJsonValueAssert} for this Resource's content.
     */
    public AbstractIntegerAssert<?> withTotalPagedResults() {
        return Assertions.assertThat(actual.getTotalPagedResults());
    }
}
