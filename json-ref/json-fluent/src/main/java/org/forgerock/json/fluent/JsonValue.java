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
 * Portions Copyrighted 2011 ForgeRock AS.
 */

package org.forgerock.json.fluent;

// Java SE
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.UUID;

// Utilities
import org.forgerock.util.RangeSet;

/**
 * Represents a value in a JSON object model structure. JSON values are represented with
 * standard Java objects: {@link String}, {@link Number}, {@link Map}, {@link List},
 * {@link Boolean} and {@code null}.
 * <p>
 * A JSON value may have one or more transformers associated with it. Transformers apply
 * transformations to the JSON value upon construction, and upon members as they are retrieved.
 * Transformers are applied iteratively, in the sequence they appear within the list. If a
 * transformer affects the value, then all transformers are re-applied, in sequence. This
 * repeats until the value is no longer affected. Transformers are inherited by and applied
 * to member values.
 *
 * @author Paul C. Bryan
 */
public class JsonValue implements Cloneable, Iterable<JsonValue> {

    /** Transformers to apply to the value; are inherited by its members. */
    private final ArrayList<JsonTransformer> transformers = new ArrayList<JsonTransformer>();

    /** The pointer to the value within a JSON structure. */
    private JsonPointer pointer;

    /** The Java object representing this JSON value. */
    private Object object;

    /**
     * Unwraps a {@link JsonValueWrapper} and/or {@link JsonValue} object. If nothing was
     * unwrapped, then {@code null} is returned.
     */
    private JsonValue unwrapObject(Object object) {
        JsonValue result = null;
        if (object != null && object instanceof JsonValueWrapper) {
            object = ((JsonValueWrapper)object).unwrap();
        }
        if (object != null && object instanceof JsonValue) {
            result = (JsonValue)object;
        }
        return result;
    }

    /**
     * Constructs a JSON value object with given object, pointer and transformers.
     *
     * This constructor will automatically unwrap any {@link JsonValueWrapper} and/or
     * {@link JsonValue} objects. The pointer is inherited from the wrapped value, except
     * if {@code pointer} is not {@code null}. The transformers are inherited from the
     * wrapped value, except if {@code transformers} is not {@code null}.
     *
     * @param object the Java object representing the JSON value.
     * @param pointer the pointer to the value in a JSON structure.
     * @param transformers a list of transformers to apply the value and its members.
     * @throws JsonException if a transformer failed during value initialization.
     */
    public JsonValue(Object object, JsonPointer pointer,
    Collection<? extends JsonTransformer> transformers) throws JsonException {
        this.object = object;
        this.pointer = pointer;
        JsonValue jv = unwrapObject(object);
        if (jv != null) {
            this.object = jv.object;
            if (pointer == null) {
                this.pointer = jv.pointer;
            }
            if (transformers == null) {
                this.transformers.addAll(jv.transformers);
            }
        }
        if (transformers != null) {
            this.transformers.addAll(transformers);
        }
        if (this.pointer == null) {
            this.pointer = new JsonPointer();
        }
        if (this.transformers.size() > 0) {
            applyTransformers();
        }
    }

    /**
     * Constructs a JSON value object with a given object and transformers.
     * This constructor will automatically unwrap any {@link JsonValueWrapper} and/or
     * {@link JsonValue} objects.
     *
     * @param object the Java object representing the JSON value.
     * @param transformers a list of transformers to apply the value and its members.
     * @throws JsonException if a transformer failed during value initialization.
     */
    public JsonValue(Object object, Collection<? extends JsonTransformer> transformers) {
        this(object, null, transformers);
    }

    /**
     * Constructs a JSON value object with a given object and pointer.
     * This constructor will automatically unwrap any {@link JsonValueWrapper} and/or
     * {@link JsonValue} objects.
     *
     * @param object the Java object representing the JSON value.
     * @param pointer the pointer to the value in a JSON structure.
     */
    public JsonValue(Object object, JsonPointer pointer) {
        this(object, pointer, null);
    }

    /**
     * Constructs a JSON value object with a given object.
     * This constructor will automatically unwrap any {@link JsonValueWrapper} and/or
     * {@link JsonValue} objects.
     *
     * @param object the Java object representing JSON value.
     */
    public JsonValue(Object object) {
        this(object, null, null);
    }

