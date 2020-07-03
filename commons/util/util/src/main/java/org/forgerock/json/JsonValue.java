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

import java.math.BigDecimal;
import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.forgerock.util.Function;
import org.forgerock.util.RangeSet;

/**
 * Represents a value in a JSON object model structure. JSON values are
 * represented with standard Java objects: {@link String}, {@link Number},
 * {@link Map}, {@link List}, {@link Boolean} and {@code null}.
 */
public class JsonValue implements Cloneable, Iterable<JsonValue> {

    /**
     * Returns a mutable JSON array containing the provided objects. This method
     * is provided as a convenience method for constructing JSON arrays. Example
     * usage:
     *
     * <pre>
     * JsonValue value = json(array(1, 2, 3));
     * </pre>
     *
     * @param objects
     *            The array elements.
     * @return A JSON array.
     */
    public static List<Object> array(final Object... objects) {
        return new ArrayList<>(Arrays.asList(objects));
    }

    /**
     * Returns a mutable JSON set containing the provided objects. This method
     * is provided as a convenience method for constructing JSON set. Example
     * usage:
     *
     * <pre>
     * JsonValue value = json(set(1, 2, 3));
     * </pre>
     *
     * @param objects
     *            The set elements.
     * @return A JSON set.
     */
    public static List<Object> set(final Object... objects) {
        return new ArrayList<>(Arrays.asList(objects));
    }
    
    /**
     * Returns a JSON field for inclusion in a JSON object using
     * {@link #object(java.util.Map.Entry...) object} only if its value is not {@code null}.
     * Example usage:
     *
     * <pre>
     * JsonValue value = json(object(fieldIfNotNull(&quot;uid&quot;, getUid()));
     * </pre>
     * <p>
     * Note: This feature depends on the {@link #object(java.util.Map.Entry...)} method that
     * checks if the entry is not {@code null} before including it into the map.
     * </p>
     *
     * @param key
     *            The JSON field name.
     * @param value
     *            The JSON field value (may be {@code null}).
     * @return The JSON field for inclusion in a JSON object or {@code null}.
     * @see #object(java.util.Map.Entry...)
     */
    public static Map.Entry<String, Object> fieldIfNotNull(final String key, final Object value) {
        return value != null ? field(key, value) : null;
    }

