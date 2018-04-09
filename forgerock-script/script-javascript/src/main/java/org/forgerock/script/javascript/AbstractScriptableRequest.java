/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
import org.forgerock.json.resource.Request;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Provides a {@code Scriptable} wrapper for an abstract {@code Request} object.
 */
abstract class AbstractScriptableRequest extends NativeObject implements Wrapper {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_METHOD = "method";

    /** The request being wrapped. */
    final transient Parameter parameter;

    /** The request being wrapped. */
    private final Request request;

    /**
     * Constructs a new scriptable wrapper around the specified request.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified request is {@code null}.
     */
    public AbstractScriptableRequest(final Parameter parameter, final Request request) {
        if (null == request) {
            throw new NullPointerException();
        }
        this.parameter = parameter;
        this.request = request;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {
        if (FIELD_METHOD.equals(name)) {
            return Converter.wrap(parameter, request.getRequestType().toString().toLowerCase(), start, false);
        } else if (Request.FIELD_FIELDS.equals(name)) {
            return Converter.wrap(parameter, request.getFields(), start, false);
        } else if (Request.FIELD_RESOURCE_PATH.equals(name)) {
            return Converter.wrap(parameter, request.getResourcePath(), start, false);
        } else if (Request.FIELD_ADDITIONAL_PARAMETERS.equals(name)) {
            return Converter.wrap(parameter, request.getAdditionalParameters(), start, false);
        } else {
            return NOT_FOUND;
        }
    }

    @Override
    public Object get(int index, Scriptable start) {
        return NOT_FOUND;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return FIELD_METHOD.equals(name)
                || Request.FIELD_FIELDS.equals(name)
                || Request.FIELD_RESOURCE_PATH.equals(name)
                || Request.FIELD_ADDITIONAL_PARAMETERS.equals(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // don't allow setting fields or resource name
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        // setting by index not supported
    }

    @Override
    public void delete(String name) {
    }

    @Override
    public void delete(int index) {
    }

    /** the generic request properties common to all request types */
    private static final Object[] PROPERTIES = new Object[] {
        FIELD_METHOD,
        Request.FIELD_FIELDS,
        Request.FIELD_RESOURCE_PATH,
        Request.FIELD_ADDITIONAL_PARAMETERS
    };

    @Override
    public Object[] getIds() {
        return PROPERTIES;
    }

    protected static Object[] concatIds(Object... properties) {
        Object[] result = Arrays.copyOf(PROPERTIES, PROPERTIES.length + properties.length);
        System.arraycopy(properties, 0, result, PROPERTIES.length, properties.length);
        return result;
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false; // no support for javascript instanceof
    }

    @Override
    public Object unwrap() {
        return request;
    }

    public String toString() {
        if (request == null) {
            return "null";
        }

        JsonValue value = new JsonValue(new HashMap<String, Object>());
        for (Object id : getIds()) {
            value.put((String) id, get((String) id, this));
        }

        return value.toString();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == String.class) {
            return toString();
        } else {
            return super.getDefaultValue(hint);
        }
    }
}