    /**
     * Returns the raw Java object representing this JSON value.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Returns a Java object representing this JSON value. If the object is a {@code Map} or
     * {@code List}, it is wrapped with a {@link JsonValueMap} or {@link JsonValueList}
     * object respectively. This maintains and applies transformations as these objects
     * (and their children) are accessed.
     */
    public Object getWrappedObject() {
        if (isMap()) {
            return new JsonValueMap(this);
        } else if (isList()) {
            return new JsonValueList<Object>(this);
        } else {
            return object;
        }
    }

    /**
     * Sets the Java object representing this JSON value. Does not apply transformers to
     * the new value.
     * <p>
     * This method will automatically unwrap any {@link JsonValueWrapper} and/or
     * {@link JsonValue} objects. Transformers are inherited from the wrapped value.
     * This value's pointer remains unaffected.
     *
     * @param object the object to set.
     */
    public void setObject(Object object) {
        this.object = object;
        JsonValue jv = unwrapObject(object);
        if (jv != null) {
            this.object = jv.object;
            this.transformers.addAll(jv.transformers);
        }
    }

    /**
     * Returns the pointer of the JSON value in its JSON structure.
     */
    public JsonPointer getPointer() {
        return pointer;
    }

    /**
     * Returns the JSON value's list of transformers. This list is modifiable. Child values
     * inherit the list when they are constructed. If any transformers are added to the
     * list, call the {@link #applyTransformers()} method to apply them to the current value.
     */
    public List<JsonTransformer> getTransformers() {
        return transformers;
    }

    /**
     * Returns {@code true} if the values are === equal.
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == o2 || (o1 != null && o1.equals(o2)));
    }

    /**
     * Applies all of the transformations to the value. If a transformer affects the value,
     * then all transformers are re-applied. This repeats until the value is no longer
     * affected.
     * <p>
     * This method has an absurdly high upper-limit of {@link Integer#MAX_VALUE} iterations,
     * beyond which a {@code JsonException} will be thrown.
     *
     * @throws JsonException if there was a failure applying transformation(s)
     */
    public void applyTransformers() throws JsonException {
        Object object = this.object;
        for (int n = 0; n < Integer.MAX_VALUE; n++) {
            boolean affected = false;
            for (JsonTransformer transformer : transformers) {
                transformer.transform(this);
                if (!eq(object, this.object)) { // transformer affected the value
                    object = this.object; // note the new value for next iteration
                    affected = true;
                    break; // reiterate all transformers
                }
            }
            if (!affected) { // full iteration of transformers without affecting value
                return; // success
            }
        }
        throw new JsonException("Transformer iteration overflow");
    }

    /**
     * Throws a {@code JsonValueException} if the JSON value is {@code null}.
     *
     * @throws JsonValueException if the JSON value is {@code null}.
     * @return this JSON value.
     */
    public JsonValue required() throws JsonValueException {
        if (object == null) {
            throw new JsonValueException(this, "Expecting a value");
        }
        return this;
    }

    /**
     * Called to enforce that the JSON value is of a particular type. A value of {@code null}
     * is allowed.
     *
     * @param type the class that the underlying value must have.
     * @return this JSON value.
     * @throws JsonValueException if the value is not the specified type.
     */
    public JsonValue expect(Class<?> type) throws JsonValueException {
        if (object != null && !type.isInstance(object)) {
            throw new JsonValueException(this, "Expecting a " + type.getName());
        }
        return this;
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Map}.
     */
    public boolean isMap() {
        return (object != null && object instanceof Map);
    }

    /**
     * Returns the JSON value as a {@code Map} object. If the JSON value is {@code null}, this
     * method returns {@code null}.
     *
     * @return the map value, or {@code null} if no value.
     * @throws JsonValueException if the JSON value is not a {@code Map}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() throws JsonValueException {
        return (object == null ? null : (Map<String, Object>)(expect(Map.class).object));
    }

    /**
     * Returns {@code true} if the JSON value is a {@link List}.
     */
    public boolean isList() {
        return (object != null && object instanceof List);
    }

