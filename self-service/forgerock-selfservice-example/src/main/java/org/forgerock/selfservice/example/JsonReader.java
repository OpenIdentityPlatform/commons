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

package org.forgerock.selfservice.example;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Simple utility class to parse json string into a json value.
 *
 * @since 0.1.0
 */
public final class JsonReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonReader() {
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
     * Given a path to a json file returns a corresponding json value.
     *
     * @param filename
     *         file path to json file
     *
     * @return json value
     */
    public static JsonValue jsonFileToJsonValue(String filename) {
        InputStream stream = JsonReader.class.getResourceAsStream(filename);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {

            StringBuilder contents = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                contents.append(line).append('\n');
            }

            return toJsonValue(contents.toString());
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

}
