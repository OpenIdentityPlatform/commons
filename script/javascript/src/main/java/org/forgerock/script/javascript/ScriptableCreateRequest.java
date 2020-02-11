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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Provides a {@code Scriptable} wrapper for a {@code CreateRequest} object.
 */
class ScriptableCreateRequest extends AbstractScriptableRequest implements Wrapper {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** The map being wrapped. */
    private final CreateRequest request;

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptableCreateRequest(final Parameter parameter, final CreateRequest request) {
        super(parameter, request);
        this.request = request;
    }

    @Override
    public String getClassName() {
        return "ScriptableCreateRequest";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {
        if (CreateRequest.FIELD_CONTENT.equals(name)) {
            return Converter.wrap(parameter, request.getContent(), start, false);
        } else if (CreateRequest.FIELD_NEW_RESOURCE_ID.equals(name)) {
            return Converter.wrap(parameter, request.getNewResourceId(), start, false);
        } else {
            return super.get(name, start);
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return CreateRequest.FIELD_CONTENT.equals(name)
                || CreateRequest.FIELD_NEW_RESOURCE_ID.equals(name)
                || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO allow updating content and newResourceId
        if (CreateRequest.FIELD_CONTENT.equals(name)) {
            try {
                Object json = JSON_MAPPER.readValue((String) value, Object.class);
                if (json == null) {
                    request.setContent(new JsonValue(new LinkedHashMap<String, Object>(0)));
                } else if (json instanceof Map) {
                    request.setContent(new JsonValue(json));
                }
            } catch (IOException e) {
                // TODO log the failure
            }
        } else if (CreateRequest.FIELD_NEW_RESOURCE_ID.equals(name)) {
            request.setNewResourceId((String) value);
        } else {
            super.put(name, start);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
    }

    /** concatenate the CreateRequest properties with the generic request properties */
    private static Object[] PROPERTIES = concatIds(
            CreateRequest.FIELD_CONTENT,
            CreateRequest.FIELD_NEW_RESOURCE_ID);

    @Override
    public Object[] getIds() {
        return PROPERTIES;
    }

}
