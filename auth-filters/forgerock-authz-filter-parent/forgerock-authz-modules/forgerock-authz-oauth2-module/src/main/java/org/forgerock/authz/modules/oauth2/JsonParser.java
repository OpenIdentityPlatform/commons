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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;

import java.io.IOException;
import java.util.Map;

/**
 * Simple wrapper around the {@link ObjectMapper} class to facilitate testing.
 *
 * @since 1.4.0
 */
public class JsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parses the Json String into a {@link JsonValue}.
     *
     * @param s The Json String to parse.
     * @return A JsonValue
     * @throws IOException If the String could not be parsed.
     */
    public JsonValue parse(final String s) throws IOException {
        return new JsonValue(MAPPER.readValue(s, Map.class));
    }
}
