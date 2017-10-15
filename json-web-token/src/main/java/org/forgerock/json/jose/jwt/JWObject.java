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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.utils.Utils;

/**
 * A base implementation class for a JSON Web object.
 * <p>
 * Provides a set of methods which are common across JWT, JWS, JWE and JWK implementations.
 *
 * @since 2.0.0
 */
public abstract class JWObject {

    private final JsonValue jsonValue;

    /**
     * Constructs a new, empty JWObject.
     */
    public JWObject() {
        this.jsonValue = new JsonValue(new LinkedHashMap<>());
    }

    /**
     * Checks that the given value is of an assignable type from the required class.
     * <p>
     * Will throw a JwtRuntimeException if the value is not of the required type
     *
     * @param value The value to check is of the required type.
     * @param requiredClazz The class of the required type.
     * @see #isValueOfType(Object, Class)
     */
    protected void checkValueIsOfType(Object value, Class<?> requiredClazz) {
        if (!requiredClazz.isAssignableFrom(value.getClass())) {
            throw new JwtRuntimeException("Value is not of the required type. Required, " + requiredClazz.getName()
                    + ", actual, " + value.getClass().getName());
        }
    }

    /**
     * Checks that the given List's type is of an assignable type from the required class.
     * <p>
     * Will throw a JwtRuntimeException if the value is not of the required type
     *
     * @param value The List to check the type is of the required type.
     * @param requiredClazz The class of the required type.
     * @see #checkValueIsOfType(Object, Class)
     */
    protected void checkListValuesAreOfType(List<?> value, Class<?> requiredClazz) {
        if (value.size() > 0) {
            checkValueIsOfType(value.get(0), requiredClazz);
        }
    }

    /**
     * Checks to see if the given value is of an assignable type from the required class.
     *
     * @param value The value to check is of the required type.
     * @param requiredClazz The class of the required type.
     * @return <code>true</code> if the value if of the required type.
     * @see #checkValueIsOfType(Object, Class)
     */
    protected boolean isValueOfType(Object value, Class<?> requiredClazz) {
        return requiredClazz.isAssignableFrom(value.getClass());
    }

    /**
     * Sets or removes the value of the specified member.
     * <p>
     * If the value is not null, then the value is set as the value of the given key.
     * <p>
     * Otherwise, if the value is null and the key already exist with a value assigned to it, then the key and its value
     * will be removed. If the specified key is not defined, calling this method has no effect.
     *
     * @param key the {@code Map} key identifying the value to set or to remove.
     * @param value the object value to assign to the member.
     */
    public void put(String key, Object value) {
        if (value != null) {
            jsonValue.put(key, value);
        } else if (jsonValue.isDefined(key)) {
            jsonValue.remove(key);
        }
    }

    /**
     * Returns the specified item value. If no such member value exists, then a JSON value containing {@code null} is
     * returned.
     *
     * @param key the {@code Map} key identifying the item to return.
     * @return a JSON value containing the value or {@code null}.
     */
    public JsonValue get(String key) {
        return jsonValue.get(key);
    }

    /**
     * Returns {@code true} if this JWObject contains the specified item.
     *
     * @param key the {@code Map} key of the item to seek.
     * @return {@code true} if this JSON value contains the specified member.
     */
    public boolean isDefined(String key) {
        return jsonValue.isDefined(key);
    }

    /**
     * Returns the set of keys for this JWObject's values.
     * <p>
     * The order of the resulting keys is undefined. If there are no values set, this method returns an empty set.
     *
     * @return A Set of keys.
     */
    public Set<String> keys() {
        return jsonValue.keys();
    }

    /**
     * Returns the {@code Map} of keys and values stored by {@link #put}.
     *
     * @return {@code Map} of this JWObject's keys and values.
     */
    Map<String, Object> getAll() {
        return jsonValue.asMap();
    }

    /**
     * Returns a string representation of the JWObject. The result is guaranteed to be valid JSON object syntax.
     *
     * @return A JSON String representation.
     */
    @Override
    public String toString() {
        return Utils.writeJsonObject(jsonValue.asMap());
    }
}
