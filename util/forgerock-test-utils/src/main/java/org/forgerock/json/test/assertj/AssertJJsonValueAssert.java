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

package org.forgerock.json.test.assertj;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.data.MapEntry;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;

/** Main that will provide the assertions on {@link JsonValue}. */
public final class AssertJJsonValueAssert {

    private AssertJJsonValueAssert() {
        // Prevent from instantiating
    }

    /**
     * Creates the relevant {@code AbstractJsonValueAssert} instance for the provided {@link JsonValue}.
     * @param value The actual value.
     * @return the subclass {@link AbstractJsonValueAssert} matching best the kind of {@link JsonValue}.
     */
    public static AbstractJsonValueAssert assertThat(JsonValue value) {
        return new JsonValueAssert(value);
    }

    /**
     * Creates a promise assert class for {@link JsonValue} instances.
     * <p>
     * On calling the succeeded method, the {@link AbstractJsonValueAssert#isObject()} and
     * {@link AbstractJsonValueAssert#isArray()} must be used to access array/object specific assert methods.
     * @param promise The {@link JsonValue} promise.
     * @return The assertion object.
     */
    public static AssertJJsonValuePromiseAssert assertThat(Promise<JsonValue, ?> promise) {
        return new AssertJJsonValuePromiseAssert(promise);
    }

    /**
     * An alias for {@link #assertThat(Promise)} for the case where different Promise assertThat methods
     * are statically imported and would clash.
     * @param promise The {@link JsonValue} promise.
     * @return The assertion object.
     */
    public static AssertJJsonValuePromiseAssert assertThatJsonValue(Promise<JsonValue, ?> promise) {
        return assertThat(promise);
    }

    /** An assertion class for promises that return {@code JsonValue}s. */
    public static final class AssertJJsonValuePromiseAssert
            extends AbstractAssertJPromiseAssert<JsonValue, AssertJJsonValuePromiseAssert, PromisedJsonValueAssert> {

        private AssertJJsonValuePromiseAssert(Promise<JsonValue, ?> promise) {
            super(promise, AssertJJsonValuePromiseAssert.class);
        }

        @Override
        protected PromisedJsonValueAssert createSucceededAssert(JsonValue jsonValue) {
            return new PromisedJsonValueAssert(jsonValue);
        }
    }

