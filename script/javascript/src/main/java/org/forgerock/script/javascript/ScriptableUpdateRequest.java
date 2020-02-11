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

import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

/**
 * Provides a {@code Scriptable} wrapper for a {@code UpdateRequest} object.
 */
class ScriptableUpdateRequest extends AbstractScriptableRequest implements Wrapper {

    private static final long serialVersionUID = 1L;


    /** The map being wrapped. */
    private final UpdateRequest request;

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptableUpdateRequest(final Parameter parameter, final UpdateRequest request) {
        super(parameter, request);
        this.request = request;
    }

    @Override
    public String getClassName() {
        return "ScriptableUpdateRequest";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, Scriptable start) {
        if (UpdateRequest.FIELD_CONTENT.equals(name)) {
            return Converter.wrap(parameter, request.getContent(), start, false);
        } else if (UpdateRequest.FIELD_REVISION.equals(name)) {
            return Converter.wrap(parameter, request.getRevision(), start, false);
        } else {
            return super.get(name, start);
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return UpdateRequest.FIELD_CONTENT.equals(name)
                || UpdateRequest.FIELD_REVISION.equals(name)
                || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO allow updating newContent and revision
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
    }

    /** concatenate the UpdateRequest properties with the generic request properties */
    private static Object[] PROPERTIES = concatIds(
            UpdateRequest.FIELD_CONTENT,
            UpdateRequest.FIELD_REVISION);

    @Override
    public Object[] getIds() {
        return PROPERTIES;
    }

}
