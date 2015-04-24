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
import org.assertj.core.api.Assertions;
import org.assertj.core.api.StringAssert;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.test.fest.FestJsonValueAssert;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;

/**
 * Offers AssertJ assertions for {@link Resource} instances.
 */
public class AssertJResourceAssert extends AbstractAssert<AssertJResourceAssert, Resource> {

    /**
     * Assert the value of the provided {@link Resource} instance.
     * @param resource The instance to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourceAssert assertThat(Resource resource) {
        return new AssertJResourceAssert(resource);
    }

    /**
     * Assert the promise of a {@link Resource} instance.
     * @param promise The promise to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourcePromiseAssert assertThat(Promise<Resource, ?> promise) {
        return new AssertJResourcePromiseAssert(promise);
    }

    public static class AssertJResourcePromiseAssert
            extends AbstractAssertJPromiseAssert<Resource, AssertJResourcePromiseAssert, AssertJResourceAssert> {

        protected AssertJResourcePromiseAssert(Promise<Resource, ?> promise) {
            super(promise, AssertJResourcePromiseAssert.class);
        }

        @Override
        protected AssertJResourceAssert createSucceededAssert(Resource resource) {
            return new AssertJResourceAssert(resource);
        }
    }

    /**
     * Creates a new {@link AssertJResourceAssert}.
     *
     * @param actual   the actual value to verify.
     */
    private AssertJResourceAssert(Resource actual) {
        super(actual, AssertJResourceAssert.class);
    }

    /**
     * Assert the value of the resource ID.
     * @return A {@code StringAssert} for this Resource's ID.
     */
    public StringAssert withId() {
        return Assertions.assertThat(actual.getId());
    }

    /**
     * Assert the value of the resource revision.
     * @return A {@code StringAssert} for this Resource's revision.
     */
    public StringAssert withRevision() {
        return Assertions.assertThat(actual.getRevision());
    }

    /**
     * Assert the value of the resource ID.
     * @return A {@code StringAssert} for this Resource's ID.
     */
    public FestJsonValueAssert.AbstractJsonValueAssert withContent() {
        return FestJsonValueAssert.assertThat(actual.getContent());
    }
}
