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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;

import java.util.concurrent.ExecutionException;

/**
 * Offers AssertJ assertions for {@link QueryResponse} instances.
 */
public class AssertJQueryResponseAssert extends AbstractAssert<AssertJQueryResponseAssert, QueryResponse> {

    /**
     * Assert the value of the provided {@link QueryResponse} instance.
     * @param resource The instance to make assertions on.
     * @return An assert object.
     */
    public static AssertJQueryResponseAssert assertThat(QueryResponse resource) {
        return new AssertJQueryResponseAssert(resource);
    }

    /**
     * Assert the promise of a {@link QueryResponse} instance.
     * @param promise The promise to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourcePromiseAssert assertThat(Promise<QueryResponse, ?> promise) {
        return new AssertJResourcePromiseAssert(promise);
    }

    public static class AssertJResourcePromiseAssert extends AbstractAssertJPromiseAssert<QueryResponse,
            AssertJResourcePromiseAssert, AssertJQueryResponseAssert> {

        protected AssertJResourcePromiseAssert(Promise<QueryResponse, ?> promise) {
            super(promise, AssertJResourcePromiseAssert.class);
        }

        @Override
        protected AssertJQueryResponseAssert createSucceededAssert(QueryResponse resource) {
            return new AssertJQueryResponseAssert(resource);
        }

        /**
         * Asserts that the promise failed.
         * @return A {@link AssertJResourceExceptionAssert} for making
         * assertions on the promise's resource exception.
         */
        public AssertJResourceExceptionAssert failedWithResourceException() {
            isNotNull();
            try {
                Object value = actual.get();
                failWithMessage("Promise succeeded with value <%s>", value);
            } catch (InterruptedException e) {
                failWithMessage("Promise was interrupted");
            } catch (ExecutionException e) {
                return AssertJResourceExceptionAssert.assertThat((ResourceException) e.getCause());
            }
            throw new IllegalStateException("Shouldn't have reached here");
        }
    }

    /**
     * Creates a new {@link AssertJQueryResponseAssert}.
     *
     * @param actual the actual value to verify.
     */
    private AssertJQueryResponseAssert(QueryResponse actual) {
        super(actual, AssertJQueryResponseAssert.class);
    }

    /**
     * Assert the value of the total paged results policy
     * @return A {@code AbstractObjectAssert} for the total paged results policy.
     */
    public AbstractObjectAssert<?, CountPolicy> withTotalPagedResultsPolicy() {
        return Assertions.assertThat(actual.getTotalPagedResultsPolicy());
    }

    /**
     * Assert the value of the paged results cookie.
     * @return A {@code AbstractCharSequenceAssert} for the paged results cookie.
     */
    public AbstractCharSequenceAssert<?, String> withPagedResultsCookie() {
        return Assertions.assertThat(actual.getPagedResultsCookie());
    }

    /**
     * Assert the value of the total paged results.
     * @return A {@code AbstractIntegerAssert} for the total paged results.
     */
    public AbstractIntegerAssert<?> withTotalPagedResults() {
        return Assertions.assertThat(actual.getTotalPagedResults());
    }
}
