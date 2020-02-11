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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.selfservice.stages.utils;

import static org.forgerock.json.JsonValue.json;

import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;

import java.io.IOException;
import java.util.Map;

/**
 * Simple utility class to parse json string into a json value.
 *
 * @since 0.2.0
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Given a json string return a corresponding json value.
     *
     * @param json
     *         json string
     *
     * @return json value
     */
    public static JsonValue toJsonValue(String json) {
        try {
            return new JsonValue(MAPPER.readValue(json, Map.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

    /**
     * Merges the provided json value instances.
     * The right most json value will override properties that match the previous json values.
     * @param jsons
     *         json value instances to be merged
     *
     * @return json new json value; returns an empty json instance if inputs are null
     */
    public static JsonValue merge(JsonValue... jsons) {
        Map<String, Object> results = new HashMap<>();
        for (JsonValue json : jsons) {
            if (json != null) {
                results.putAll(json.asMap());
            }
        }
        return json(results);
    }

}