    /**
     * Returns the JSON value as a {@link List} object. If the JSON value is {@code null},
     * this method returns {@code null}.
     *
     * @return the list value, or {@code null} if no value.
     * @throws JsonValueException if the JSON value is not a {@code List}.
     */
    public List<Object> asList() throws JsonValueException {
        return asList(Object.class);
    }

    /**
     * Returns the JSON value as a {@link List} containing objects of the specified type. If
     * the value is {@code null}, this method returns {@code null}. If any of the elements
     * of the list are not {@code null} and not of the specified type,
     * {@code JsonValueException} is thrown.
     *
     * @param type the type of object that all elements are expected to be.
     * @return the list value, or {@code null} if no value.
     * @throws JsonValueException if the JSON value is not a {@code List} or contains an unexpected type.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> asList(Class<E> type) throws JsonValueException {
        if (object != null) {
            expect(List.class);
            if (type != Object.class) {
                List<Object> list = (List<Object>)this.object;
                for (Object element : list) {
                    if (element != null && !type.isInstance(element)) {
                        throw new JsonValueException(this, "Expecting a List of " + type.getName() + " elements");
                    }
                }
            }
        }
        return (List<E>)object;
    }

    /**
     * Returns {@code true} if the JSON value is a {@link String}.
     */
    public boolean isString() {
        return (object != null && object instanceof String);
    }

    /**
     * Returns the JSON value as a {@code String} object. If the JSON value is {@code null},
     * this method returns {@code null}.
     *
     * @return the string value.
     * @throws JsonValueException if the JSON value is not a string.
     */
    public String asString() throws JsonValueException {
        return (object == null ? null : (String)(expect(String.class).object));
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Number}.
     */
    public boolean isNumber() {
        return (object != null && object instanceof Number);
    }

    /**
     * Returns the JSON value as a {@code Number} object. If the JSON value is {@code null},
     * this method returns {@code null}.
     *
     * @return the numeric value.
     * @throws JsonValueException if the JSON value is not a number.
     */
    public Number asNumber() throws JsonValueException {
        return (object == null ? null : (Number)(expect(Number.class).object));
    }

    /**
     * Returns the JSON value as an {@link Integer} object. This may involve rounding or
     * truncation. If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the integer value.
     * @throws JsonValueException if the JSON value is not a number.
     */
    public Integer asInteger() throws JsonValueException {
        return (object == null ? null : Integer.valueOf(asNumber().intValue()));
    }

    /**
     * Returns the JSON value as a {@link Double} object. This may involve rounding.
     * If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the double-precision floating point value.
     * @throws JsonValueException if the JSON value is not a number.
     */
    public Double asDouble() throws JsonValueException {
        return (object == null ? null : Double.valueOf(asNumber().doubleValue()));
    }

    /**
     * Returns the JSON value as a {@link Long} object. This may involve rounding or
     * truncation. If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the long integer value.
     * @throws JsonValueException if the JSON value is not a number.
     */
    public Long asLong() throws JsonValueException {
        return (object == null ? null : Long.valueOf(asNumber().longValue()));
    }

    /**
     * Returns {@code true} if the JSON value is a {@link Boolean}.
     */
    public boolean isBoolean() {
        return (object != null && object instanceof Boolean);
    }

    /**
     * Returns the JSON value as a {@link Boolean} object. If the value is {@code null},
     * this method returns {@code null}.
     *
     * @return the boolean value.
     * @throws JsonValueException if the JSON value is not a boolean type.
     */
    public Boolean asBoolean() throws JsonValueException {
        return (object == null ? null : (Boolean)(expect(Boolean.class).object));
    }

    /**
     * Returns {@code true} if the value is {@code null}.
     */
    public boolean isNull() {
        return (object == null);
    }

