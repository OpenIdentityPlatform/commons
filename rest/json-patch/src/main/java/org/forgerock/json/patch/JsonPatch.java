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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.patch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * Processes partial modifications to JSON values. Implements
 * <a href="http://tools.ietf.org/html/draft-pbryan-json-patch-02">draft-pbryan-json-patch-02</a>.
 *
 * @author Paul C. Bryan
 */
public class JsonPatch {

    /** Internet media type for the JSON Patch format. */
    public static final String MEDIA_TYPE = "application/json-patch";

    /**
     * Returns {@code true} if the type contained by {@code v1} is different than the type
     * contained by {@code v2}.
     * <p>
     * Note: If an unexpected (non-JSON) type is encountered, this method returns
     * {@code true}, triggering a change in the resulting patch. 
     */
    private static boolean differentTypes(JsonValue v1, JsonValue v2) {
        if (v1.isNull() && v2.isNull()) { // both values are null
            return false;
        } else if (v1.isMap() && v2.isMap()) {
            return false;
        } else if (v1.isList() && v2.isList()) {
            return false;
        } else if (v1.isString() && v2.isString()) {
            return false;
        } else if (v1.isNumber() && v2.isNumber()) {
            return false;
        } else if (v1.isBoolean() && v2.isBoolean()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Produces a JSON Patch operation object.
     *
     * @param op the operation to perform.
     * @param pointer the JSON value to modify.
     * @param value the JSON value to apply, or {@code null} if not applicable.
     * @return the resulting JSON Patch operation.
     */
    private static HashMap<String, Object> op(String op, JsonPointer pointer, JsonValue value) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(op, pointer.toString());
        if (value != null) {
            result.put("value", value.copy().getObject());
        }
        return result;
    }

    /**
     * Returns value of an operation.
     *
     * @param op the patch operation containing the value to be returned.
     * @return the value specified in the operation.
     * @throws JsonValueException if a value is not provided.
     */
    private static Object opValue(JsonValue op) throws JsonValueException {
        Object value = op.get("value").getObject();
        if (value == null && !op.isDefined("value")) { // allow explicit null value
            throw new JsonValueException(op, "expecting a value member");
        }
        return value;
    }

    /**
     * Returns the parent value of the value identified by the JSON pointer.
     *
     * @param pointer the pointer to the value whose parent value is to be returned.
     * @param target the JSON value against which to resolve the JSON pointer.
     * @return the parent value of the value identified by the JSON pointer.
     * @throws JsonException if the parent value could not be found.
     */
    private static JsonValue parentValue(JsonPointer pointer, JsonValue target) throws JsonException {
        JsonValue result = null;
        JsonPointer parent = pointer.parent();
        if (parent != null) {
            result = target.get(parent);
            if (result == null) {
                throw new JsonException("parent value not found");
            }
        }
        return result;
    }

    /**
     * Compares two JSON values, and produces a JSON Patch value, which contains the
     * operations necessary to modify the {@code original} value to arrive at the
     * {@code target} value.
     *
     * @param original the original value.
     * @param target the intended target value.
     * @return the resulting JSON Patch value.
     * @throws NullPointerException if either of {@code original} or {@code target} are {@code null}.
     */
    public static JsonValue diff(JsonValue original, JsonValue target) {
        ArrayList<Object> result = new ArrayList<>();
        if (differentTypes(original, target)) { // different types cause a replace
            result.add(op("replace", original.getPointer(), target));
        } else if (original.isMap()) {
            for (String key : original.keys()) {
                if (target.isDefined(key)) { // target also has the property
                    JsonValue diff = diff(original.get(key), target.get(key)); // recursively compare properties
                    if (diff.size() > 0) {
                        result.addAll(diff.asList()); // add diff results
                    }
                } else { // property is missing in target
                    result.add(op("remove", original.getPointer().child(key), null));
                }
            }
            for (String key : target.keys()) {
                if (!original.isDefined(key)) { // property is in target, not in original
                    result.add(op("add", original.getPointer().child(key), target.get(key)));
                }
            }
        } else if (original.isList()) {
            boolean replace = false;
            if (original.size() != target.size()) {
                replace = true;
            } else {
                Iterator<JsonValue> i1 = original.iterator();
                Iterator<JsonValue> i2 = target.iterator();
                while (i1.hasNext() && i2.hasNext()) {
                    if (diff(i1.next(), i2.next()).size() > 0) { // recursively compare elements
                        replace = true;
                        break;
                    }
                }
            }
            if (replace) { // replace list entirely
                result.add(op("replace", original.getPointer(), target));
            }
        } else if (!original.isNull() && !original.getObject().equals(target.getObject())) { // simple value comparison
            result.add(op("replace", original.getPointer(), target));
        }
        return new JsonValue(result);
    }

    /**
     * Applies a set of modifications in a JSON patch value to an original value, resulting
     * in the intended target value. In the event of a failure, this method does not revert
     * any modifications applied up to the point of failure.
     *
     * @param original the original value on which to apply the modifications.
     * @param patch the JSON Patch value, specifying the modifications to apply to the original value.
     * @throws JsonValueException if application of the patch failed.
     */
    public static void patch(JsonValue original, JsonValue patch) throws JsonValueException {
        for (JsonValue op : patch.required().expect(List.class)) {
            JsonPointer pointer;
            if ((pointer = op.get("replace").asPointer()) != null) {
                JsonValue parent = parentValue(pointer, original);
                if (parent != null) { // replacing a child
                    String leaf = pointer.leaf();
                    if (!parent.isDefined(leaf)) {
                        throw new JsonValueException(op, "value to replace not found");
                    }
                    parent.put(leaf, opValue(op));
                } else { // replacing the root value itself
                    original.setObject(opValue(op));
                }
            } else if ((pointer = op.get("add").asPointer()) != null) {
                JsonValue parent = parentValue(pointer, original);
                if (parent == null) {
                    if (original.getObject() != null) {
                        throw new JsonValueException(op, "root value already exists");
                    }
                    original.setObject(opValue(op));
                } else {
                    try {
                        parent.add(pointer.leaf(), opValue(op));
                    } catch (JsonException je) {
                        throw new JsonValueException(op, je);
                    }
                }
            } else if ((pointer = op.get("remove").asPointer()) != null) {
                JsonValue parent = parentValue(pointer, original);
                String leaf = pointer.leaf();
                if (parent == null) {
                    original.setObject(null);
                } else {
                    if (!parent.isDefined(leaf)) {
                        throw new JsonValueException(op, "value to remove not found");
                    }
                    try {
                        parent.remove(leaf);
                    } catch (JsonException je) {
                        throw new JsonValueException(op, je);
                    }
                }
            } else {
                throw new JsonValueException(op, "expecting add, remove or replace member");
            }
        }
    }
}