    /**
     * Abstract class for assertions on {@link JsonValue}.
     * @param <T> the assertion class
     */
    public abstract static class AbstractJsonValueAssert<T extends AbstractAssert<T, JsonValue>>
            extends AbstractAssert<T, JsonValue> {

        private AbstractJsonValueAssert(Class<T> type, JsonValue value) {
            super(value, type);
        }

        /**
         * Check that the {@link JsonValue} is an object.
         * @return The {@link ObjectJsonValueAssert} representation of this Assert instance.
         */
        public ObjectJsonValueAssert isObject() {
            isNotNull();
            if (!actual.isMap()) {
                failWithMessage("Expected %s to be an object", actual.getPointer());
            }
            return new ObjectJsonValueAssert(actual);
        }

        /**
         * Check that the {@link JsonValue} is an array.
         * @return The {@link ArrayJsonValueAssert} representation of this Assert instance.
         */
        public ArrayJsonValueAssert isArray() {
            isNotNull();
            if (!actual.isSet() && !actual.isList()) {
                failWithMessage("Expected %s to be an array", actual.getPointer());
            }
            return new ArrayJsonValueAssert(actual);
        }

        /**
         * Check that the {@link JsonValue} is a set.
         * @return The {@link AbstractIterableAssert} representation of this Assert instance.
         */
        public AbstractIterableAssert<?, ? extends Iterable<?>, Object> isSet() {
            isNotNull();
            if (!actual.isSet()) {
                failWithMessage("Expected %s to be a set", actual.getPointer());
            }
            return Assertions.assertThat(actual.asSet());
        }

        /**
         * Check that the {@link JsonValue} is a string.
         * @return The {@link AbstractCharSequenceAssert} representation of this Assert instance.
         */
        public AbstractCharSequenceAssert<?, String> isString() {
            isNotNull();
            if (!actual.isString()) {
                failWithMessage("Expected %s to be a string", actual.getPointer());
            }
            return Assertions.assertThat(actual.asString());
        }

        /**
         * Check that the {@link JsonValue} is a boolean.
         * @return The {@link AbstractBooleanAssert} representation of this Assert instance.
         */
        public AbstractBooleanAssert<?> isBoolean() {
            isNotNull();
            if (!actual.isBoolean()) {
                failWithMessage("Expected %s to be a boolean", actual.getPointer());
            }
            return Assertions.assertThat(actual.asBoolean());

        }

        /**
         * Check that the {@link JsonValue} is a number.
         * @return The {@link NumberJsonValueAssert} representation of this Assert instance.
         */
        public NumberJsonValueAssert isNumber() {
            isNotNull();
            if (!actual.isNumber()) {
                failWithMessage("Expected %s to be a number", actual.getPointer());
            }
            return new NumberJsonValueAssert(actual);
        }

        /**
         * Check that the {@link JsonValue} is an integer.
         * @return The {@link AbstractIntegerAssert} representation of this Assert instance.
         */
        public AbstractIntegerAssert<?> isInteger() {
            return assertThat(actual).isNumber().isInteger();
        }

        /**
         * Check that the {@link JsonValue} is a long.
         * @return The {@link AbstractLongAssert} representation of this Assert instance.
         */
        public AbstractLongAssert<?> isLong() {
            return assertThat(actual).isNumber().isLong();
        }

        /**
         * Check that the {@link JsonValue} is a long.
         * @return The {@link AbstractDoubleAssert} representation of this Assert instance.
         */
        public AbstractDoubleAssert<?> isDouble() {
            return assertThat(actual).isNumber().isDouble();
        }

        /**
         * Check that the JSON is either an array or an object and is empty.
         * @return This assertion object.
         */
        public T isEmpty() {
            if (actual.isMap()) {
                isObject().isEmpty();
            } else {
                isArray().isEmpty();
            }
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} is an object.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return The {@link ObjectJsonValueAssert} for that node.
         */
        public ObjectJsonValueAssert hasObject(String path) {
            return hasPath(path).isObject();
        }

        /**
         * Check that the referenced {@link JsonValue} is an array.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return The {@link ArrayJsonValueAssert} for that node.
         */
        public ArrayJsonValueAssert hasArray(String path) {
            return hasPath(path).isArray();
        }

        /**
         * Check that the referenced {@link JsonValue} is null.
         * @param path The {@link JsonPointer} path to the expected null.
         * @return This assert object, for further processing.
         */
        public T hasNull(String path) {
            JsonValue child = child(path);
            // Either it does not contain that child or the defined child is null
            if (child != null && child.isNotNull()) {
                failWithMessage("Expected not to find a defined child at %s from %s", path, actual.getPointer());
            }
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} doesn't exist in this object.
         * @param path The {@link JsonPointer} path.
         * @return This assert object, for further processing.
         */
        public T doesNotContain(String path) {
            Assertions.assertThat(child(path)).isNull();
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} is a boolean.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T booleanIs(String path, Condition<Boolean> condition) {
            booleanAt(path).is(condition);
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} is a boolean, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasBoolean(String path) {
            booleanAt(path);
            return myself;
        }

        /**
         * Get a {@link AbstractBooleanAssert} for the referenced {@link JsonValue} is a boolean, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link AbstractBooleanAssert} instance.
         */
        public AbstractBooleanAssert<?> booleanAt(String path) {
            return hasPath(path).isBoolean();
        }

        /**
         * Check that the referenced {@link JsonValue} is a string, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasString(String path) {
            hasPath(path).isString();
            return myself;
        }

        /**
         * Check the value of the referenced {@link JsonValue} string.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T stringIs(String path, Condition<String> condition) {
            stringAt(path).is(condition);
            return myself;
        }

        /**
         * Get a {@link AbstractCharSequenceAssert} for the referenced {@link JsonValue} is a string, to check its
         * value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link AbstractCharSequenceAssert} instance.
         */
        public AbstractCharSequenceAssert<?, String> stringAt(String path) {
            return hasPath(path).isString();
        }

        /**
         * Check that the referenced {@link JsonValue} is a number, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasNumber(String path) {
            hasPath(path).isNumber();
            return myself;
        }

        /**
         * Check the integer value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T integerIs(String path, Condition<Integer> condition) {
            integerAt(path).is(condition);
            return myself;
        }

        /**
         * Get a {@link AbstractIntegerAssert} for the referenced {@link JsonValue} is an integer, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link AbstractIntegerAssert} instance.
         */
        public AbstractIntegerAssert<?> integerAt(String path) {
            return hasPath(path).isNumber().isInteger();
        }

        /**
         * Check the long value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T longIs(String path, Condition<Long> condition) {
            longAt(path).is(condition);
            return myself;
        }

        /**
         * Get a {@link AbstractLongAssert} for the referenced {@link JsonValue} is a long, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link AbstractLongAssert} instance.
         */
        public AbstractLongAssert<?> longAt(String path) {
            return hasPath(path).isNumber().isLong();
        }

        /**
         * Check the double value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T doubleIs(String path, Condition<Double> condition) {
            doubleAt(path).is(condition);
            return myself;
        }

        /**
         * Get a {@link AbstractDoubleAssert} for the referenced {@link JsonValue} is a double, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link AbstractDoubleAssert} instance.
         */
        public AbstractDoubleAssert<?> doubleAt(String path) {
            return hasPath(path).isNumber().isDouble();
        }

        @Override
        public void isNull() {
            if (actual.isNotNull()) {
                failWithMessage("Expected %s to be null but contains %s", actual.getPointer(), actual.getObject());
            }
        }

        @Override
        public T isNotNull() {
            if (actual.isNull()) {
                failWithMessage("Expected %s not to be null.", actual.getPointer());
            }
            return myself;
        }

        /**
         * Get a {@link AbstractJsonValueAssert} for the referenced {@link JsonValue}. It will fail if the path does
         * not match any valid {@link JsonValue}.
         *
         * @param path The {@link JsonPointer} path to the expected value.
         * @return A new {@link AbstractJsonValueAssert} instance built around the found child.
         */
        public AbstractJsonValueAssert hasPath(String path) {
            JsonValue value = child(path);
            if (value == null) {
                failWithMessage("Expected the child %s from %s to defined", path, actual.getPointer());
            }
            return assertThat(value);
        }

        private JsonValue child(String path) {
            return actual.get(new JsonPointer(path));
        }
    }

