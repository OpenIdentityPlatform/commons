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

package org.forgerock.util.test.assertj;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.api.FileAssert;
import org.assertj.core.api.InputStreamAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.LongAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.StringAssert;
import org.assertj.core.api.ThrowableAssert;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

/**
 * Assertion class for a promise. Allows verification of the value that was completed with.
 */
public final class AssertJPromiseAssert
        extends AbstractAssertJPromiseAssert<Object, AssertJPromiseAssert, AssertJPromiseAssert.SuccessfulPromiseAssert> {

    /**
     * Creates an {@code AssertJPromiseAssert} instance for making assertions on a {@link Promise}.
     * @param promise The actual promise instance.
     * @return The {@code AssertJPromiseAssert} instance.
     */
    public static AssertJPromiseAssert assertThat(Promise<?, ?> promise) {
        return new AssertJPromiseAssert(promise);
    }

    private AssertJPromiseAssert(Promise<?, ?> promise) {
        super((Promise<Object, ?>) promise, AssertJPromiseAssert.class,
                new Function<Object, SuccessfulPromiseAssert, RuntimeException>() {
                    @Override
                    public SuccessfulPromiseAssert apply(Object value) throws RuntimeException {
                        return new SuccessfulPromiseAssert(value);
                    }
                });
    }

    /**
     * Asserts that the promise succeeded.
     * @return A {@link org.forgerock.util.test.assertj.AssertJPromiseAssert.SuccessfulPromiseAssert} for making
     * assertions on the promise's completed value.
     */
    public SuccessfulPromiseAssert succeeded() {
        isNotNull();
        if (!actual.isDone()) {
            failWithMessage("Promise is not completed");
        }
        Object result = null;
        try {
            result = actual.get();
        } catch (InterruptedException e) {
            failWithMessage("Promise was interrupted");
        } catch (ExecutionException e) {
            failWithMessage("Promise failed: <%s>", e.getCause());
        }
        return new SuccessfulPromiseAssert(result);
    }

    /**
     * Asserts that the promise failed.
     * @return A {@link org.assertj.core.api.ThrowableAssert} for making
     * assertions on the promise's failure cause.
     */
    public ThrowableAssert failedWithException() {
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

    /**
     * An assertion class for making assertions on the successful completion value of a {@link Promise}.
     */
    public static final class SuccessfulPromiseAssert extends AbstractAssert<SuccessfulPromiseAssert, Object> {

        private SuccessfulPromiseAssert(Object actual) {
            super(actual, SuccessfulPromiseAssert.class);
        }

        /**
         * Asserts that the value was a {@link Map} instance.
         * @param <K> The map key type.
         * @param <V> The map value type.
         * @return A {@link org.assertj.core.api.MapAssert} instance for making assertions on the value.
         */
        public <K, V> MapAssert<K, V> withMap() {
            isInstanceOf(Map.class);
            return Assertions.assertThat((Map<K, V>) actual);
        }

        /**
         * Asserts that the value was a {@link Iterable} instance.
         * @param <T> The iterable contents type.
         * @return A {@link IterableAssert} instance for making assertions on the value.
         */
        public <T> IterableAssert<T> withIterable() {
            isInstanceOf(Iterable.class);
            return Assertions.assertThat((Iterable<T>) actual);
        }

        /**
         * Asserts that the value was a {@link List} instance.
         * @param <T> The list contents type.
         * @return A {@link ListAssert} instance for making assertions on the value.
         */
        public <T> ListAssert<T> withList() {
            isInstanceOf(List.class);
            return Assertions.assertThat((List<T>) actual);
        }

        /**
         * Asserts that the value was a {@link String} instance.
         * @return A {@link StringAssert} instance for making assertions on the value.
         */
        public StringAssert withString() {
            isInstanceOf(String.class);
            return Assertions.assertThat((String) actual);
        }

        /**
         * Asserts that the value was a {@link InputStream} instance.
         * @return A {@link InputStreamAssert} instance for making assertions on the value.
         */
        public InputStreamAssert withInputStream() {
            isInstanceOf(InputStream.class);
            return Assertions.assertThat((InputStream) actual);
        }

        /**
         * Asserts that the value was a {@link File} instance.
         * @return A {@link FileAssert} instance for making assertions on the value.
         */
        public FileAssert withFile() {
            isInstanceOf(File.class);
            return Assertions.assertThat((File) actual);
        }

        /**
         * Asserts that the value was a {@link Integer} instance.
         * @return A {@link IntegerAssert} instance for making assertions on the value.
         */
        public IntegerAssert withInteger() {
            isInstanceOf(Integer.class);
            return Assertions.assertThat((Integer) actual);
        }

        /**
         * Asserts that the value was a {@link Boolean} instance.
         * @return A {@link BooleanAssert} instance for making assertions on the value.
         */
        public BooleanAssert withBoolean() {
            isInstanceOf(Boolean.class);
            return Assertions.assertThat((Boolean) actual);
        }

        /**
         * Asserts that the value was a {@link Long} instance.
         * @return A {@link LongAssert} instance for making assertions on the value.
         */
        public LongAssert withLong() {
            isInstanceOf(Long.class);
            return Assertions.assertThat((Long) actual);
        }

        /**
         * Asserts that the value was a {@link Double} instance.
         * @return A {@link DoubleAssert} instance for making assertions on the value.
         */
        public DoubleAssert withDouble() {
            isInstanceOf(Double.class);
            return Assertions.assertThat((Double) actual);
        }

        /**
         * Asserts that the value was an instance of type {@code T}.
         * @param <T> The type of the expected object.
         * @return A {@link ObjectAssert} instance for making assertions on the value.
         */
        public <T> ObjectAssert<T> withObject() {
            return Assertions.assertThat((T) actual);
        }

        /**
         * Asserts that the value was an array of type {@code T}.
         * @param <T> The type of the expected array.
         * @return A {@link ObjectArrayAssert} instance for making assertions on the value.
         */
        public <T> ObjectArrayAssert<T> withObjectArray() {
            return Assertions.assertThat((T[]) actual);
        }

    }
}
