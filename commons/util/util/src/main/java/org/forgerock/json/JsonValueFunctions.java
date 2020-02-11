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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2010–2011 ApexIdentity Inc. All rights reserved.
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package org.forgerock.json;

import static org.forgerock.util.Reject.checkNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.forgerock.util.Function;
import org.forgerock.util.Utils;
import org.forgerock.util.time.Duration;

/**
 * This class contains the utility functions to convert a {@link JsonValue} to another type.
 */
public final class JsonValueFunctions {

    private JsonValueFunctions() {
    }

    private static class TypeFunction<V> implements Function<JsonValue, V, JsonValueException> {

        private final Class<V> type;

        TypeFunction(Class<V> type) {
            this.type = checkNotNull(type);
        }

        @Override
        public V apply(JsonValue value) throws JsonValueException {
            if (value.isNull()) {
                return null;
            }

            Object object = value.getObject();
            if (type.isInstance(object)) {
                return type.cast(object);
            }

            throw new JsonValueException(value, "Expecting an element of type " + type.getName());
        }
    }

    //@Checkstyle:off
    private static final Function<JsonValue, Charset, JsonValueException> TO_CHARSET =
            new Function<JsonValue, Charset, JsonValueException>() {
                @Override
                public Charset apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : Charset.forName(value.asString());
                    } catch (final IllegalCharsetNameException | UnsupportedCharsetException e) {
                        throw new JsonValueException(value, e);
                    }
                }
            };

    private static final Function<JsonValue, Duration, JsonValueException> TO_DURATION =
            new Function<JsonValue, Duration, JsonValueException>() {
                @Override
                public Duration apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : Duration.duration(value.asString());
                    } catch (final IllegalArgumentException iae) {
                        throw new JsonValueException(value, iae);
                    }
                }
            };

    private static final Function<JsonValue, File, JsonValueException> TO_FILE =
            new Function<JsonValue, File, JsonValueException>() {
                @Override
                public File apply(JsonValue value) throws JsonValueException {
                    return value.isNull() ? null : new File(value.asString());
                }
            };

    private static final Function<JsonValue, Pattern, JsonValueException> TO_PATTERN =
            new Function<JsonValue, Pattern, JsonValueException>() {
                @Override
                public Pattern apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : Pattern.compile(value.asString());
                    } catch (final PatternSyntaxException pse) {
                        throw new JsonValueException(value, pse);
                    }
                }
            };

    private static final Function<JsonValue, JsonPointer, JsonValueException> TO_POINTER =
            new Function<JsonValue, JsonPointer, JsonValueException>() {
                @Override
                public JsonPointer apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : new JsonPointer(value.asString());
                    } catch (final JsonValueException jve) {
                        throw jve;
                    } catch (final JsonException je) {
                        throw new JsonValueException(value, je);
                    }
                }
            };

    private static final Function<JsonValue, URL, JsonValueException> TO_URL =
            new Function<JsonValue, URL, JsonValueException>() {
                @Override
                public URL apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : new URL(value.asString());
                    } catch (final MalformedURLException e) {
                        throw new JsonValueException(value, e);
                    }
                }
            };

    private static final Function<JsonValue, URI, JsonValueException> TO_URI =
            new Function<JsonValue, URI, JsonValueException>() {
                @Override
                public URI apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : new URI(value.asString());
                    } catch (final URISyntaxException use) {
                        throw new JsonValueException(value, use);
                    }
                }
            };

    private static final Function<JsonValue, UUID, JsonValueException> TO_UUID =
            new Function<JsonValue, UUID, JsonValueException>() {
                @Override
                public UUID apply(JsonValue value) throws JsonValueException {
                    try {
                        return value.isNull() ? null : UUID.fromString(value.asString());
                    } catch (final IllegalArgumentException iae) {
                        throw new JsonValueException(value, iae);
                    }
                }
            };

    private static final Function<JsonValue, JsonValue, JsonValueException> IDENTITY =
            new Function<JsonValue, JsonValue, JsonValueException>() {
                @Override
                public JsonValue apply(JsonValue value) throws JsonValueException {
                    return value.copy();
                }
            };
    //@Checkstyle:on

    /**
     * Returns the JSON string value as a character set used for byte
     * encoding/decoding. If the JSON value is {@code null}, this function returns
     * {@code null}.
     *
     * @return the character set represented by the string value.
     * @throws JsonValueException
     *         if the JSON value is not a string or the character set
     *         specified is invalid.
     */
    public static Function<JsonValue, Charset, JsonValueException> charset() {
        return TO_CHARSET;
    }

    /**
     * Returns the JSON string value as a {@link Duration}. If the JSON value is {@code null}, this method returns
     * {@code null}.
     *
     * @return the duration represented by the string value.
     * @throws JsonValueException
     *         if the JSON value is not a string or the duration
     *         specified is invalid.
     */
    public static Function<JsonValue, Duration, JsonValueException> duration() {
        return TO_DURATION;
    }

    /**
     * Returns the JSON string value as an enum constant of the specified enum
     * type. The string value and enum constants are compared, ignoring case
     * considerations. If the JSON value is {@code null}, this method returns
     * {@code null}.
     *
     * @param <T>
     *         the enum type sub-class.
     * @param type
     *         the enum type to match constants with the value.
     * @return the enum constant represented by the string value.
     * @throws IllegalArgumentException
     *         if {@code type} does not represent an enum type. or
     *         if the JSON value does not match any of the enum's constants.
     * @throws NullPointerException
     *         if {@code type} is {@code null}.
     */
    public static <T extends Enum<T>> Function<JsonValue, T, JsonValueException> enumConstant(final Class<T> type) {
        return new Function<JsonValue, T, JsonValueException>() {
            @Override
            public T apply(JsonValue value) throws JsonValueException {
                return Utils.asEnum(value.asString(), type);
            }
        };
    }

    /**
     * Returns the JSON string value as a {@code File} object. If the JSON value
     * is {@code null}, this method returns {@code null}.
     *
     * @return a file represented by the string value.
     * @throws JsonValueException
     *         if the JSON value is not a string.
     */
    public static Function<JsonValue, File, JsonValueException> file() {
        return TO_FILE;
    }

    /**
     * Returns the JSON string value as a regular expression pattern. If the
     * JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the compiled regular expression pattern.
     * @throws JsonValueException
     *         if the pattern is not a string or the value is not a valid
     *         regular expression pattern.
     */
    public static Function<JsonValue, Pattern, JsonValueException> pattern() {
        return TO_PATTERN;
    }

    /**
     * Returns the JSON string value as a JSON pointer. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the JSON pointer represented by the JSON value string.
     * @throws JsonValueException
     *         if the JSON value is not a string or valid JSON pointer.
     */
    public static Function<JsonValue, JsonPointer, JsonValueException> pointer() {
        return TO_POINTER;
    }

    /**
     * Returns the JSON string value as a uniform resource identifier. If the
     * JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the URI represented by the string value.
     * @throws JsonValueException
     *         if the given string violates URI syntax.
     */
    public static Function<JsonValue, URI, JsonValueException> uri() {
        return TO_URI;
    }

    /**
     * Returns the JSON string value as a uniform resource locator. If the
     * JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the URL represented by the string value.
     * @throws JsonValueException
     *         if the given string violates URL syntax.
     */
    public static Function<JsonValue, URL, JsonValueException> url() {
        return TO_URL;
    }

    /**
     * Returns the JSON string value as a universally unique identifier (UUID).
     * If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the UUID represented by the JSON value string.
     * @throws JsonValueException
     *         if the JSON value is not a string or valid UUID.
     */
    public static Function<JsonValue, UUID, JsonValueException> uuid() {
        return TO_UUID;
    }

    /**
     * Returns the JSON value as a {@link List} containing objects whose type
     * (and value) is specified by a transformation function. If the value is
     * {@code null}, this method returns {@code null}. It is up to to the
     * transformation function to transform/enforce source types of the elements
     * in the Json source collection.  If any of the elements of the list are not of
     * the appropriate type, or the type-transformation cannot occur,
     * the exception specified by the transformation function is thrown.
     *
     * @param <V>
     *            the type of elements in this list
     * @param <E>
     *            the type of exception thrown by the transformation function
     * @param transformFunction
     *            a {@link Function} to transform an element of the JsonValue list
     *            to the desired type
     * @return the list value, or {@code null} if no value.
     * @throws E
     *             if the JSON value is not a {@code List}, not a {@code Set}, contains an
     *             unexpected type, or contains an element that cannot be transformed
     * @throws NullPointerException
     *             if {@code transformFunction} is {@code null}.
     */
    public static <V, E extends Exception> Function<JsonValue, List<V>, E> listOf(
            final Function<JsonValue, V, E> transformFunction) throws E {
        return new Function<JsonValue, List<V>, E>() {
            @Override
            public List<V> apply(JsonValue value) throws E {
                if (value.isCollection()) {
                    final List<V> list = new ArrayList<>(value.size());
                    for (JsonValue elem : value) {
                        list.add(elem.as(transformFunction));
                    }
                    return list;
                }
                return null;
            }
        };
    }

    /**
     * Returns the JSON value as a {@link Set} containing objects whose type
     * (and value) is specified by a transformation function. If the value is
     * {@code null}, this method returns {@code null}. It is up to to the
     * transformation function to transform/enforce source types of the elements
     * in the Json source collection.  If called on an object which wraps a List,
     * this method will drop duplicates performing element comparisons using
     * equals/hashCode. If any of the elements of the collection are not of
     * the appropriate type, or the type-transformation cannot occur, the
     * exception specified by the transformation function is thrown.
     *
     * @param <V>
     *            the type of elements in this set
     * @param <E>
     *            the type of exception thrown by the transformation function
     * @param transformFunction
     *            a {@link Function} to transform an element of the JsonValue set
     *            to the desired type
     * @return the set value, or {@code null} if no value.
     * @throws E
     *             if the JSON value is not a {@code Set}, contains an
     *             unexpected type, or contains an element that cannot be
     *             transformed
     * @throws NullPointerException
     *             if {@code transformFunction} is {@code null}.
     */
    public static <V, E extends Exception> Function<JsonValue, Set<V>, E> setOf(
            final Function<JsonValue, V, E> transformFunction) throws E {
        return new Function<JsonValue, Set<V>, E>() {
            @Override
            public Set<V> apply(JsonValue value) throws E {
                if (value.isCollection()) {
                    final Set<V> set = new LinkedHashSet<>(value.size());
                    for (JsonValue elem : value) {
                        set.add(elem.as(transformFunction));
                    }
                    return set;
                }
                return null;
            }
        };
    }

    /**
     * Returns the JSON value as a {@link Set} containing objects whose type
     * (and value) is specified by the parameter {@code type}. If the value is
     * {@code null}, this method returns {@code null}. If called on an object
     * which wraps a List, this method will drop duplicates performing element
     * comparisons using equals/hashCode. If any of the elements of the collection
     * are not of the appropriate type, or the type-transformation cannot occur,
     * {@link JsonValueException} is thrown.
     *
     * @param <V>
     *            the type of elements in this set
     * @param type
     *            a {@link Class} that specifies the desired type of each element
     *            in the resultant JsonValue set
     * @return the set value, or {@code null} if no value.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     * @throws JsonValueException
     *             if the elements of the collection cannot be cast as {@code type}.
     */
    public static <V> Function<JsonValue, Set<V>, JsonValueException> setOf(final Class<V> type) {
        return setOf(new TypeFunction<>(type));
    }

    /**
     * Returns the JSON value as the result of a deep JsonValue object-traversal,
     * applying the provided transform {@code function} to each element.
     *
     * @param function
     *            a {@link Function} that applies the desired element transformation
     *            in the resultant JsonValue set
     * @return the transformed JsonValue
     * @throws JsonValueException
     *             if the elements of the JsonValue cannot be transformed by {@code function}.
     */
    public static Function<JsonValue, JsonValue, JsonValueException> deepTransformBy(
            Function<JsonValue, ?, JsonValueException> function) {
        return new JsonValueTraverseFunction(function);
    }

    /**
     * Returns an identity function that will copy the input {@link JsonValue}.
     * @return an identity function that will copy the input {@link JsonValue}.
     * @throws JsonValueException
     *             if an error occurred while copying the input.
     */
    public static Function<JsonValue, JsonValue, JsonValueException> identity() {
        return IDENTITY;
    }
}