    /**
     * Returns a JSON field for inclusion in a JSON object using
     * {@link #object(java.util.Map.Entry...) object}. This method is provided
     * as a convenience method for constructing JSON objects. Example usage:
     *
     * <pre>
     * JsonValue value = json(object(field(&quot;uid&quot;, &quot;bjensen&quot;), field(&quot;age&quot;, 30)));
     * </pre>
     *
     * @param key
     *            The JSON field name.
     * @param value
     *            The JSON field value.
     * @return The JSON field for inclusion in a JSON object.
     */
    public static Map.Entry<String, Object> field(final String key, final Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * Returns a JSON value whose content is the provided object. This method is
     * provided as a convenience method for constructing JSON objects, instead
     * of using {@link #JsonValue(Object)}. Example usage:
     *
     * <pre>
     * JsonValue value =
     *         json(object(field(&quot;uid&quot;, &quot;bjensen&quot;),
     *                     field(&quot;roles&quot;, array(&quot;sales&quot;, &quot;marketing&quot;))));
     * </pre>
     *
     * @param object
     *            the Java object representing the JSON value.
     * @return The JSON value.
     */
    public static JsonValue json(final Object object) {
        return object instanceof JsonValue ? (JsonValue) object : new JsonValue(object);
    }

    /**
     * Returns a JSON object comprised of the provided JSON
     * {@link #field(String, Object) fields}. This method is provided as a
     * convenience method for constructing JSON objects. Example usage:
     *
     * <pre>
     * JsonValue value = json(object(field(&quot;uid&quot;, &quot;bjensen&quot;), field(&quot;age&quot;, 30)));
     * </pre>
     *
     * @param fields
     *            The list of {@link #field(String, Object) fields} to include
     *            in the JSON object. {@code null} elements are allowed, but are
     *            not included in the returned map (this makes it easier to
     *            include optional elements).
     * @return The JSON object.
     */
    @SafeVarargs
    public static Map<String, Object> object(final Map.Entry<String, Object>... fields) {
        final Map<String, Object> object = object(fields.length);
        for (final Map.Entry<String, Object> field : fields) {
            if (field != null) {
                object.put(field.getKey(), field.getValue());
            }
        }
        return object;
    }

    /**
     * Produces an empty JSON object pre-allocated for {@code size}
     * {@link #field(String, Object) fields}. This method is provided as a
     * convenience method for constructing JSON objects. Example usage:
     *
     * <pre>
     * JsonValue value = json(object(20));
     * for (Map.Entry&lt;String, Object&gt; entry : someMap.entrySet()) {
     *     value.put(entry.getKey(), entry.getValue());
     * }
     * </pre>
     *
     * @param size
     *            The size of the JSON object to allocate.
     * @return The [empty] JSON object.
     */
    public static Map<String, Object> object(int size) {
        return new LinkedHashMap<>(size);
    }

    /**
     * Returns the key as an list index value. If the string does not represent
     * a valid list index value, then {@code -1} is returned.
     *
     * @param key
     *            the key to be converted into an list index value.
     * @return the converted index value, or {@code -1} if invalid.
     */
    public static int toIndex(final String key) {
        if (key == null || key.isEmpty()) {
            return -1;
        }

        // verify that every character is a digit (this also prevents negative values)
        int result = 0;

        for (int i = 0; i < key.length(); ++i) {
            final char c = key.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            result = result * 10 + (c - '0');
        }
        return result;
    }

    /** The Java object representing this JSON value. */
    private Object object;

    /** The pointer to the value within a JSON structure. */
    private JsonPointer pointer;

    /**
     * Constructs a JSON value object with a given object. This constructor will
     * automatically unwrap {@link JsonValue} objects.
     *
     * @param object
     *            the Java object representing the JSON value.
     */
    public JsonValue(final Object object) {
        this(object, null);
    }

    /**
     * Constructs a JSON value object with a given object and pointer. This
     * constructor will automatically unwrap {@link JsonValue} objects.
     *
     * @param object
     *            the Java object representing the JSON value.
     * @param pointer
     *            the pointer to the value in a JSON structure.
     */
    public JsonValue(final Object object, final JsonPointer pointer) {
        this.object = object;
        this.pointer = pointer;
        if (object instanceof JsonValue) {
            final JsonValue jv = (JsonValue) object;
            this.object = jv.object;
            if (pointer == null) {
                this.pointer = jv.pointer;
            }
        }
        if (this.pointer == null) {
            this.pointer = new JsonPointer();
        }
    }

    /**
     * Adds the specified value to the list. Adding a value to a list shifts any
     * existing elements at or above the specified index to the right by one.
     *
     * @param index
     *            the {@code List} index of the value to add.
     * @param object
     *            the java object to add.
     * @return this JSON value.
     * @throws JsonValueException
     *             if this JSON value is not a {@code List} or index is out of
     *             range.
     */
    public JsonValue add(final int index, final Object object) {
        final List<Object> list = required().asList();
        if (index < 0 || index > list.size()) {
            throw new JsonValueException(this, "List index out of range: " + index);
        }
        list.add(index, object);
        return this;
    }

    /**
     * Adds the value identified by the specified pointer, relative to this
     * value as root. If doing so would require the creation of a new object or
     * list, a {@code JsonValueException} will be thrown.
     * <p>
     * NOTE: values may be added to a list using the reserved JSON pointer token
     * "-". For example, the pointer "/a/b/-" will add a new element to the list
     * referenced by "/a/b".
     *
     * @param pointer
     *            identifies the child value to add.
     * @param object
     *            the Java object value to add.
     * @return this JSON value.
     * @throws JsonValueException
     *             if the specified pointer is invalid.
     */
    public JsonValue add(final JsonPointer pointer, final Object object) {
        navigateToParentOf(pointer).required().addToken(pointer.leaf(), object);
        return this;
    }

    /**
     * Adds the specified value to the end of the list. This method is
     * equivalent to the following code:
     *
     * <pre>
     * add(size(), object);
     * </pre>
     *
     * @param object
     *            the java object to add.
     * @return this JSON value.
     * @throws JsonValueException
     *             if this JSON value is not a {@code List}.
     */
    public JsonValue add(final Object object) {
        if (isList()) {
            return add(size(), object);
        }
        throw new JsonValueException(this, "Expecting a List");
    }

    /**
     * Adds the specified value.
     * <p>
     * If adding to a list value, the specified key must be parseable as an
     * unsigned base-10 integer and be less than or equal to the list size.
     * Adding a value to a list shifts any existing elements at or above the
     * specified index to the right by one.
     *
     * @param key
     *            the {@code Map} key or {@code List} index to add.
     * @param object
     *            the Java object to add.
     * @return this JSON value.
     * @throws JsonValueException
     *             if not a {@code Map} or {@code List}, the {@code Map} key
     *             already exists, or the {@code List} index is out of range.
     */
    public JsonValue add(final String key, final Object object) {
        if (isMap()) {
            final Map<String, Object> map = asMap();
            if (map.containsKey(key)) {
                throw new JsonValueException(this, "Map key " + key + " already exists");
            }
            map.put(key, object);
        } else if (isList()) {
            add(toIndex(key), object);
        } else {
            throw new JsonValueException(this, "Expecting a Map or List");
        }
        return this;
    }

    /**
     * Adds the value identified by the specified pointer, relative to this
     * value as root. Missing parent objects or lists will be created on demand.
     * <p>
     * NOTE: values may be added to a list using the reserved JSON pointer token
     * "-". For example, the pointer "/a/b/-" will add a new element to the list
     * referenced by "/a/b".
     *
     * @param pointer
     *            identifies the child value to add.
     * @param object
     *            the Java object value to add.
     * @return this JSON value.
     * @throws JsonValueException
     *             if the specified pointer is invalid.
     */
    public JsonValue addPermissive(final JsonPointer pointer, final Object object) {
        navigateToParentOfPermissive(pointer).addToken(pointer.leaf(), object);
        return this;
    }

    /**
     * Returns the JSON value as a {@link Boolean} object. If the value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the boolean value.
     * @throws JsonValueException
     *             if the JSON value is not a boolean type.
     */
    public Boolean asBoolean() {
        return (object == null ? null : (Boolean) (expect(Boolean.class).object));
    }

    /**
     * Returns the JSON value as a {@link Double} object. This may involve
     * rounding. If the JSON value is {@code null}, this method returns
     * {@code null}.
     *
     * @return the double-precision floating point value.
     * @throws JsonValueException
     *             if the JSON value is not a number.
     */
    public Double asDouble() {
        return (object == null ? null : Double.valueOf(asNumber().doubleValue()));
    }

    /**
     * Returns the JSON value as an {@link Integer} object. This may involve
     * rounding or truncation. If the JSON value is {@code null}, this method
     * returns {@code null}.
     *
     * @return the integer value.
     * @throws JsonValueException
     *             if the JSON value is not a number.
     */
    public Integer asInteger() {
        return (object == null ? null : Integer.valueOf(asNumber().intValue()));
    }
    
    /**
     * Returns the JSON value as an {@link BigDecimal} object. This may involve
     * rounding or truncation. If the JSON value is {@code null}, this method
     * returns {@code null}.
     *
     * @return the integer value.
     * @throws JsonValueException
     *             if the JSON value is not a number.
     */
    public BigDecimal asBigDecimal() {
        return (object == null ? null : BigDecimal.valueOf(asNumber().doubleValue()));
    }

    /**
     * Returns the JSON value as a {@link Collection} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the collection value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Collection}.
     */
    public Collection<Object> asCollection() {
        return asCollection(Object.class);
    }

    /**
     * Returns the JSON value as a {@link List} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     * The returned {@link List} is <b>not</b> a copy : any interaction with it
     * will affect the {@link JsonValue}.
     *
     * @return the list value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code List}.
     */
    public List<Object> asList() {
        return asList(Object.class);
    }

    public Set<Object> asSet() {
        return new HashSet<Object>(asCollection());
    }
    
    /**
     * Returns the JSON value as a {@link Set} containing objects of the
     * specified type. If the value is {@code null}, this method returns
     * {@code null}. If any of the elements of the set are not {@code null} and
     * not of the specified type, {@code JsonValueException} is thrown.  If
     * called on an object which wraps a List, this method will drop duplicates
     * performing element comparisons using equals/hashCode.
     * The returned {@link Set} is <b>not</b> a copy : any interaction with it
     * will affect the {@link JsonValue}.
     *
     * @param <E>
     *            the type of elements in this set
     * @param type
     *            the type of object that all elements are expected to be.
     * @return the set value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Set}, not a {@code List},
     *             or contains an unexpected type.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <E> Set<E> asSet(final Class<E> type) {
    		return new HashSet<E>(asCollection(type));
    }
    
    /**
     * Returns the JSON value as a {@link Collection} containing objects of the
     * specified type. If the value is {@code null}, this method returns
     * {@code null}. If any of the elements of the collection are not {@code null} and
     * not of the specified type, {@code JsonValueException} is thrown.
     * The returned {@link Collection} is <b>not</b> a copy : any interaction with it
     * will affect the {@link JsonValue}.
     *
     * @param <E>
     *            the type of elements in this collection
     * @param type
     *            the type of object that all elements are expected to be.
     * @return the collection value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Collection} or contains an
     *             unexpected type.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <E> Collection<E> asCollection(final Class<E> type) {
        if (object != null) {
            expect(Collection.class);
            if (type != Object.class) {
                final Collection<Object> coll = (Collection<Object>) this.object;
                for (final Object element : coll) {
                    if (element != null && !type.isInstance(element)) {
                        throw new JsonValueException(this, "Expecting a Collection of " + type.getName()
                                + " elements");
                    }
                }
            }
        }
        return (Collection<E>) object;
    }

    /**
     * Returns the JSON value as a {@link List} containing objects of the
     * specified type. If the value is {@code null}, this method returns
     * {@code null}. If any of the elements of the list are not {@code null} and
     * not of the specified type, {@code JsonValueException} is thrown.
     * The returned {@link List} is <b>not</b> a copy : any interaction with it
     * will affect the {@link JsonValue}.
     *
     * @param <E>
     *            the type of elements in this list
     * @param type
     *            the type of object that all elements are expected to be.
     * @return the list value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code List} or contains an unexpected type.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> asList(final Class<E> type) {
        if (object != null) {
            expect(List.class);
            if (type != Object.class) {
                final List<Object> list = (List<Object>) this.object;
                for (final Object element : list) {
                    if (element != null && !type.isInstance(element)) {
                        throw new JsonValueException(this, "Expecting a List of " + type.getName()
                                + " elements");
                    }
                }
            }
        }
        return (List<E>) object;
    }

    /**
     * Returns the JSON value as an object whose type
     * (and value) is specified by a transformation function. It is up to to the
     * transformation function to transform/enforce source types of the elements
     * in the Json source element and to decide what to do depending on the kind
     * of {@link JsonValue} : if it is null, a {@link String}, a {@link List},
     * or {@link Map}. If the type-transformation cannot occur,
     * the exception specified by the transformation function is thrown.
     *
     * @param <V>
     *            the type of element
     * @param <E>
     *            the type of exception thrown by the transformation function
     * @param transformFunction
     *            a {@link Function} to transform the JsonValue element to the desired type
     * @return the value, or {@code null} if no value.
     * @throws E
     *             if the JsonValue element cannot be transformed
     * @throws NullPointerException
     *             if {@code transformFunction} is {@code null}.
     */
    public <V, E extends Exception> V as(final Function<JsonValue, V, E> transformFunction) throws E {
        return transformFunction.apply(this);
    }

    /**
     * Returns the JSON value as a {@link Long} object. This may involve
     * rounding or truncation. If the JSON value is {@code null}, this method
     * returns {@code null}.
     *
     * @return the long integer value.
     * @throws JsonValueException
     *             if the JSON value is not a number.
     */
    public Long asLong() {
        return (object == null ? null : Long.valueOf(asNumber().longValue()));
    }

    /**
     * Returns the JSON value as a {@code Map} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the map value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Map}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() {
        return (object == null ? null : (Map<String, Object>) (expect(Map.class).object));
    }

    /**
     * Returns the JSON string value as an enum constant of the specified enum
     * type. The string value and enum constants are compared, ignoring case
     * considerations. If the JSON value is {@code null}, this method returns
     * {@code null}.
     *
     * @param <T>
     *            the enum type sub-class.
     * @param type
     *            the enum type to match constants with the value.
     * @return the enum constant represented by the string value.
     * @throws IllegalArgumentException
     *             if {@code type} does not represent an enum type. or
     *             if the JSON value does not match any of the enum's constants.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     * @deprecated Use the method {@link #as(Function)} with the appropriate function. (Replace the following call
     * {@code jv.asEnum(clazz)} with {@code jv.map(JsonValueFunctions.enumConstant(clazz)}).
     * @see #as(Function)
     * @see JsonValueFunctions#enumConstant(Class)
     */
    @Deprecated
    public <T extends Enum<T>> T asEnum(final Class<T> type) {
        return as(JsonValueFunctions.enumConstant(type));
    }
    
    /**
     * Returns the JSON value as a {@link Map} containing objects of the
     * specified type. If the value is {@code null}, this method returns
     * {@code null}. If any of the values of the map are not {@code null} and
     * not of the specified type, {@code JsonValueException} is thrown.
     *
     * @param <V>
     *            the type of values in this map
     * @param type
     *            the type of object that all values are expected to be.
     * @return the map value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Map} or contains an
     *             unexpected type.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <V> Map<String, V> asMap(final Class<V> type) {
        if (object != null) {
            expect(Map.class);
            if (type != Object.class) {
                final Map<String, Object> map = (Map<String, Object>) this.object;
                for (final Object element : map.values()) {
                    if (element != null && !type.isInstance(element)) {
                        throw new JsonValueException(this, "Expecting a Map of " + type.getName()
                                + " elements");
                    }
                }
            }
        }
        return (Map<String, V>) object;
    }
    /**
     * Returns the JSON value as a {@link Map} containing a collection of
     * objects of the specified type. If the value is {@code null}, this method
     * returns {@code null}. If any of the values of the map are not {@code null} and
     * not of the specified type, {@code JsonValueException} is thrown.
     *
     * @param <E>
     *            the type of elements in the collection
     * @param elementType
     *            the type of object that all collection elements are
     *            expected to be.
     * @return the map value, or {@code null} if no value.
     * @throws JsonValueException
     *             if the JSON value is not a {@code Map} or contains an
     *             unexpected type.
     * @throws NullPointerException
     *             if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <E> Map<String, List<E>> asMapOfList(final Class<E> elementType) {
        if (object != null) {
            expect(Map.class);
            if (elementType != Object.class) {
                final Map<String, Object> map = (Map<String, Object>) this.object;
                for (final Object value : map.values()) {
                    if (value != null && !(value instanceof List)) {
                        throw new JsonValueException(this, "Expecting a Map of List values");
                    }
                    final List<?> list = (List<?>) value;
                    for (final Object element : list) {
                        if (element != null && !elementType.isInstance(element)) {
                            throw new JsonValueException(this, "Expecting a Map of Lists with "
                                    + elementType.getName() + " elements");
                        }
                    }
                }
            }
        }
        return (Map<String, List<E>>) object;
    }

    /**
     * Returns the JSON value as a {@code Number} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the numeric value.
     * @throws JsonValueException
     *             if the JSON value is not a number.
     */
    public Number asNumber() {
        return (object == null ? null : (Number) (expect(Number.class).object));
    }

    /**
     * Returns the JSON value as a {@code String} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the string value.
     * @throws JsonValueException
     *             if the JSON value is not a string.
     */
    public String asString() {
        return (object == null ? null : (String) (expect(String.class).object));
    }

    /**
     * Removes all child values from this JSON value, if it has any.
     */
    public void clear() {
        if (isMap()) {
            asMap().clear();
        } else if (isCollection()) {
            asCollection().clear();
        }
    }

    /**
     * Returns a shallow copy of this JSON value. If this JSON value contains a
     * {@code Map} or a {@code List} object, the returned JSON
     * value will contain a shallow copy of the original contained object.
     * <p>
     * The new value's members can be modified without affecting the original
     * value. Modifying the member's members will almost certainly affect the
     * original value. To avoid this, use the {@link #copy} method to return a
     * deep copy of the JSON value.
     * <p>
     * This method does not traverse the value's members, nor will it apply any
     * transformations.
     *
     * @return a shallow copy of this JSON value.
     */
    @Override
    public JsonValue clone() {
        final JsonValue result = new JsonValue(this.object, this.pointer);
        if (isMap()) {
            result.object = new LinkedHashMap<>(this.asMap());
        } else if (isList()) {
            result.object = new ArrayList<>(this.asList());
        }
        return result;
    }

    /**
     * Returns {@code true} this JSON value contains an item with the specified
     * value.
     *
     * @param object
     *            the object to seek within this JSON value.
     * @return {@code true} if this value contains the specified member value.
     */
    public boolean contains(final Object object) {
        boolean result = false;
        if (isMap()) {
            result = asMap().containsValue(object);
        } else if (isCollection()) {
            result = asCollection().contains(object);
        }
        return result;
    }

    /**
     * Returns a deep copy of this JSON value.
     * <p>
     * Note: This method is recursive, and currently has no ability to detect or
     * correct for structures containing cyclic references. Processing such a
     * structure will result in a {@link StackOverflowError} being thrown.
     *
     * @return a deep copy of this JSON value.
     */
    public JsonValue copy() {
        // TODO: track original values to resolve cyclic references
        final JsonValue result = new JsonValue(object, pointer); // start with shallow copy
        if (this.isMap()) {
            final Map<String, Object> map = object(size());
            for (final String key : keys()) {
                map.put(key, this.get(key).copy().getObject()); // recursion
            }
            result.object = map;
        } else if (isList()) {
            final ArrayList<Object> list = new ArrayList<>(size());
            for (final JsonValue element : this) {
                list.add(element.copy().getObject()); // recursion
            }
            result.object = list;
        }
        return result;
    }

    /**
     * Defaults the JSON value to the specified value if it is currently
     * {@code null}.
     *
     * @param object
     *            the object to default to.
     * @return this JSON value or a new JSON value containing the default value.
     */
    public JsonValue defaultTo(final Object object) {
        return (this.object != null ? this : new JsonValue(object, this.pointer));
    }

    /**
     * Called to enforce that the JSON value is of a particular type. A value of
     * {@code null} is allowed.
     *
     * @param type
     *            the class that the underlying value must have.
     * @return this JSON value.
     * @throws JsonValueException
     *             if the value is not the specified type.
     */
    public JsonValue expect(final Class<?> type) {
        if (object != null && !type.isInstance(object)) {
            throw new JsonValueException(this, "Expecting a " + type.getName());
        }
        return this;
    }

    /**
     * Returns the specified child value. If this JSON value is not a
     * {@link List} or if no such child exists, then a JSON value containing a
     * {@code null} is returned.
     *
     * @param index
     *            index of child element value to return.
     * @return the child value, or a JSON value containing {@code null}.
     * @throws JsonValueException
     *             if index is negative.
     */
    public JsonValue get(final int index) {
        Object result = null;
        if (index < 0) {
            throw new JsonValueException(this, "List index out of range: " + index);
        }
        if (isList() && index >= 0) {
            final List<Object> list = asList();
            if (index < list.size()) {
                result = list.get(index);
            }
        }
        return new JsonValue(result, pointer.child(index));
    }

    /**
     * Returns the specified child value with a pointer, relative to this value
     * as root. If the specified child value does not exist, then {@code null}
     * is returned.
     *
     * @param pointer
     *            the JSON pointer identifying the child value to return.
     * @return the child value, or {@code null} if no such value exists.
     */
    public JsonValue get(final JsonPointer pointer) {
        JsonValue result = this;
        for (final String token : pointer) {
            final JsonValue member = result.get(token);
            if (member.isNull() && !result.isDefined(token)) {
                return null; // undefined value yields null, not a JSON value containing null
            }
            result = member;
        }
        return result;
    }

    /**
     * Returns the specified item value. If no such member value exists, then a
     * JSON value containing {@code null} is returned.
     *
     * @param key
     *            the {@code Map} key or {@code List} index identifying the item
     *            to return.
     * @return a JSON value containing the value or {@code null}.
     */
    public JsonValue get(final String key) {
        Object result = null;
        if (isMap()) {
            result = asMap().get(key);
        } else if (isList()) {
            final List<Object> list = asList();
            final int index = toIndex(key);
            if (index >= 0 && index < list.size()) {
                result = list.get(index);
            }
        }
        return new JsonValue(result, pointer.child(key));
    }

    /**
     * Returns the raw Java object representing this JSON value.
     *
     * @return the raw Java object representing this JSON value.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Returns the pointer of the JSON value in its JSON structure.
     *
     * @return the pointer of the JSON value in its JSON structure.
     */
    public JsonPointer getPointer() {
        return pointer;
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Boolean}.
     *
     * @return {@code true} if the JSON value is a {@link Boolean}.
     */
    public boolean isBoolean() {
        return (object != null && object instanceof Boolean);
    }

    /**
     * Returns {@code true} if this JSON value contains the specified item.
     *
     * @param key
     *            the {@code Map} key or {@code List} index of the item to seek.
     * @return {@code true} if this JSON value contains the specified member.
     * @throws NullPointerException
     *             if {@code key} is {@code null}.
     */
    public boolean isDefined(final String key) {
        boolean result = false;
        if (isMap()) {
            result = asMap().containsKey(key);
        } else if (isList()) {
            final int index = toIndex(key);
            result = (index >= 0 && index < asList().size());
        }
        return result;
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Set}.
     *
     * @return {@code true} if the JSON value is a {@link Set}.
     */
    public boolean isSet() {
        return isCollection();
    }
    
    /**
     * Returns {@code true} if the JSON value is a {@link Collection}.
     *
     * @return {@code true} if the JSON value is a {@link Collection}.
     */
    public boolean isCollection() {
        return (object instanceof Collection);
    }

    /**
     * Returns {@code true} if the JSON value is a {@link List}.
     *
     * @return {@code true} if the JSON value is a {@link List}.
     */
    public boolean isList() {
        return (object instanceof List);
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Map}.
     *
     * @return {@code true} if the JSON value is a {@link Map}.
     */
    public boolean isMap() {
        return (object instanceof Map);
    }

    /**
     * Returns {@code true} if the value is {@code null}.
     *
     * @return {@code true} if the value is {@code null}.
     */
    public boolean isNull() {
        return (object == null);
    }

    /**
     * Returns {@code true} if the value is not {@code null}.
     *
     * @return {@code true} if the value is not {@code null}.
     */
    public boolean isNotNull() {
        return !isNull();
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Number}.
     *
     * @return {@code true} if the JSON value is a {@link Number}.
     */
    public boolean isNumber() {
        return (object != null && object instanceof Number);
    }

    /**
     * Returns {@code true} if the JSON value is a {@link String}.
     *
     * @return {@code true} if the JSON value is a {@link String}.
     */
    public boolean isString() {
        return (object != null && object instanceof String);
    }

    /**
     * Returns an iterator over the child values that this JSON value contains.
     * If this value is a {@link Map}, then the order of the
     * resulting child values is undefined. Calling the {@link Iterator#remove()}
     * method of the returned iterator will throw a {@link UnsupportedOperationException}.
     *
     * @return an iterator over the child values that this JSON value contains.
     */
    @Override
    public Iterator<JsonValue> iterator() {
        if (isList()) { // optimize for list
            return new Iterator<JsonValue>() {
                int cursor = 0;
                Iterator<Object> i = asList().iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public JsonValue next() {
                    final Object element = i.next();
                    return new JsonValue(element, pointer.child(cursor++));
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return new Iterator<JsonValue>() {
                Iterator<String> i = keys().iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public JsonValue next() {
                    return get(i.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Returns the set of keys for this JSON value's child values. If this value
     * is a {@code Map}, then the order of the resulting keys is the same as the
     * underlying Map implementation. If there are no child values, this method
     * returns an empty set.
     *
     * @return the set of keys for this JSON value's child values.
     */
    public Set<String> keys() {
        if (isMap()) {
            return asMap().keySet();
        } else if (isList()) {
            return new AbstractSet<String>() {
                final RangeSet range = new RangeSet(JsonValue.this.size()); // 0 through size-1 inclusive

                @Override
                public boolean contains(final Object o) {
                    boolean result = false;
                    if (o instanceof String) {
                        try {
                            result = range.contains(Integer.valueOf((String) o));
                        } catch (final NumberFormatException nfe) {
                            // ignore; yields false
                        }
                    }
                    return result;
                }

                @Override
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        Iterator<Integer> i = range.iterator();

                        @Override
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        @Override
                        public String next() {
                            return i.next().toString();
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    return range.size();
                }
            };
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Sets the value of the specified child list element.
     *
     * @param index
     *            the {@code List} index identifying the child value to set.
     * @param object
     *            the Java value to assign to the list element.
     * @return this JSON value.
     * @throws JsonValueException
     *             if this JSON value is not a {@code List} or index is out of
     *             range.
     */
    public JsonValue put(final int index, final Object object) {
        final List<Object> list = required().asList();
        if (index < 0 || index > list.size()) {
            throw new JsonValueException(this, "List index out of range: " + index);
        } else if (index == list.size()) { // appending to end of list
            list.add(object);
        } else { // replacing existing element
            list.set(index, object);
        }
        return this;
    }

    /**
     * Sets the value identified by the specified pointer, relative to this
     * value as root. If doing so would require the creation of a new object or
     * list, a {@code JsonValueException} will be thrown.
     * <p>
     * NOTE: values may be added to a list using the reserved JSON pointer token
     * "-". For example, the pointer "/a/b/-" will add a new element to the list
     * referenced by "/a/b".
     *
     * @param pointer
     *            identifies the child value to set.
     * @param object
     *            the Java object value to set.
     * @return this JSON value.
     * @throws JsonValueException
     *             if the specified pointer is invalid.
     */
    public JsonValue put(final JsonPointer pointer, final Object object) {
        navigateToParentOf(pointer).required().putToken(pointer.leaf(), object);
        return this;
    }

    /**
     * Sets the value of the specified member.  
     * <p>
     * If setting a list element, the specified key must be parseable as an
     * unsigned base-10 integer and be less than or equal to the size of the
     * list.
     *
     * @param key
     *            the {@code Map} key or {@code List} index identifying the
     *            child value to set.
     * @param object
     *            the object value to assign to the member.
     * @return this JSON value.
     * @throws JsonValueException
     *             if this JSON value is not a {@code Map} or {@code List}.
     * @throws NullPointerException
     *             if {@code key} is {@code null}.
     */
    public JsonValue put(final String key, final Object object) {
        if (key == null) {
            throw new NullPointerException();
        } else if (isMap()) {
            asMap().put(key, object);
        } else if (isList()) {
            put(toIndex(key), object);
        } else {
            throw new JsonValueException(this, "Expecting a Map or List");
        }
        return this;
    }

    /**
     * Sets the value identified by the specified pointer, relative to this
     * value as root. Missing parent objects or lists will be created on demand.
     * <p>
     * NOTE: values may be added to a list using the reserved JSON pointer token
     * "-". For example, the pointer "/a/b/-" will add a new element to the list
     * referenced by "/a/b".
     *
     * @param pointer
     *            identifies the child value to set.
     * @param object
     *            the Java object value to set.
     * @return this JSON value.
     * @throws JsonValueException
     *             if the specified pointer is invalid.
     */
    public JsonValue putPermissive(final JsonPointer pointer, final Object object) {
        navigateToParentOfPermissive(pointer).putToken(pointer.leaf(), object);
        return this;
    }

    /**
     * Removes the specified child value, shifting any subsequent elements to
     * the left. If the JSON value is not a {@code List}, calling this method
     * has no effect.
     *
     * @param index
     *            the {@code List} index identifying the child value to remove.
     */
    public void remove(final int index) {
        if (index >= 0 && isList()) {
            final List<Object> list = asList();
            if (index < list.size()) {
                list.remove(index);
            }
        }
    }

    /**
     * Removes the specified child value with a pointer, relative to this value
     * as root. If the specified child value is not defined, calling this method
     * has no effect.
     *
     * @param pointer
     *            the JSON pointer identifying the child value to remove.
     */
    public void remove(final JsonPointer pointer) {
        navigateToParentOf(pointer).remove(pointer.leaf());
    }

    /**
     * Removes the specified child value. If the specified child value is not
     * defined, calling this method has no effect.
     *
     * @param key
     *            the {@code Map} key or {@code List} index identifying the
     *            child value to remove.
     */
    public void remove(final String key) {
        if (isMap()) {
            asMap().remove(key);
        } else if (isList()) {
            remove(toIndex(key));
        }
    }

    /**
     * Throws a {@code JsonValueException} if the JSON value is {@code null}.
     *
     * @return this JSON value.
     * @throws JsonValueException
     *             if the JSON value is {@code null}.
     */
    public JsonValue required() {
        if (object == null) {
            throw new JsonValueException(this, "Expecting a value");
        }
        return this;
    }

    /**
     * Sets the Java object representing this JSON value.
     * <p>
     * This method will automatically unwrap {@link JsonValue} objects.
     *
     * @param object
     *            the object to set.
     */
    public void setObject(final Object object) {
        this.object = object;
        if (object instanceof JsonValue) {
            final JsonValue jv = (JsonValue) object;
            this.object = jv.object;
        }
    }

    /**
     * Returns the number of values that this JSON value contains.
     *
     * @return the number of values that this JSON value contains.
     */
    public int size() {
        if (isMap()) {
            return asMap().size();
        } else if (isCollection()) {
            return asCollection().size();
        } else {
            return 0;
        }
    }

    /**
     * Returns a string representation of the JSON value. The result
     * resembles—but is not guaranteed to conform to—JSON syntax. This method
     * does not apply transformations to the value's children.
     *
     * @return a string representation of the JSON value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (isNull()) {
            sb.append("null");
        } else if (isMap()) {
            sb.append("{ ");
            final Map<Object, Object> map = (Map<Object, Object>) object;
            for (final Iterator<Object> i = map.keySet().iterator(); i.hasNext();) {
                final Object key = i.next();
                sb.append('"');
                appendEscapedString(sb, key.toString());
                sb.append("\": ");
                sb.append(new JsonValue(map.get(key)).toString()); // recursion
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(" }");
        } else if (isCollection()) {
            sb.append("[ ");
            for (final Iterator<Object> i = ((Collection<Object>) object).iterator(); i.hasNext();) {
                sb.append(new JsonValue(i.next()).toString()); // recursion
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(" ]");
        } else if (isString()) {
            sb.append('"');
            appendEscapedString(sb, object.toString());
            sb.append('"');
        } else {
            sb.append(object.toString());
        }
        return sb.toString();
    }

    /**
     * As per json.org a string is any Unicode character except " or \ or
     * control characters. Special characters will be escaped using a \ as
     * follows:
     * <ul>
     * <li> {@literal \ "} - double quote
     * <li> {@literal \ \} - back slash
     * <li> {@literal \ b} - backspace
     * <li> {@literal \ f} - form feed
     * <li> {@literal \ n} - new line
     * <li> {@literal \ r} - carriage return
     * <li> {@literal \ t} - tab
     * <li> {@literal \ u xxxx} - other control characters.
     * </ul>
     */
    private static void appendEscapedString(final StringBuilder sb, final String s) {
        final int size = s.length();
        for (int i = 0; i < size; i++) {
            final char c = s.charAt(i);
            switch (c) {
            // Escape characters which must be escaped.
            case '"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            // Escape common controls to the C equivalent to make them easier to read.
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if (Character.isISOControl(c)) {
                    final String hex = Integer.toHexString(c).toUpperCase(Locale.ENGLISH);
                    final int hexPadding = 4 - hex.length();
                    sb.append("\\u");
                    for (int j = 0; j < hexPadding; j++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                } else {
                    sb.append(c);
                }
            }
        }
    }

    private void addToken(final String token, final Object object) {
        if (isEndOfListToken(token) && isList()) {
            add(object);
        } else {
            add(token, object);
        }
    }

    private boolean isEndOfListToken(final String token) {
        return token.equals("-");
    }

    private boolean isIndexToken(final String token) {
        if (token.isEmpty()) {
            return false;
        } else {
            for (int i = 0; i < token.length(); i++) {
                final char c = token.charAt(i);
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    private JsonValue navigateToParentOf(final JsonPointer pointer) {
        JsonValue jv = this;
        final int size = pointer.size();
        for (int n = 0; n < size - 1; n++) {
            jv = jv.get(pointer.get(n));
            if (jv.isNull()) {
                break;
            }
        }
        return jv;
    }

    private JsonValue navigateToParentOfPermissive(final JsonPointer pointer) {
        JsonValue jv = this;
        final int size = pointer.size();
        for (int n = 0; n < size - 1; n++) {
            final String token = pointer.get(n);
            final JsonValue next = jv.get(token);
            if (next.isNotNull()) {
                jv = next;
            } else if (isIndexToken(token)) {
                throw new JsonValueException(this, "Expecting a value");
            } else {
                // Create the field based on the type of the next token.
                final String nextToken = pointer.get(n + 1);
                if (isEndOfListToken(nextToken)) {
                    jv.add(token, new ArrayList<>());
                    jv = jv.get(token);
                } else if (isIndexToken(nextToken)) {
                    throw new JsonValueException(this, "Expecting a value");
                } else {
                    jv.add(token, new LinkedHashMap<>());
                    jv = jv.get(token);
                }
            }
        }
        return jv;
    }

    private void putToken(final String token, final Object object) {
        if (isEndOfListToken(token) && isList()) {
            add(object);
        } else {
            put(token, object);
        }
    }

    /**
     * Performs a deep comparison of this JSON value with another JSON value, and returns whether the two objects
     * are identical.  Fails fast in that a {@code false} is returned as soon as a difference is detected.
     * <p>
     *     <b>Note:</b> Only values recognisable as JSON primitives ({@link Map}, {@link List}, {@link Number},
     *     {@link Boolean}, {@link String} and {@code null}) are supported.
     * </p>
     * @param other another value.
     * @return whether the two objects are equal.
     * @throws NullPointerException if {@code other} is {@code null}.
     * @throws IllegalArgumentException if this or the {@code other} value contains non-JSON primitive values.
     */
    public boolean isEqualTo(JsonValue other) {
        return JsonPatch.isEqual(this, other);
    }

    /**
     * Performs a deep comparison of this JSON vlaue with another JSON value, and produces a
     * JSON Patch value, which contains the operations necessary to modify the current value
     * to arrive at the {@code target} value.
     *
     * @param target the intended target value.
     * @return the resulting JSON Patch value.
     * @throws NullPointerException if either of {@code original} or {@code target} are {@code null}.
     */
    public JsonValue diff(JsonValue target) {
        return JsonPatch.diff(this, target);
    }

    /**
     * Applies a set of modifications in a JSON patch value to the current object, resulting
     * in the intended target value. In the event of a failure, this method does not revert
     * any modifications applied up to the point of failure.
     *
     * @param patch the JSON Patch value, specifying the modifications to apply to the original value.
     * @throws JsonValueException if application of the patch failed.
     */
    public void patch(JsonValue patch) {
        JsonPatch.patch(this, patch);
    }
    
    @Deprecated
    public URI asURI() {
        return as(JsonValueFunctions.uri());
    }

}