    /**
     * Returns the JSON string value as an enum constant of the specified enum type.
     * The string value and enum constants are compared, ignoring case considerations.
     * If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @param type the enum type to match constants with the value.
     * @return the enum constant represented by the string value.
     * @throws IllegalArgumentException if {@code type} does not represent an enum type.
     * @throws JsonValueException if the JSON value does not match any of the enum's constants.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    public <T extends Enum<T>> T asEnum(Class<T> type) throws JsonValueException {
        T result = null;
        String string = asString();
        if (string != null) {
            T[] constants = type.getEnumConstants();
            if (constants == null) {
                throw new IllegalArgumentException("Type is not an enum class");
            }
            for (T constant : constants) {
                if (string.equalsIgnoreCase(constant.toString())) {
                    result = constant;
                    break;
                }
            }
            if (result == null) {
                StringBuilder sb = new StringBuilder("Expecting String containing one of:");
                for (T constant : constants) {
                    sb.append(' ').append(constant.toString());
                }
                throw new JsonValueException(this, sb.toString());
            }
        }
        return result;
    }

    /**
     * Returns the JSON string value as a {@code File} object. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return a file represented by the string value.
     * @throws JsonValueException if the JSON value is not a string.
     */
    public File asFile() throws JsonValueException {
        String string = asString();
        return (string != null ? new File(string) : null);
    }

    /**
     * Returns the JSON string value as a character set used for byte encoding/decoding.
     * If the JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the character set represented by the string value.
     * @throws JsonValueException if the JSON value is not a string or the character set specified is invalid.
     */
    public Charset asCharset() throws JsonValueException {
        try {
            return (object == null ? null : Charset.forName(asString()));
        } catch (IllegalCharsetNameException icne) {
            throw new JsonValueException(this, icne);
        } catch (UnsupportedCharsetException uce) {
            throw new JsonValueException(this, uce);
        }
    }

    /**
     * Returns the JSON string value as a regular expression pattern. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the compiled regular expression pattern.
     * @throws JsonValueException if the pattern is not a string or the value is not a valid regular expression pattern.
     */
    public Pattern asPattern() throws JsonValueException {
        try {
            return (object == null ? null : Pattern.compile(asString()));
        } catch (PatternSyntaxException pse) {
            throw new JsonValueException(this, pse);
        }
    }

    /**
     * Returns the JSON string value as a uniform resource identifier. If the JSON value is
     * {@code null}, this method returns {@code null}.
     *
     * @return the URI represented by the string value.
     * @throws JsonValueException if the given string violates URI syntax.
     */
    public URI asURI() throws JsonValueException {
        try {
            return (object == null ? null : new URI(asString()));
        } catch (URISyntaxException use) {
            throw new JsonValueException(this, use);
        }
    }

    /**
     * Returns the JSON string value as a JSON pointer. If the JSON value is {@code null},
     * this method returns {@code null}.
     *
     * @return the JSON pointer represented by the JSON value string.
     * @throws JsonValueException if the JSON value is not a string or valid JSON pointer.
     */
    public JsonPointer asPointer() throws JsonValueException {
        try {
            return (object == null ? null : new JsonPointer(asString()));
        } catch (JsonException je) {
            throw (je instanceof JsonValueException ? je : new JsonValueException(this, je));
        }
    }

    /**
     * Returns the JSON string value as a universally unique identifier (UUID). If the
     * JSON value is {@code null}, this method returns {@code null}.
     *
     * @return the UUID represented by the JSON value string.
     * @throws JsonValueException if the JSON value is not a string or valid UUID.
     */
    public UUID asUUID() throws JsonValueException {
        try {
            return (object == null ? null : UUID.fromString(asString()));
        } catch (IllegalArgumentException iae) {
            throw new JsonValueException(this, iae);
        }
    }

    /**
     * Defaults the JSON value to the specified value if it is currently {@code null}.
     *
     * @param object the object to default to.
     * @return this JSON value or a new JSON value containing the default value.
     */
    public JsonValue defaultTo(Object object) {
        return (this.object != null ? this : new JsonValue(object, this.pointer, this.transformers));
    }

    /**
     * Returns the number of values that this JSON value contains.
     */
    public int size() {
        int result = 0;
        if (isMap()) {
            result = asMap().size();
        } else if (isList()) {
            result = asList().size();
        }
        return result;
    }

