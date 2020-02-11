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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.json;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.util.Function;

/**
 * An implementation of {@link Function} that recursively traverses the {@link JsonValue} and applies some
 * transformation if needed. This class may be subclassed to override needed methods to perform the
 * expected transformation(s).
 */
public class JsonValueTraverseFunction implements Function<JsonValue, JsonValue, JsonValueException> {

    /** the transformation function to be applied to each value. */
    private Function<JsonValue, ?, JsonValueException> transform;

    /**
     * Construct the traversal function with a transformation function to apply to each array element
     * nested object attribute value element, or primitive element.
     *
     * @param transform a transformation function
     */
    public JsonValueTraverseFunction(Function<JsonValue, ?, JsonValueException> transform) {
        this.transform = transform;
    }

    @Override
    public final JsonValue apply(JsonValue value) {
        return new JsonValue(traverse(value), value.getPointer());
    }

    private Object traverse(JsonValue value) {
        if (value.isList()) {
            return traverseList(value);
        }
        if (value.isMap()) {
            return traverseMap(value);
        }
        return value.as(transform);
    }

    /**
     * Transform a JsonValue List into another object. Default implementation is to return a new
     * {@link ArrayList} filled with the elements on which we applied the transformations.
     *
     * @param value the value to transform
     * @return the transformed value
     */
    protected Object traverseList(JsonValue value) {
        List<Object> result = array();
        for (JsonValue elem : value) {
            result.add(apply(elem).getObject());
        }
        return result;
    }

    /**
     * Transform a JsonValue Map into another object. Default implementation is to return a new
     * {@link LinkedHashMap} filled with the elements on which we applied the transformations.
     *
     * @param value the value to transform
     * @return the transformed value
     */
    protected Object traverseMap(JsonValue value) {
        Map<String, Object> result = object(value.size());
        for (String key : value.keys()) {
            result.put(key, apply(value.get(key)).getObject());
        }
        return result;
    }

}
