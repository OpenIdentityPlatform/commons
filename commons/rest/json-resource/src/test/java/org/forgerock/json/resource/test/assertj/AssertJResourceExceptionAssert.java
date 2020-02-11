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

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;

/**
 * AssertJ asserter for ResourceExceptions.
 */
public class AssertJResourceExceptionAssert extends
        AbstractThrowableAssert<AssertJResourceExceptionAssert, ResourceException> {

    /**
     * Creates a new {@link AssertJResourceExceptionAssert}.
     *
     * @param actual the actual value to verify.
     */
    private AssertJResourceExceptionAssert(ResourceException actual) {
        super(actual, AssertJResourceExceptionAssert.class);
    }

    /**
     * Assert the {@link ResourceException} instance.
     * @param exception The resource exception to make assertions on.
     * @return An assert object.
     */
    public static AssertJResourceExceptionAssert assertThat(ResourceException exception) {
        return new AssertJResourceExceptionAssert(exception);
    }

    /**
     * Returns an asserter for asserting the contained {@literal code}.
     * @return This {@code AssertJResourceExceptionAssert}.
     */
    public AssertJResourceExceptionAssert withCode(int code) {
        Assertions.assertThat(actual.getCode()).isEqualTo(code);
        return this;
    }

    /**
     * Returns an asserter for asserting the contained {@literal reason}.
     * @return This {@code AssertJResourceExceptionAssert}.
     */
    public AssertJResourceExceptionAssert withReason(String reason) {
        Assertions.assertThat(actual.getReason()).isEqualTo(reason);
        return this;
    }

    /**
     * Returns an asserter for asserting the contained {@code JsonValue} detail.
     * @return A {@code AbstractJsonValueAssert} for this {@code ResourceException}.
     */
    public AssertJJsonValueAssert.AbstractJsonValueAssert<?> withDetail() {
        return AssertJJsonValueAssert.assertThat(actual.getDetail());
    }

    /**
     * Returns an asserter for asserting the contained {@literal resource version}.
     * @return This {@code AssertJResourceExceptionAssert}.
     */
    public AssertJResourceExceptionAssert withVersion(Version version) {
        Assertions.assertThat(actual.getResourceApiVersion()).isEqualTo(version);
        return this;
    }

    /**
     * Returns an asserter for asserting the contained {@literal cause}.
     * @return A {@code AbstractJsonValueAssert} for this {@code ResourceException}.
     */
    public AbstractThrowableAssert<?, ? extends Throwable> withCause() {
        return Assertions.assertThat(actual.getCause());
    }
}
