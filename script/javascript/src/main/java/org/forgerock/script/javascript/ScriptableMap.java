/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.javascript;

import org.forgerock.json.JsonValue;
import org.forgerock.script.scope.AbstractFactory;
import org.forgerock.script.scope.Parameter;
import org.forgerock.util.LazyMap;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.util.Map;

/**
 * Provides a {@code Scriptable} wrapper for a {@code Map} object.
 *
 * Rhino 1.7R3 supports native Map
 *
 * @author Paul C. Bryan
 */
class ScriptableMap extends NativeObject implements Wrapper {

    private static final long serialVersionUID = 1L;

    /** The request being wrapped. */
    private final transient Parameter parameter;

    /** The map being wrapped. */
    private final Map<String, Object> map;

    public ScriptableMap(final AbstractFactory.MapFactory factory) {
        if (null == factory) {
            throw new NullPointerException();
        }
        this.parameter = factory.getParameter();
        this.map = new LazyMap<String, Object>(factory);
    }

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param map
     *            the map to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptableMap(final Parameter parameter, final Map<String, Object> map) {
        if (null == map) {
            throw new NullPointerException();
        }
        this.parameter = parameter;
        this.map = map;
    }

    @Override
    public String getClassName() {
        return "ScriptableMap";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {
        if (map.containsKey(name)) {
            return Converter.wrap(parameter, map.get(name), start, map instanceof LazyMap);
        } else {
            return NOT_FOUND;
        }
    }

    @Override
    public Object get(int index, Scriptable start) {
        return has(index, start) ? get(Integer.toString(index), start) : super.get(index, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return (map.containsKey(name));
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return has(Integer.toString(index), start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        try {
            map.put(name, Converter.convert(value));
        } catch (Exception e) {
            throw Context.reportRuntimeError("map prohibits modification");
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        put(Integer.toString(index), start, value);
    }

    @Override
    public void delete(String name) {
        try {
            map.remove(name);
        } catch (Exception e) {
            throw Context.reportRuntimeError("map prohibits modification");
        }
    }

    @Override
    public void delete(int index) {
        delete(Integer.toString(index));
    }

    @Override
    public Object[] getIds() {
        return map.keySet().toArray();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == String.class) {
            return toString();
        } else if (hint == Number.class) {
            return Double.NaN;
        } else if (hint == Boolean.class) {
            return Boolean.TRUE;
        } else {
            return this;
        }
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false; // no support for javascript instanceof
    }

    @Override
    public Object unwrap() {
        return map;
    }

    public String toString() {
        return map == null ? "null" : new JsonValue(map).toString();
    }
}
