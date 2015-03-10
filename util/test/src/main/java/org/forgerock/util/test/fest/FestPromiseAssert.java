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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.fest.assertions.Assertions;
import org.fest.assertions.BooleanAssert;
import org.fest.assertions.CollectionAssert;
import org.fest.assertions.DoubleAssert;
import org.fest.assertions.FileAssert;
import org.fest.assertions.GenericAssert;
import org.fest.assertions.IntAssert;
import org.fest.assertions.ListAssert;
import org.fest.assertions.LongAssert;
import org.fest.assertions.MapAssert;
import org.fest.assertions.ObjectArrayAssert;
import org.fest.assertions.ObjectAssert;
import org.fest.assertions.StringAssert;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

/**
 * Assertion class for a promise. Allows verification of the value that was completed with.
 */
public final class FestPromiseAssert
        extends AbstractFestPromiseAssert<Object, FestPromiseAssert, FestPromiseAssert.SuccessfulPromiseAssert> {

    /**
     * Creates an {@code FestPromiseAssert} instance for making assertions on a {@link Promise}.
     * @param promise The actual promise instance.
     * @return The {@code FestPromiseAssert} instance.
     */
    public static FestPromiseAssert assertThat(Promise<?, ?> promise) {
        return new FestPromiseAssert(promise);
    }

    private FestPromiseAssert(Promise<?, ?> promise) {
        super((Promise<Object, ?>) promise, FestPromiseAssert.class,
                new Function<Object, SuccessfulPromiseAssert, RuntimeException>() {
                    @Override
                    public SuccessfulPromiseAssert apply(Object value) throws RuntimeException {
                        return new SuccessfulPromiseAssert(value);
                    }
                });
    }

    /**
     * An assertion class for making assertions on the successful completion value of a {@link Promise}.
     */
    public static final class SuccessfulPromiseAssert extends GenericAssert<SuccessfulPromiseAssert, Object> {

        SuccessfulPromiseAssert(Object actual) {
            super(SuccessfulPromiseAssert.class, actual);
        }

        /**
         * Asserts that the value was a {@link Map} instance.
         * @return A {@link org.assertj.core.api.MapAssert} instance for making assertions on the value.
         */
        public MapAssert withMap() {
            isInstanceOf(Map.class);
            return Assertions.assertThat((Map) actual);
        }

        /**
         * Asserts that the value was a {@link Collection} instance.
         * @return A {@link CollectionAssert} instance for making assertions on the value.
         */
        public CollectionAssert withCollection() {
            isInstanceOf(Collection.class);
            return Assertions.assertThat((Collection) actual);
        }

        /**
         * Asserts that the value was a {@link List} instance.
         * @return A {@link ListAssert} instance for making assertions on the value.
         */
        public ListAssert withList() {
            isInstanceOf(List.class);
            return Assertions.assertThat((List) actual);
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
         * Asserts that the value was a {@link File} instance.
         * @return A {@link FileAssert} instance for making assertions on the value.
         */
        public FileAssert withFile() {
            isInstanceOf(File.class);
            return Assertions.assertThat((File) actual);
        }

        /**
         * Asserts that the value was a {@link Integer} instance.
         * @return A {@link IntAssert} instance for making assertions on the value.
         */
        public IntAssert withInteger() {
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
         * Asserts that the value was an {@link Object} instance.
         * @return A {@link ObjectAssert} instance for making assertions on the value.
         */
        public ObjectAssert withObject() {
            return Assertions.assertThat(actual);
        }

        /**
         * Asserts that the value was an array of {@link Object}s.
         * @return A {@link ObjectArrayAssert} instance for making assertions on the value.
         */
        public ObjectArrayAssert withObjectArray() {
            return Assertions.assertThat((Object[]) actual);
        }

        private void isInstanceOf(Class<?> type) {
            if (!type.isAssignableFrom(actual.getClass())) {
                fail("Object " + actual + " is not an instance of " + type);
            }
        }

    }

}
