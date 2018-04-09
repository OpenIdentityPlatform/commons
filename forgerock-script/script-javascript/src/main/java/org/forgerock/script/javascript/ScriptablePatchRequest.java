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
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.util.ArrayList;

/**
 * Provides a {@code Scriptable} wrapper for a {@code PatchRequest} object.
 */
class ScriptablePatchRequest extends AbstractScriptableRequest implements Wrapper {

    private static final long serialVersionUID = 1L;


    /** The map being wrapped. */
    private final PatchRequest request;

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptablePatchRequest(final Parameter parameter, final PatchRequest request) {
        super(parameter, request);
        this.request = request;
    }

    @Override
    public String getClassName() {
        return "ScriptablePatchRequest";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {
        if (PatchRequest.FIELD_PATCH_OPERATIONS.equals(name)) {
            final JsonValue value = new JsonValue(new ArrayList<Object>());
            for (final PatchOperation operation : request.getPatchOperations()) {
                value.add(operation.toJsonValue().getObject());
            }
            return Converter.wrap(parameter, value, start, false);
        } else if (PatchRequest.FIELD_REVISION.equals(name)) {
            return Converter.wrap(parameter, request.getRevision(), start, false);
        } else {
            return super.get(name, start);
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return PatchRequest.FIELD_PATCH_OPERATIONS.equals(name)
                || PatchRequest.FIELD_REVISION.equals(name)
                || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO allow updating patch operations and revision(?)
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
    }

    /** concatenate the PatchRequest properties with the generic request properties */
    private static Object[] PROPERTIES = concatIds(
            PatchRequest.FIELD_PATCH_OPERATIONS, 
            PatchRequest.FIELD_REVISION);

    @Override
    public Object[] getIds() {
        return PROPERTIES;
    }

}
