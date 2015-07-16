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
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert.AbstractJsonValueAssert;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;

/**
 * Offers AssertJ assertions for {@link ResourceResponse} instances.
 */
public class AssertJJsonValueResponseAssert extends AbstractAssert<AssertJJsonValueResponseAssert, ActionResponse> {

    /**
     * Assert the value of the provided {@link ResourceResponse} instance.
     * @param resource The instance to make assertions on.
     * @return An assert object.
     */
    public static AssertJJsonValueResponseAssert assertThat(ActionResponse resource) {
        return new AssertJJsonValueResponseAssert(resource);
    }

    /**
     * Assert the promise of a {@link ResourceResponse} instance.
     * @param promise The promise to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourcePromiseAssert assertThat(Promise<ActionResponse, ?> promise) {
        return new AssertJResourcePromiseAssert(promise);
    }

    public static class AssertJResourcePromiseAssert
            extends AbstractAssertJPromiseAssert<ActionResponse, AssertJResourcePromiseAssert, AssertJJsonValueResponseAssert> {

        protected AssertJResourcePromiseAssert(Promise<ActionResponse, ?> promise) {
            super(promise, AssertJResourcePromiseAssert.class);
        }

        @Override
        protected AssertJJsonValueResponseAssert createSucceededAssert(ActionResponse resource) {
            return new AssertJJsonValueResponseAssert(resource);
        }
    }

    /**
     * Creates a new {@link AssertJJsonValueResponseAssert}.
     *
     * @param actual the actual value to verify.
     */
    private AssertJJsonValueResponseAssert(ActionResponse actual) {
        super(actual, AssertJJsonValueResponseAssert.class);
    }

    /**
     * Returns an asserter for asserting the contained {@code JsonValue}.
     * @return A {@code AbstractJsonValueAssert} for this {@code ActionResponse}.
     */
    public AbstractJsonValueAssert withContent() {
        return AssertJJsonValueAssert.assertThat(actual.getJsonContent());
    }
}