    /**
     * Returns the key as an list index value. If the string does not represent a valid
     * list index value, then {@code -1} is returned.
     *
     * @param key the key to be converted into an list index value.
     * @return the converted index value, or {@code -1} if invalid.
     */
    private static int toIndex(String key) {
        int result;
        try {
            result = Integer.parseInt(key);
        } catch (NumberFormatException nfe) {
            result = -1;
        }
        return (result >= 0 ? result : -1);
    }

    /**
     * Returns {@code true} if this JSON value contains the specified item.
     *
     * @param key the {@code Map} key or {@code List} index of the item to seek.
     * @return {@code true} if this JSON value contains the specified member.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public boolean isDefined(String key) {
        boolean result = false;
        if (isMap()) {
            result = asMap().containsKey(key);
        } else if (isList()) {
            int index = toIndex(key);
            result = (index >= 0 && index < asList().size());
        }
        return result;
    }

    /**
     * Returns {@code true} this JSON value contains an item with the specified value.
     *
     * @param object the object to seek within this JSON value.
     * @return {@code true} if this value contains the specified member value.
     */
    public boolean contains(Object object) {
        boolean result = false;
        if (isMap()) {
            result = asMap().containsValue(object);
        } else if (isList()) {
            result = asList().contains(object);
        }
        return result;
    }

    /**
     * Returns the specified item value. If no such member value exists, then a JSON value
     * containing {@code null} is returned.
     *
     * @param key the {@code Map} key or {@code List} index identifying the item to return.
     * @return a JSON value containing the value or {@code null}.
     * @throws JsonException if a transformer failed to transform the child value.
     */
    public JsonValue get(String key) throws JsonException {
        Object result = null;
        if (isMap()) {
            result = asMap().get(key);
        } else if (isList()) {
            List<Object> list = asList();
            int index = toIndex(key);
            if (index >= 0 && index < list.size()) {
                result = list.get(index);
            }
        }
        return new JsonValue(result, pointer.child(key), transformers);
    }

    /**
     * Returns the specified child value. If this JSON value is not a {@link List} or if no
     * such child exists, then a JSON value containing a {@code null} is returned.
     *
     * @param index index of child element value to return.
     * @return the child value, or a JSON value containing {@code null}.
     * @throws JsonValueException if index is negative.
     * @throws JsonException if a transformer failed to transform the child value.
     */
    public JsonValue get(int index) throws JsonException {
        Object result = null;
        if (index < 0) {
            throw new JsonValueException(this, "List index out of range: " + index);
        }
        if (isList() && index >= 0) {
            List<Object> list = asList();
            if (index < list.size()) {
                result = list.get(index);
            }
        }
        return new JsonValue(result, pointer.child(index), transformers);
    }

    /**
     * Returns the specified child value with a pointer, relative to this value as root.
     * If the specified child value does not exist, then {@code null} is returned.
     *
     * @param pointer the JSON pointer identifying the child value to return.
     * @return the child value, or {@code null} if no such value exists.
     * @throws JsonException if a transformer failed to transform the resulting value.
     */
    public JsonValue get(JsonPointer pointer) throws JsonException {
        JsonValue result = this;
        for (String token : pointer) {
            JsonValue member = result.get(token);
            if (member.isNull() && !result.isDefined(token)) {
                return null; // undefined value yields null, not a JSON value containing null
            }
            result = member;
        }
        return result;
    }

    /**
     * Sets the value of the specified member.
     * <p>
     * If setting a list element, the specified key must be parseable as an unsigned
     * base-10 integer and be less than or equal to the size of the list.
     *
     * @param key the {@code Map} key or {@code List} index identifying the child value to set.
     * @param object the object value to assign to the member.
     * @throws JsonValueException if this JSON value is not a {@code Map} or {@code List}.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public void put(String key, Object object) throws JsonValueException {
        if (key == null) {
            throw new NullPointerException();
        }
        if (isMap()) {
            asMap().put(key, object);
        } else if (isList()) {
            put(toIndex(key), object);
        } else {
            throw new JsonValueException(this, "Expecting a Map or List");
        }
    }

    /**
     * Sets the value of the specified child list element.
     *
     * @param index the {@code List} index identifying the child value to set.
     * @param object the Java value to assign to the list element.
     * @throws JsonValueException if this JSON value is not a {@code List} or index is out of range.
     */
    public void put(int index, Object object) throws JsonValueException {
        List<Object> list = required().asList();
        if (index < 0 || index > list.size()) {
            throw new JsonValueException(this, "List index out of range: " + index);
        } else if (index == list.size()) { // appending to end of list
            list.add(object);
        } else { // replacing existing element
            list.set(index, object);
        }
    }

