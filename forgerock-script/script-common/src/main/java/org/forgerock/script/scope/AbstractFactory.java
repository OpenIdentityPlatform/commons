/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.scope;

import org.forgerock.util.Factory;
import org.forgerock.util.LazyList;
import org.forgerock.util.LazyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An AbstractFactory does ...
 *
 * @author Laszlo Hordos
 */
public abstract class AbstractFactory<T> implements Factory<T> {

    protected final T source;

    protected AbstractFactory(final T source) {
        this.source = source;
    }

    public abstract Parameter getParameter();

    /**
     * Gets the {@code null} object representation.
     * <p/>
     * If the {@code null} object has special representation in the script scope
     * this method returns with that object.
     *
     * @return {@code null} or representation of {@code null} object.
     */
    protected Object convertNull() {
        return null;
    }

    protected Object convertBoolean(Boolean source) {
        return source;
    }

    protected Object convertNumber(Number source) {
        return source;
    }

    protected Object convertString(String source) {
        return source;
    }

    protected Object convertList(List<Object> source) {
        return source;
    }

    protected Object convertMap(Map<String, Object> source) {
        return source;
    }

    protected Object convertObject(Object source) {
        return source;
    }

    protected abstract Factory<List<Object>> newListFactory(List<Object> source);

    protected abstract Factory<Map<String, Object>> newMapFactory(Map<String, Object> source);

    protected abstract Object convertFunction(Function<?> source);

    protected Object process(Object source) {
        if (null == source) {
            return convertNull();
        }
        Class sourceClass = source.getClass();
        if ((Number.class.isAssignableFrom(sourceClass)) || (int.class == sourceClass)
                || (double.class == sourceClass) || (float.class == sourceClass)
                || (long.class == sourceClass)) {
            return convertNumber((Number) source);
        } else if ((Boolean.class.isAssignableFrom(sourceClass)) || (boolean.class == sourceClass)) {
            return convertBoolean((Boolean) source);
        } else if (String.class.isAssignableFrom(sourceClass)) {
            return convertString((String) source);
        } else if (Map.class.isAssignableFrom(sourceClass)) {
            return convertMap(new LazyMap<String, Object>(
                    newMapFactory((Map<String, Object>) source)));
        } else if (List.class.isAssignableFrom(sourceClass)) {
            return convertList(new LazyList<Object>(newListFactory((List<Object>) source)));
        } else if (source instanceof Function) {
            return convertFunction((Function) source);
        } else {
            return convertObject(source);
        }
    }

    public static abstract class MapFactory extends AbstractFactory<Map<String, Object>> {

        protected MapFactory(final Map<String, Object> source) {
            super(source);
        }

        @Override
        public Map<String, Object> newInstance() {
            final Map<String, Object> target = new HashMap<String, Object>(source.size());
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                target.put(entry.getKey(), process(entry.getValue()));
            }
            return target;
        }
    }

    public static abstract class ListFactory extends AbstractFactory<List<Object>> {

        protected ListFactory(final List<Object> source) {
            super(source);
        }

        @Override
        public List<Object> newInstance() {
            final List<Object> target = new ArrayList<Object>(source.size());
            for (Object o : source) {
                target.add(process(o));
            }
            return target;
        }
    }

}
