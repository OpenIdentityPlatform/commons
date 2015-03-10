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

package org.forgerock.util.test.fest;

import java.util.concurrent.ExecutionException;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import org.fest.assertions.ThrowableAssert;
import org.forgerock.util.promise.Promise;

/**
 * Assertion class for a promise. Allows verification of the value that was completed with.
 */
public abstract class AbstractFestPromiseAssert<T, A extends AbstractFestPromiseAssert<T, A, S>, S extends GenericAssert<S, T>>
        extends GenericAssert<A, Promise<T, ?>> {

    protected AbstractFestPromiseAssert(Promise<T, ?> promise, Class<A> type) {
        super(type, promise);
    }

    /**
     * Factory method for the succeeded assert class.
     * @param actual The promised value.
     * @return The {@link GenericAssert} implementation.
     */
    protected abstract S createSucceededAssert(T actual);

    /**
     * Asserts that the promise succeeded.
     * @return A {@link GenericAssert} for making assertions on the promise's completed value.
     */
    public final S succeeded() {
        isNotNull();
        if (!actual.isDone()) {
            fail("Promise is not completed");
        }
        T result = null;
        try {
            result = actual.get();
        } catch (InterruptedException e) {
            fail("Promise was interrupted");
        } catch (ExecutionException e) {
            fail("Promise failed", e.getCause());
        }
        return createSucceededAssert(result);
    }

    /**
     * Asserts that the promise failed.
     * @return A {@link org.assertj.core.api.ThrowableAssert} for making
     * assertions on the promise's failure cause.
     */
    public final ThrowableAssert failedWithException() {
        isNotNull();
        try {
            Object value = actual.get();
            fail("Promise succeeded with value " + value);
        } catch (InterruptedException e) {
            fail("Promise was interrupted");
        } catch (ExecutionException e) {
            return Assertions.assertThat(e.getCause());
        }
        throw new IllegalStateException("Shouldn't have reached here");
    }

}
