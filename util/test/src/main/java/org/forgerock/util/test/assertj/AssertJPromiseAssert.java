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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractInputStreamAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
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
        super((Promise<Object, ?>) promise, AssertJPromiseAssert.class);
    }

    @Override
    protected SuccessfulPromiseAssert createSucceededAssert(Object actual) {
        return new SuccessfulPromiseAssert(actual);
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
        public <K, V> AbstractMapAssert<?, ? extends Map<K, V>, K, V> withMap() {
            isInstanceOf(Map.class);
            return Assertions.assertThat((Map<K, V>) actual);
        }

        /**
         * Asserts that the value was a {@link Iterable} instance.
         * @param <T> The iterable contents type.
         * @return A {@link AbstractIterableAssert} instance for making assertions on the value.
         */
        public <T> AbstractIterableAssert<?, ? extends Iterable<? extends T>, T> withIterable() {
            isInstanceOf(Iterable.class);
            return Assertions.assertThat((Iterable<T>) actual);
        }

        /**
         * Asserts that the value was a {@link List} instance.
         *
         * @param <T> The list contents type.
         * @return A {@link AbstractListAssert} instance for making assertions on the value.
         */
        public <T> AbstractListAssert<?, ? extends List<? extends T>, T> withList() {
            isInstanceOf(List.class);
            return Assertions.assertThat((List<T>) actual);
        }

        /**
         * Asserts that the value was a {@link String} instance.
         * @return A {@link AbstractCharSequenceAssert} instance for making assertions on the value.
         */
        public AbstractCharSequenceAssert<?, String> withString() {
            isInstanceOf(String.class);
            return Assertions.assertThat((String) actual);
        }

        /**
         * Asserts that the value was a {@link InputStream} instance.
         * @return A {@link AbstractInputStreamAssert} instance for making assertions on the value.
         */
        public AbstractInputStreamAssert<?, ? extends InputStream> withInputStream() {
            isInstanceOf(InputStream.class);
            return Assertions.assertThat((InputStream) actual);
        }

        /**
         * Asserts that the value was a {@link File} instance.
         * @return A {@link AbstractFileAssert} instance for making assertions on the value.
         */
        public AbstractFileAssert<?> withFile() {
            isInstanceOf(File.class);
            return Assertions.assertThat((File) actual);
        }

        /**
         * Asserts that the value was a {@link Integer} instance.
         * @return A {@link AbstractIntegerAssert} instance for making assertions on the value.
         */
        public AbstractIntegerAssert<?> withInteger() {
            isInstanceOf(Integer.class);
            return Assertions.assertThat((Integer) actual);
        }

        /**
         * Asserts that the value was a {@link Boolean} instance.
         * @return A {@link AbstractBooleanAssert} instance for making assertions on the value.
         */
        public AbstractBooleanAssert<?> withBoolean() {
            isInstanceOf(Boolean.class);
            return Assertions.assertThat((Boolean) actual);
        }

        /**
         * Asserts that the value was a {@link Long} instance.
         * @return A {@link AbstractLongAssert} instance for making assertions on the value.
         */
        public AbstractLongAssert<?> withLong() {
            isInstanceOf(Long.class);
            return Assertions.assertThat((Long) actual);
        }

        /**
         * Asserts that the value was a {@link Double} instance.
         * @return A {@link AbstractDoubleAssert} instance for making assertions on the value.
         */
        public AbstractDoubleAssert<?> withDouble() {
            isInstanceOf(Double.class);
            return Assertions.assertThat((Double) actual);
        }

        /**
         * Asserts that the value was an instance of type {@code T}.
         * @param <T> The type of the expected object.
         * @return A {@link AbstractObjectAssert} instance for making assertions on the value.
         */
        public <T> AbstractObjectAssert<?, T> withObject() {
            return Assertions.assertThat((T) actual);
        }

        /**
         * Asserts that the value was an array of type {@code T}.
         * @param <T> The type of the expected array.
         * @return A {@link AbstractObjectArrayAssert} instance for making assertions on the value.
         */
        public <T> AbstractObjectArrayAssert<?, T> withObjectArray() {
            return Assertions.assertThat((T[]) actual);
        }

    }
}