    /**
     * Sets the value of the value identified by the specified pointer, relative to this value
     * as root. If doing so would require the creation of a new object or list, a
     * {@code JsonValueException} will be thrown.
     *
     * @param pointer identifies the child value to set.
     * @param object the Java object value to set.
     * @throws JsonValueException if the specified pointer is invalid.
     */
    public void put(JsonPointer pointer, Object object) throws JsonValueException {
        JsonValue jv = this;
        String[] tokens = pointer.toArray();
        for (int n = 0; n < tokens.length -1; n++) {
            jv = jv.get(tokens[n]).required();
        }
        jv.put(tokens[tokens.length - 1], object);
    }

    /**
     * Removes the specified child value. If the specified child value is not defined, calling
     * this method has no effect.
     *
     * @param key the {@code Map} key or {@code List} index identifying the child value to remove.
     */
    public void remove(String key) {
        if (isMap()) {
            asMap().remove(key);
        } else if (isList()) {
            remove(toIndex(key));
        }
    }

    /**
     * Removes the specified child value, shifting any subsequent elements to the left. If the
     * JSON value is not a {@code List}, calling this method has no effect.
     *
     * @param index the {@code List} index identifying the child value to remove.
     */
    public void remove(int index) {
        if (index >= 0 && isList()) {
            List<Object> list = asList();
            if (index < list.size()) {
                list.remove(index);
            }
        }
    }

    /**
     * Removes all child values from this JSON value, if it has any.
     */
    public void clear() throws JsonValueException {
        if (isMap()) {
            asMap().clear();
        } else if (isList()) {
            asList().clear();
        }
    }

    /**
     * Adds the specified value.
     * <p>
     * If adding to a list value, the specified key must be parseable as an unsigned
     * base-10 integer and be less than or equal to the list size. Adding a value to a list
     * shifts any existing elements at or above the specified index to the right by one.
     *
     * @param key the {@code Map} key or {@code List} index to add.
     * @param object the Java object to add.
     * @throws JsonValueException if not a {@code Map} or {@code List}, the {@code Map} key already exists, or the {@code List} index is out of range.
     */
    public void add(String key, Object object) throws JsonValueException {
        if (isMap()) {
            Map<String, Object> map = asMap();
            if (map.containsKey(key)) {
                throw new JsonValueException(this, "Map key " + key + " already exists");
            }
            map.put(key, object);
        } else if (isList()) {
            add(toIndex(key), object);
        } else {
            throw new JsonValueException(this, "Expecting a Map or List");
        }
    }

    /**
     * Adds the specified value to the list. Adding a value to a list shifts any existing
     * elements at or above the specified index to the right by one.
     *
     * @param index the {@code List} index of the value to add.
     * @param object the java object to add.
     * @throws JsonValueException if this JSON value is not a {@code List} or index is out of range.
     */
    public void add(int index, Object object) throws JsonValueException {
        List<Object> list = required().asList();
        if (index < 0 || index > list.size()) {
            throw new JsonValueException(this, "List index out of range: " + index);
        }
        list.add(index, object);
    }

