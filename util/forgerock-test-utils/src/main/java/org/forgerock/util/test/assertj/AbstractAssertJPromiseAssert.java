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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.util.test.assertj;

import java.util.concurrent.ExecutionException;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.forgerock.util.promise.Promise;

/**
 * Assertion class for a promise. Allows verification of the value that was completed with.
 * @param <T> The promised type.
 * @param <A> The type of assert that this class is.
 * @param <S> The type of assert that is returned from the succeeded method.
 */
//@Checkstyle:ignoreFor 2
public abstract class AbstractAssertJPromiseAssert<T, A extends AbstractAssertJPromiseAssert<T, A, S>, S extends AbstractAssert<S, T>>
        extends AbstractAssert<A, Promise<T, ?>> {

    /**
     * Constructs a new assertion on promise.
     * @param promise the actual promise to check
     * @param type the type of assertion
     */
    protected AbstractAssertJPromiseAssert(Promise<T, ?> promise, Class<A> type) {
        super(promise, type);
    }

    /**
     * Factory method for the succeeded assert class.
     * @param actual The promised value.
     * @return The {@link AbstractAssert} implementation.
     */
    protected abstract S createSucceededAssert(T actual);

    /**
     * Asserts that the promise succeeded.
     * @return An {@link AbstractAssert} for making assertions on the promise's completed value.
     */
    public final S succeeded() {
        isNotNull();
        if (!actual.isDone()) {
            failWithMessage("Promise is not completed");
        }
        T result = null;
        try {
            result = actual.get();
        } catch (InterruptedException e) {
            failWithMessage("Promise was interrupted");
        } catch (ExecutionException e) {
            failWithMessage("Promise failed: <%s>", e.getCause());
        }
        return createSucceededAssert(result);
    }

    /**
     * Asserts that the promise failed.
     * @return A {@link org.assertj.core.api.ThrowableAssert} for making
     * assertions on the promise's failure cause.
     */
    public final AbstractThrowableAssert<?, ? extends Throwable> failedWithException() {
        isNotNull();
        try {
            Object value = actual.get();
            failWithMessage("Promise succeeded with value <%s>", value);
        } catch (InterruptedException e) {
            failWithMessage("Promise was interrupted");
        } catch (ExecutionException e) {
            return Assertions.assertThat(e.getCause());
        }
        throw new IllegalStateException("Shouldn't have reached here");
    }

}