    /** Class for assertions on {@link JsonValue} promises. */
    public static final class PromisedJsonValueAssert extends AbstractJsonValueAssert<PromisedJsonValueAssert> {
        private PromisedJsonValueAssert(JsonValue value) {
            super(PromisedJsonValueAssert.class, value);
        }
    }

    /** Class for assertions on object {@link JsonValue}. */
    public static final class ObjectJsonValueAssert extends AbstractJsonValueAssert<ObjectJsonValueAssert> {

        private AbstractMapAssert<?, ? extends Map<String, Object>, String, Object> mapAssert;

        private ObjectJsonValueAssert(JsonValue value) {
            super(ObjectJsonValueAssert.class, value);
            this.mapAssert = Assertions.assertThat(value.asMap());
        }

        @Override
        public ObjectJsonValueAssert isEmpty() {
            mapAssert.isEmpty();
            return myself;
        }

        /**
         * Check that this object contains a property with the given name, and value.
         * @param key The name of the object property.
         * @param value The expected value.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#containsEntry
         */
        public ObjectJsonValueAssert contains(String key, Object value) {
            mapAssert.containsEntry(key, value);
            return this;
        }

        /**
         * Check that this object contains the specified properties.
         * @param entries The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#contains
         */
        public ObjectJsonValueAssert contains(MapEntry... entries) {
            mapAssert.contains(entries);
            return this;
        }

        /**
         * Check that this object only contains the specified properties.
         * @param entries The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#containsOnly
         */
        public ObjectJsonValueAssert containsOnly(MapEntry... entries) {
            mapAssert.containsOnly(entries);
            return this;
        }

        /**
         * Check that this object contains exactly the specified properties.
         * @param entries The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#containsExactly
         */
        public ObjectJsonValueAssert containsExactly(MapEntry... entries) {
            mapAssert.containsExactly(entries);
            return this;
        }

        /**
         * Check that this object contains a field with the specified name.
         * @param key The expected key.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#containsKey
         */
        public ObjectJsonValueAssert containsField(String key) {
            mapAssert.containsKey(key);
            return this;
        }

        /**
         * Check that this object contains fields with the specified names.
         * @param keys The expected keys.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#containsKeys
         */
        public ObjectJsonValueAssert containsFields(String... keys) {
            mapAssert.containsKeys(keys);
            return this;
        }

        /**
         * Check that this object does not contain a property with the given name, and value.
         * @param key The name of the object property.
         * @param value The expected value it should not equal if it exists.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#doesNotContainEntry
         */
        public ObjectJsonValueAssert doesNotContain(String key, Object value) {
            mapAssert.doesNotContainEntry(key, value);
            return this;
        }

