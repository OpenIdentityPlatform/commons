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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jose.jwt;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class JWObject {

    private final JsonValue jsonValue;

    public JWObject() {
        this.jsonValue = new JsonValue(new HashMap<String, Object>());
    }

    protected void checkValueIsOfType(Object value, Class<?> requiredClazz) {
        if (!requiredClazz.isAssignableFrom(value.getClass())) {
            //TODO throw appropriate exception
            throw new RuntimeException("Casting exception!");
        }
    }

    protected void checkListValuesAreOfType(List value, Class<?> requiredClazz) {
        if (value.size() > 0) {
            checkValueIsOfType(value.get(0), requiredClazz);
        }
    }

    protected boolean isValueOfType(Object value, Class<?> requiredClazz) {
        return requiredClazz.isAssignableFrom(value.getClass());
    }

    public void put(String key, Object value) throws JsonValueException {
        if (value != null) {
            jsonValue.put(key, value);
        } else if (jsonValue.isDefined(key)) {
            jsonValue.remove(key);
        }
    }

    public JsonValue get(String key) {
        return jsonValue.get(key);
    }

    public boolean isDefined(String key) {
        return jsonValue.isDefined(key);
    }

    public Set<String> keys() {
        return jsonValue.keys();
    }

    @Override
    public String toString() {
        return jsonValue.toString();//.replaceAll("\\s",""); //TODO need to test this out as seemed to break the session-jwt in the bridge for some reason?...
    }
}
