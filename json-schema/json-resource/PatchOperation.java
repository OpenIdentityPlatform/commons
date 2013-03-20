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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * The interface that provides the ability to apply partial changes to
 * resources. Implementations interpret a specific patch document
 * representation.
 */
public final class PatchOperation {

    public static final String ADD_TYPE = "add";
    public static final String REMOVE_TYPE = "remove";
    public static final String REPLACE_TYPE = "replace";

    private final String type;
    private final JsonPointer field;
    private final JsonValue value;

    public PatchOperation(String type, JsonPointer field, JsonValue value) {
        this.type = notNull(type);
        this.field = notNull(field);
        this.value = notNull(value);
    }

    public static PatchOperation add(JsonPointer field, JsonValue value) {
        return new PatchOperation(ADD_TYPE, field, value);
    }

    public static PatchOperation remove(JsonPointer field) {
        return new PatchOperation(REMOVE_TYPE, field, new JsonValue(null));
    }

    public static PatchOperation remove(JsonPointer field, JsonValue value) {
        // TODO: need to check if this is possible.
        return new PatchOperation(REMOVE_TYPE, field, value);
    }

    public static PatchOperation replace(JsonPointer field, JsonValue value) {
        return new PatchOperation(REPLACE_TYPE, field, value);
    }

    public static final PatchOperation valueOf(JsonValue json) {
        // TODO
        return null;
    }

    public JsonValue toJsonValue() {
        // TODO
        return null;
    }

    public String toString() {
        // TODO
        return null;
    }

    public String getType() {
        return type;
    }

    public JsonPointer getField() {
        return field;
    }

    public JsonValue getValue() {
        return value;
    }

    /**
     * Applies a {@code PatchOperationVisitor} to this {@code PatchOperation}.
     *
     * @param <R>
     *            The return type of the visitor's methods.
     * @param <P>
     *            The type of the additional parameters to the visitor's
     *            methods.
     * @param v
     *            The patch operation visitor.
     * @param p
     *            Optional additional visitor parameter.
     * @return A result as specified by the visitor.
     */
    public <R, P> R accept(final PatchOperationVisitor<R, P> v, final P p) {
        if (type.equals(ADD_TYPE)) {
            return v.visitAddOperation(p, field, value);
        } else if (type.equals(REMOVE_TYPE)) {
            return v.visitRemoveOperation(p, field);
        } else if (type.equals(REPLACE_TYPE)) {
            return v.visitReplaceOperation(p, field, value);
        } else {
            return v.visitUnrecognizedOperation(p, type, field, value);
        }
    }

    private static <T> T notNull(final T object) {
        if (object != null) {
            return object;
        } else {
            throw new NullPointerException();
        }
    }
}