        /**
         * Check that this object does not contain a property with the given name, and value.
         * @param entries The expected entries that should not exist.
         * @return This assert instance for further processing (if required).
         * @see AbstractMapAssert#doesNotContain
         */
        public ObjectJsonValueAssert doesNotContain(MapEntry... entries) {
            mapAssert.doesNotContain(entries);
            return this;
        }

    }

    /** Class for assertions on array {@link JsonValue}. */
    public static final class ArrayJsonValueAssert extends AbstractJsonValueAssert<ArrayJsonValueAssert> {
        private AbstractListAssert<?, ? extends List<?>, Object> listAssert;

        private ArrayJsonValueAssert(JsonValue value) {
            super(ArrayJsonValueAssert.class, value);
            this.listAssert = Assertions.assertThat(value.asList());
        }

        @Override
        public ArrayJsonValueAssert isEmpty() {
            listAssert.isEmpty();
            return myself;
        }

        /**
         * Check that this array contains the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#contains
         */
        public ArrayJsonValueAssert contains(Object... values) {
            listAssert.contains(values);
            return this;
        }

        /**
         * Check that this array contains exactly the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#containsExactly
         */
        public ArrayJsonValueAssert containsExactly(Object... values) {
            listAssert.containsExactly(values);
            return this;
        }

        /**
         * Check that this array contains the given values as a sequence.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#containsSequence
         */
        public ArrayJsonValueAssert containsSequence(Object... values) {
            listAssert.containsSequence(values);
            return this;
        }

        /**
         * Check that this array contains only the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#containsOnly
         */
        public ArrayJsonValueAssert containsOnly(Object... values) {
            listAssert.containsOnly(values);
            return this;
        }

        /**
         * Check that this array does not contain the given values.
         * @param values The values expected to not be contained.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#doesNotContain
         */
        public ArrayJsonValueAssert doesNotContain(Object... values) {
            listAssert.doesNotContain(values);
            return this;
        }

        /**
         * Check that this array starts with the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#startsWith
         */
        public ArrayJsonValueAssert startsWith(Object... values) {
            listAssert.startsWith(values);
            return this;
        }

        /**
         * Check that this array ends with the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#endsWith
         */
        public ArrayJsonValueAssert endsWith(Object... values) {
            listAssert.endsWith(values);
            return this;
        }

        /**
         * Check that this array does not contain duplicates.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#doesNotHaveDuplicates
         */
        public ArrayJsonValueAssert doesNotHaveDuplicates() {
            listAssert.doesNotHaveDuplicates();
            return this;
        }

        /**
         * Check that this array contains the given size.
         * @param size The expected size.
         * @return This assert instance for further processing (if required).
         * @see AbstractListAssert#hasSize
         */
        public ArrayJsonValueAssert hasSize(int size) {
            listAssert.hasSize(size);
            return this;
        }

    }

    /** Class for assertions on simple {@link JsonValue}. */
    public static final class JsonValueAssert extends AbstractJsonValueAssert<JsonValueAssert> {

        private JsonValueAssert(JsonValue value) {
            super(JsonValueAssert.class, value);
        }

    }

    /** Class for assertions on number {@link JsonValue}. */
    public static final class NumberJsonValueAssert extends AbstractJsonValueAssert<NumberJsonValueAssert> {

        private NumberJsonValueAssert(JsonValue value) {
            super(NumberJsonValueAssert.class, value);
        }

        /**
         * Check that the {@link JsonValue} is an integer.
         * @return The {@link AbstractIntegerAssert} representation of this Assert instance.
         */
        @Override
        public AbstractIntegerAssert<?> isInteger() {
            Assertions.assertThat(actual.getObject()).isInstanceOf(Integer.class);
            return Assertions.assertThat(actual.asInteger());
        }

        /**
         * Check that the {@link JsonValue} is a long.
         * @return The {@link AbstractLongAssert} representation of this Assert instance.
         */
        @Override
        public AbstractLongAssert<?> isLong() {
            Assertions.assertThat(actual.getObject()).isInstanceOf(Long.class);
            return Assertions.assertThat(actual.asLong());
        }

        /**
         * Check that the {@link JsonValue} is a double.
         * @return The {@link AbstractDoubleAssert} representation of this Assert instance.
         */
        @Override
        public AbstractDoubleAssert<?> isDouble() {
            Assertions.assertThat(actual.getObject()).isInstanceOf(Double.class);
            return Assertions.assertThat(actual.asDouble());
        }
    }
}