    /**
     * Returns the set of keys for this JSON value's child values. If this value is a
     * {@code Map}, then the order of the resulting keys is undefined. If there are no child
     * values, this method returns an empty set.
     */
    public Set<String> keys() {
        Set<String> result;
        if (isMap()) {
            result = new HashSet<String>();
            for (Object key : asMap().keySet()) {
                if (key instanceof String) {
                    result.add((String)key); // only expose string keys in map
                }
            }
        } else if (isList()) {
            result = new AbstractSet<String>() {
                RangeSet range = new RangeSet(size()); // 0 through size-1 inclusive
                @Override public int size() {
                    return range.size();
                }
                @Override public boolean contains(Object o) {
                    boolean result = false;
                    if (o instanceof String) {
                        try {
                            result = range.contains(Integer.valueOf((String)o));
                        } catch (NumberFormatException nfe) {
                            // ignore; yields false
                        }
                    }
                    return result;
                }
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        Iterator<Integer> i = range.iterator();
                        public boolean hasNext() {
                            return i.hasNext();
                        }
                        public String next() {
                            return i.next().toString();
                        }
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    /**
     * Returns an iterator over the child values that this JSON value contains. If this value
     * is a {@link Map}, then the order of the resulting child values is undefined.
     * Calling the {@link Iterator#remove()} method of the returned iterator will throw a
     * {@link UnsupportedOperationException}.
     * <p>
     * Note: calls to the {@code next()} method may throw the runtime {@link JsonException}
     * if any transformers fail to execute.
     */
    public Iterator<JsonValue> iterator() {
        if (isList()) { // optimize for list
            return new Iterator<JsonValue>() {
                int cursor = 0;
                Iterator<Object> i = asList().iterator();
                public boolean hasNext() {
                    return i.hasNext();
                }
                public JsonValue next() {
                    Object element = i.next();
                    return new JsonValue(element, pointer.child(cursor++), transformers);
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return new Iterator<JsonValue>() {
                Iterator<String> i = keys().iterator();
                public boolean hasNext() {
                    return i.hasNext();
                }
                public JsonValue next() {
                    return get(i.next());
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Returns a deep copy of this JSON value.
     * <p>
     * This method applies all transformations while traversing the values's members and
     * their members, and so on. Consequently, the returned copy does not include the
     * transformers from this value.
     * <p>
     * Note: This method is recursive, and currently has no ability to detect or correct for
     * structures containing cyclic references. Processing such a structure will result in a
     * {@link StackOverflowError} being thrown.
     */
    public JsonValue copy() {
// TODO: track original values to resolve cyclic references
        JsonValue result = new JsonValue(object, pointer); // start with shallow copy
        if (this.isMap()) {
            HashMap<String, Object> map = new HashMap<String, Object>(size());
            for (String key : keys()) {
                map.put(key, this.get(key).copy().getObject()); // recursion
            }
            result.object = map;
        } else if (isList()) {
            ArrayList<Object> list = new ArrayList<Object>(size());
            for (JsonValue element : this) {
                list.add(element.copy().getObject()); // recursion
            }
            result.object = list;
        }
        return result;
    }

    /**
     * Returns a shallow copy of this JSON value. If this JSON value contains a {@code Map}
     * or a {@code List} object, the returned JSON value will contain a shallow copy of the
     * original contained object.
     * <p>
     * The new value's members can be modified without affecting the original value.
     * Modifying the member's members will almost certainly affect the original value. To
     * avoid this, use the {@link #copy} method to return a deep copy of the JSON value.
     * <p>
     * This method does not traverse the value's members, nor will it apply any
     * transformations.
     */
    @Override
    public JsonValue clone() {
        JsonValue result = new JsonValue(this.object, this.pointer);
        result.transformers.addAll(this.transformers); // avoid re-applying transformers
        if (isMap()) {
            result.object = new HashMap<String, Object>(this.asMap());
        } else if (isList()) {
            result.object = new ArrayList<Object>(this.asList());
        }
        return result;
    }

    /**
     * Returns a string representation of the JSON value. The result resembles—but is not
     * guaranteed to conform to—JSON syntax. This method does not apply transformations to
     * the value's children.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isNull()) {
            sb.append("null");
        } else if (isMap()) {
            sb.append("{ ");
            Map<Object, Object> map = (Map<Object, Object>)object;
            for (Iterator<Object> i = map.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                sb.append('"').append(key.toString()).append("\": ");
                sb.append(new JsonValue(map.get(key)).toString()); // recursion
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(" }");
        } else if (isList()) {
            sb.append("[ ");
            for (Iterator<Object> i = ((List<Object>)object).iterator(); i.hasNext();) {
                sb.append(new JsonValue(i.next()).toString()); // recursion
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(" ]");
        } else if (isString()) {
            sb.append('"').append(object).append('"');
        } else {
            sb.append(object.toString());
        }
        return sb.toString();
    }
}
